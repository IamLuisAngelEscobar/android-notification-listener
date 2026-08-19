# FCC fork — notes

This is a personal fork of
[`daohoangson/android-notification-listener`](https://github.com/daohoangson/android-notification-listener)
adapted to capture bank/credit-card push notifications and post them to the
Financial Command Center `/ingest` endpoint over Tailscale.

It implements the capture side of **Slice 4** (issue #53, PRD #49) per **ADR-0012**
in the main repo. No banking credentials, no Plaid — capture is from notifications
only.

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

## Tuning the parsers

`fcc/IngestConfig.kt` ships **starting-point** regexes for BBVA and Amex. Real
notification wording varies; refine `amountRegex` / `currencyRegex` / `dropRegex`
against captures you actually see, and re-run `IngestTransformTest` (add a case
per new wording). A notification with no parseable amount is dropped, not sent.
