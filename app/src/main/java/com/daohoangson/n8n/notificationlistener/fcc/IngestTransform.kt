package com.daohoangson.n8n.notificationlistener.fcc

import com.daohoangson.n8n.notificationlistener.utils.NotificationData
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Outcome of turning one notification into an `/ingest` row. */
sealed interface TransformResult {
    /** A financial spend that should be buffered and posted. */
    data class Ingestable(val payload: IngestPayload) : TransformResult

    /** Not a financial spend (wrong app, noise, or no parseable amount): drop it. */
    data class Dropped(val reason: String) : TransformResult
}

/**
 * Pure function: a captured [NotificationData] → an [IngestPayload] or a drop.
 *
 * "Pure" is the whole point of ADR-0012's switch away from the no-code Tasker
 * stack — this is a deterministic function over notification text with no
 * Android, network, or clock dependency (the zone is injected), so the parsing
 * and the account mapping are unit-tested off-device.
 *
 * The idempotency contract lives here: `source.reference` and `occurred_at` are
 * derived deterministically from the notification itself, so the same capture
 * always yields the same identity. The buffer then freezes the serialized body
 * and replays it verbatim, making a re-drain a `duplicate: true` no-op.
 */
object IngestTransform {

    private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun transform(
        data: NotificationData,
        zone: ZoneId = ZoneId.systemDefault(),
    ): TransformResult {
        val rule = IngestConfig.ruleFor(data.packageName)
            ?: return TransformResult.Dropped("not a tracked financial app: ${data.packageName}")

        val haystack = listOfNotNull(data.title, data.text).joinToString(" ").trim()
        if (haystack.isEmpty()) return TransformResult.Dropped("empty notification text")

        rule.dropRegex?.let { drop ->
            if (drop.containsMatchIn(haystack)) {
                return TransformResult.Dropped("noise filter matched")
            }
        }

        val amount = parseAmount(haystack, rule.amountRegex)
            ?: return TransformResult.Dropped("no parseable amount in: $haystack")
        if (amount.signum() == 0) return TransformResult.Dropped("amount is zero")

        val currency = parseCurrency(haystack, rule)

        val occurredAt = Instant.ofEpochMilli(data.timestamp)
            .atZone(zone)
            .toOffsetDateTime()
            .withNano(0)
            .format(ISO)

        val reference = deriveReference(data, amount, currency)

        val payload = IngestPayload(
            account = rule.account,
            amount = amount.toDouble(),
            currency = currency,
            description = haystack,
            occurred_at = occurredAt,
            source = IngestSource(
                reference = reference,
                app = data.packageName,
                raw_text = haystack,
            ),
        )
        return TransformResult.Ingestable(payload)
    }

    /** Extract the magnitude (group 1), stripping thousands separators. */
    internal fun parseAmount(text: String, regex: Regex): BigDecimal? {
        val raw = regex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: return null
        // Locale here is MXN/USD: `,` is a thousands separator, `.` the decimal.
        val normalized = raw.replace(",", "")
        return normalized.toBigDecimalOrNull()?.takeIf { it.signum() != 0 }
    }

    /** The account default unless the text explicitly names another ISO currency. */
    internal fun parseCurrency(text: String, rule: AccountRule): String {
        val named = rule.currencyRegex?.find(text)?.groupValues?.getOrNull(1)?.uppercase()
        val iso = when (named) {
            null -> null
            "US\$" -> "USD"
            else -> named
        }
        return (iso ?: rule.defaultCurrency).uppercase()
    }

    /**
     * A stable per-notification id: the same OS notification always hashes to the
     * same reference, so an OS re-post can't create a second ledger row, and a
     * retry replays this exact value. Built from the fields that identify the
     * event, not the wall clock.
     */
    internal fun deriveReference(
        data: NotificationData,
        amount: BigDecimal,
        currency: String,
    ): String {
        val seed = listOf(
            data.packageName,
            data.timestamp.toString(),
            data.id.toString(),
            data.tag ?: "",
            amount.toPlainString(),
            currency,
        ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray())
        return "notif-" + digest.joinToString("") { "%02x".format(it) }.take(32)
    }
}
