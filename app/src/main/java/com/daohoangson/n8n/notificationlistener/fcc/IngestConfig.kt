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

object IngestConfig {
    // A charge line such as "$1,234.56", "MXN 250.00", "US$45.30". Group 1 is the
    // bare number (thousands separators allowed); IngestTransform normalizes it.
    private val MONEY = """(?:MXN|USD|US\$|\$)\s?([0-9][0-9.,]*)""".toRegex(RegexOption.IGNORE_CASE)

    // Text that signals "not a spend" even though it came from a bank app.
    private val NOISE =
        """(?i)\b(c[oó]digo|otp|verificaci[oó]n|saldo disponible|promoci[oó]n|beneficio|estado de cuenta)\b""".toRegex()

    // Which currency a line is denominated in, when it isn't the account default.
    private val CURRENCY = """(USD|US\$|MXN)""".toRegex(RegexOption.IGNORE_CASE)

    val rules: List<AccountRule> = listOf(
        AccountRule(
            packageName = "com.bbva.bbvamovil",
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
    )

    fun ruleFor(packageName: String): AccountRule? =
        rules.firstOrNull { it.packageName == packageName }
}
