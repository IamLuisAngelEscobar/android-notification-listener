package com.daohoangson.n8n.notificationlistener.fcc

/**
 * Endpoint + secret for the private API, injected from `local.properties` at
 * build time (see app/build.gradle.kts → BuildConfig). Kept out of version
 * control so the shared secret never lands in the fork repo.
 */
data class IngestEndpoint(
    val url: String,     // e.g. http://100.72.37.0:8000/ingest  (Tailscale IP)
    val secret: String,  // the same FCC_INGEST_SECRET the backend runs with
)

/**
 * How one bank/card app's push notification becomes an `/ingest` row.
 *
 * `account` must match a **seeded account name** exactly (`BBVA Debit`,
 * `Amex Credit`, …) or the server answers 422. `amountRegex` must capture the
 * numeric magnitude in group 1; `currencyRegex` (optional) overrides
 * `defaultCurrency` when the text names a currency. `dropRegex` is the on-device
 * noise filter (Branch 5): a notification from this app whose text matches it —
 * an OTP, a balance/marketing ping — is dropped and never sent.
 *
 * The defaults below are a **starting point** the operator tunes against real
 * captured notification text (unparseable captures surface in the app's list so
 * the regex can be refined). See ADR-0012 and infrastructure-setup.md.
 */
data class AccountRule(
    val packageName: String,
    val account: String,
    val defaultCurrency: String,
    val amountRegex: Regex,
    val currencyRegex: Regex? = null,
    val dropRegex: Regex? = null,
)

/**
 * How one SMS sender's message becomes an `/ingest` row (ADR-0013). Mirrors
 * [AccountRule] but is keyed by the **sender**, because an SMS has no app
 * package: DiDi balance / cashloan spends arrive only by SMS, so the
 * notification listener never sees them.
 *
 * `senderRegex` matches the SMS originating address — a shortcode or an
 * alphanumeric sender id like `DiDi`. `account` must match a **seeded account
 * name** exactly or the server answers 422. `amountRegex` / `currencyRegex` /
 * `dropRegex` behave exactly as on [AccountRule].
 */
data class SmsRule(
    val senderRegex: Regex,
    val account: String,
    val defaultCurrency: String,
    val amountRegex: Regex,
    val currencyRegex: Regex? = null,
    val dropRegex: Regex? = null,
)

object IngestConfig {
    // A charge line such as "$1,234.56", "MXN 250.00", "US$45.30". Group 1 is the
    // bare number (thousands separators allowed); IngestTransform normalizes it.
    // The group must END on a digit: banks routinely close the sentence right
    // after the amount ("… por $132.57."), and a swallowed full stop made the
    // magnitude unparseable, silently dropping real captures.
    private val MONEY = """(?:MXN|USD|US\$|\$)\s?([0-9](?:[0-9.,]*[0-9])?)""".toRegex(RegexOption.IGNORE_CASE)

    // Text that signals "not a spend" even though it came from a bank app.
    //
    // Two groups (financial-command-center#66):
    //  1. the original OTP / statement / balance pings;
    //  2. promotional offers and bill reminders that QUOTE an amount — those
    //     parsed fine and became Inbox rows ("Usa $16,500 de tu TDC",
    //     "Aumenta hasta $17,200 más a tu TDC", "Haz tu primer depósito …
    //     Deposita $200", "Tienes un recibo que vence … Paga ahora $428.88").
    //
    // Keyed on the promotional FRAMING, never on a product name: "Efectivo
    // Inmediato" is also a real BBVA cash advance, and a rule on that name would
    // hide a genuine debt. Every alternative below is phrasing an offer uses and a
    // real charge/deposit notification does not; the survivors are pinned by
    // `real_spends_and_transfers_survive_the_noise_rules`.
    private val NOISE = (
        """(?i)(\b(c[oó]digo|otp|verificaci[oó]n|saldo disponible|promoci[oó]n|beneficio|estado de cuenta)\b""" +
            // Credit-line and cash-advance offers.
            """|\busa\s+\$?[\d,.]+\s+de\s+tu\b|\bretira\s+(?:hasta\s+)?\$?[\d,.]*\s*de\s+tu\b""" +
            """|\baumenta\s+hasta\b|\btu\s+incremento\b|\bsin\s+comisi[oó]n\b""" +
            // Savings / deposit promos.
            """|\bhaz\s+tu\s+primer\s+dep[oó]sito\b|\btasa\s+de\s+hasta\b""" +
            // Bill reminders: a due notice, not a payment that already happened.
            """|\brecibo\s+que\s+vence\b|\bpaga\s+ahora\b|\bprogramar\s+o\s+hacer\s+tu\s+pago\b)"""
        ).toRegex()

    // Which currency a line is denominated in, when it isn't the account default.
    private val CURRENCY = """(USD|US\$|MXN)""".toRegex(RegexOption.IGNORE_CASE)

    val rules: List<AccountRule> = listOf(
        AccountRule(
            // BBVA México (formerly Bancomer). Confirmed on-device via the
            // notification's sbn.packageName. (BBVA Spain is com.bbva.bbvamovil.)
            packageName = "com.bancomer.mbanking",
            account = "BBVA Debit",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        AccountRule(
            packageName = "com.americanexpress.android.acctsvcs.us",
            account = "Amex Credit",
            defaultCurrency = "USD",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        // ── Multi-bank rollout, batch 1 (#66) ──────────────────────────────────
        // Starting-point rules: package names confirmed installed on the S25
        // (2026-08-20); the amount/noise regexes are the generic MXN defaults,
        // tuned against real captured wording during on-device verify.
        AccountRule(
            // Mercado Pago wallet → seeded "Mercado Pago" (bank/debit).
            packageName = "com.mercadopago.wallet",
            account = "Mercado Pago",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        AccountRule(
            packageName = "mx.bancosantander.supermovil",
            account = "Santander Credit",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        AccountRule(
            packageName = "mx.openbank.modelbank",
            account = "Openbank Credit",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        AccountRule(
            // DiDi Finanzas / crédito. Note: the rideshare app is a different
            // package (com.didiglobal.passenger) — this is the cashloan one.
            packageName = "com.didiglobal.cashloan",
            account = "Didi Credit",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
        // ── Google Wallet tap-to-pay: REMOVED (financial-command-center#65) ────
        // ADR-0013 Track A captured Wallet alongside the underlying card and left
        // the duplicate to backend reconciliation (#69). Real captures showed that
        // costs more than it gives:
        //   * Wallet posts the amount in the TERMINAL's currency behind a bare
        //     "$" — abroad that is USD, but this rule defaulted to MXN, so a
        //     $31.64 tap was stored as 31.64 MXN, ~1/17th of the real spend;
        //   * the card's own bank app had already captured the same purchase
        //     correctly in MXN (6 of 9 Wallet rows matched a Santander row
        //     minute-for-minute at the USD→MXN rate).
        // The bank notification is the single source of truth, so the duplicate
        // never exists rather than being reconciled away. A tap on a card whose
        // bank app isn't tracked is now missed — a reason to add that bank's rule,
        // not to re-add Wallet.
    )

    fun ruleFor(packageName: String): AccountRule? =
        rules.firstOrNull { it.packageName == packageName }

    /**
     * SMS-sourced rules (ADR-0013, issue #68). **STARTING POINT** — the DiDi
     * sender id and the exact wording must be tuned on-device against real
     * messages (grant the app SMS access, watch what actually arrives), exactly
     * like the notification rules above.
     *
     * ⚠️ `account` must be a **seeded** account name, or `/ingest` 422s. "Didi
     * Credit" is reused from the notification rule as a placeholder; if DiDi
     * *balance* (the wallet, not the cashloan line) should map to a distinct
     * account, seed that account in the backend first and add a second rule.
     */
    val smsRules: List<SmsRule> = listOf(
        SmsRule(
            // DiDi delivers balance/cashloan alerts by SMS; the sender id contains
            // "DiDi". Refine to the real shortcode once observed on-device.
            senderRegex = """(?i)didi""".toRegex(),
            account = "Didi Credit",
            defaultCurrency = "MXN",
            amountRegex = MONEY,
            currencyRegex = CURRENCY,
            dropRegex = NOISE,
        ),
    )

    fun smsRuleFor(sender: String): SmsRule? =
        smsRules.firstOrNull { it.senderRegex.containsMatchIn(sender) }
}
