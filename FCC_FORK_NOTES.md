# FCC fork — notes

This is a personal fork of
[`daohoangson/android-notification-listener`](https://github.com/daohoangson/android-notification-listener)
adapted to capture bank/credit-card push notifications and post them to the
Financial Command Center `/ingest` endpoint over Tailscale.

It implements the capture side of **Slice 4** (issue #53, PRD #49) per **ADR-0012**
in the main repo, extended to **multi-source capture** per **ADR-0013**
(notifications **+ SMS**). No banking credentials, no Plaid — capture is from
on-device notifications and SMS only.

## The three scoped changes vs. upstream

1. **Payload transform + account mapping** — `fcc/IngestTransform.kt` (pure,
   unit-tested) turns a notification into the exact `/ingest` body. Account
   mapping and the on-device noise filter live in `fcc/IngestConfig.kt`; the wire
   model is `fcc/IngestPayload.kt` (only the server's allowed keys — the model is
   `extra="forbid"`).
2. **`X-Ingest-Secret` header** on every post — `network/IngestApi.kt`.
3. **WorkManager auto-drain** — every capture is written to the Room buffer
   (`data/database/PendingCapture*`) *before* any network call, then
   `work/IngestDrainWorker.kt` drains it (`NetworkType.CONNECTED` + exponential
   backoff), replacing upstream's manual, UI-driven retry.

The capture service (`NotificationListenerService.kt`) and the app
(`NotificationListenerApplication.kt`, `di/AppModule.kt`, `AndroidManifest.xml`)
were rewired to this buffer-first flow.

## Multi-source capture (ADR-0013)

Some spends never appear as an app notification. **DiDi balance / cashloan**
alerts arrive only by **SMS** (via Google Messages), with no bank push behind
them — so the notification listener alone can never see them. ADR-0013 adds SMS
as a **first-class, reliable source** and moves cross-source de-duplication to
the backend.

- **SMS source (issue #68).** `SmsReceiver.kt` captures the SMS *itself* (a
  `RECEIVE_SMS` `BroadcastReceiver`, not the Messages notification), and
  `work/SmsSweepWorker.kt` is a periodic `READ_SMS` inbox sweep that backstops a
  broadcast missed while the app was dead. Both feed the **same** buffer-first
  flow via `IngestTransform.transformSms(...)` (pure, unit-tested) and a
  sender-keyed `SmsRule` in `fcc/IngestConfig.kt`. SMS captures tag
  `source.app = "sms:<sender>"`. Reading the SMS removes the "did a notification
  pop?" failure entirely — the OS persists every SMS regardless.
- **Skip-logging / miss-rate (issue #70).** A drop from a *tracked* source
  (a known app/sender we still couldn't parse) is now recorded in
  `undecided_notifications` (bounded), so the capture miss-rate is measurable
  instead of guessed. Untracked apps/senders are still black-holed so the table
  doesn't flood. Driven by `TransformResult.Dropped.fromTrackedSource`.
- **Reconciliation (issue #69) is backend-only.** One real spend can arrive from
  several sources (bank push + Google Wallet + SMS); the fuzzy corroborate /
  insert / route-to-Inbox logic lives at `/ingest`, not here. This fork only
  emits the `source.app` provenance that logic relies on — no change needed for
  reconciliation itself.
- **Google Wallet capture was REMOVED** (financial-command-center#65). ADR-0013
  Track A captured Wallet *and* the underlying card, leaving the duplicate to
  backend reconciliation. Real captures showed two problems: Wallet posts the
  **terminal's** currency behind a bare `$` (USD abroad) while the rule defaulted
  to MXN — a `$31.64` tap was stored as `31.64 MXN` — and 6 of 9 Wallet rows
  already had a correct MXN row from the card's own bank app, minute-for-minute
  at the USD→MXN rate. The bank notification is now the single source of truth. A
  tap on a card whose bank app isn't tracked is missed; add that bank's rule
  rather than re-adding Wallet.

> ⚠️ **SMS rules are a starting point.** `IngestConfig.smsRules` matches the DiDi
> sender by `(?i)didi` and maps to the seeded **`Didi Credit`** account as a
> placeholder — tune the sender/wording against real messages on-device, and if
> DiDi *balance* needs a distinct account, seed it in the backend first (an
> unseeded account name is a 422).

## 9 PM daily-summary trigger (#80, ADR-0016 in the main repo)

The backend's `POST /whatsapp/send-summary?date=…` is passive: the phone owns the
clock. This app fires it at 9 PM phone-local, replacing the Tasker job the main
repo's ADR-0011 originally named.

- `fcc/SummarySchedule.kt` (pure, unit-tested):
  - computes the next 21:00 in the phone's zone, stable across DST;
  - derives the send-summary URL from `fcc.ingest.url` (`…/ingest` →
    `…/whatsapp/send-summary`), so there's **no new `local.properties` key**;
  - maps HTTP codes to send / retry / give up.
- `work/SummaryAlarm.kt` arms an exact alarm (`setExactAndAllowWhileIdle`). When it
  fires, `SummaryAlarmReceiver` queues a unique-per-date
  `work/SummaryTriggerWorker` (network-constrained, exponential backoff, gives up
  after 8 attempts ≈ 2h) and re-arms tomorrow's alarm.
- `SummaryRearmReceiver` re-arms after boot, a time-zone or clock change, and an
  app update. The app also re-arms on every start.
- Permissions: `USE_EXACT_ALARM` (granted at install for this sideloaded app) and
  `RECEIVE_BOOT_COMPLETED`.
- After installing, **open the app once** so the alarm is armed, and set its
  battery usage to **Unrestricted**.
- Logcat tags: `FccSummaryAlarm` (armed for …) and `FccSummaryTrigger` (summary
  sent / retry / giving up).
- For a missed night, message the bot `summary` or `summary YYYY-MM-DD`.

## Build configuration (required)

The Tailscale endpoint and shared secret are injected from `local.properties`
(gitignored — the secret never enters version control). Add these two lines to
`local.properties`:

```properties
fcc.ingest.url=http://100.72.37.0:8000/ingest
fcc.ingest.secret=REPLACE_WITH_FCC_INGEST_SECRET
```

- `fcc.ingest.url` — `http://<PI_TAILNET_IP>:8000/ingest` (plain http; the tailnet
  is the encryption boundary).
- `fcc.ingest.secret` — the same `FCC_INGEST_SECRET` the backend runs with (from
  `deploy/fcc.env`, kept in your password manager).

Then build/install:

```sh
./gradlew :app:testDebugUnitTest   # runs IngestTransformTest (the pure seam)
./gradlew :app:installDebug        # sideload to the connected S25
```

After install, grant the **SMS** runtime permissions so ADR-0013 capture works
(the notification-listener access grant is separate, as before):

```sh
adb shell pm grant com.daohoangson.n8n.notificationlistener android.permission.RECEIVE_SMS
adb shell pm grant com.daohoangson.n8n.notificationlistener android.permission.READ_SMS
```

## Tuning the parsers

`fcc/IngestConfig.kt` ships **starting-point** regexes for the notification
`rules` (BBVA, Amex, and the batch-1 banks) and for the SMS `smsRules` (DiDi).
Real notification/SMS wording varies; refine `amountRegex` / `currencyRegex` /
`dropRegex` (and, for SMS, `senderRegex`) against captures you actually see, and
re-run `IngestTransformTest` (add a case per new wording). Anything with no
parseable amount is dropped, not sent — and, from a tracked app/sender, recorded
in `undecided_notifications` so you can spot wording the parser is missing.
