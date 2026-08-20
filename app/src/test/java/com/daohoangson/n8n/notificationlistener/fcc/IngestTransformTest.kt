package com.daohoangson.n8n.notificationlistener.fcc

import com.daohoangson.n8n.notificationlistener.utils.NotificationData
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class IngestTransformTest {

    // Fixed -06:00 offset (Mexico has no DST since 2022) so the formatted
    // occurred_at is deterministic regardless of the JVM's bundled tz database.
    private val mexicoCity = ZoneId.of("-06:00")

    // 2026-08-14T20:15:00-06:00.
    private val fixedTs = 1_786_760_100_000L

    private fun notif(
        pkg: String,
        title: String? = null,
        text: String? = null,
        id: Int = 42,
        tag: String? = null,
        ts: Long = fixedTs,
    ) = NotificationData(packageName = pkg, title = title, text = text, timestamp = ts, id = id, tag = tag)

    private fun ingestable(result: TransformResult): IngestPayload {
        assertTrue("expected Ingestable but was $result", result is TransformResult.Ingestable)
        return (result as TransformResult.Ingestable).payload
    }

    @Test
    fun bbva_debit_purchase_maps_account_amount_currency() {
        val result = IngestTransform.transform(
            notif("com.bancomer.mbanking", title = "BBVA", text = "Compra por \$250.00 en OXXO ROMA NORTE"),
            zone = mexicoCity,
        )
        val p = ingestable(result)
        assertEquals("BBVA Debit", p.account)
        assertEquals(250.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
        assertTrue(p.description.contains("OXXO ROMA NORTE"))
        assertEquals("2026-08-14T20:15:00-06:00", p.occurred_at)
    }

    @Test
    fun bbva_mx_abono_deposit_parses() {
        // Real BBVA México deposit wording observed on-device (2026-08-20).
        val p = ingestable(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", title = "BBVA", text = "Abono a tu cuenta de \$100"),
                zone = mexicoCity,
            )
        )
        assertEquals("BBVA Debit", p.account)
        assertEquals(100.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun amex_charge_defaults_to_usd() {
        val result = IngestTransform.transform(
            notif("com.americanexpress.android.acctsvcs.us", text = "You made a transaction of \$45.30 at STARBUCKS"),
            zone = mexicoCity,
        )
        val p = ingestable(result)
        assertEquals("Amex Credit", p.account)
        assertEquals(45.30, p.amount, 0.0001)
        assertEquals("USD", p.currency)
    }

    @Test
    fun thousands_separator_is_stripped() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", text = "Cargo por MXN 1,234.56 en AMAZON"),
                zone = mexicoCity,
            )
        )
        assertEquals(1234.56, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun explicit_us_dollar_token_overrides_default_currency() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", text = "Compra por US\$12.00 en APPLE"),
                zone = mexicoCity,
            )
        )
        assertEquals("USD", p.currency)
    }

    @Test
    fun amount_is_always_positive() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", text = "Compra por \$99.99 en SPOTIFY"),
                zone = mexicoCity,
            )
        )
        assertTrue("expected a positive magnitude", p.amount > 0)
    }

    @Test
    fun untracked_app_is_dropped() {
        val result = IngestTransform.transform(notif("com.slack", text = "You have a new message"), zone = mexicoCity)
        assertTrue(result is TransformResult.Dropped)
    }

    @Test
    fun otp_noise_from_bank_app_is_dropped() {
        val result = IngestTransform.transform(
            notif("com.bancomer.mbanking", text = "Tu código de verificación es 123456"),
            zone = mexicoCity,
        )
        assertTrue(result is TransformResult.Dropped)
    }

    @Test
    fun balance_ping_without_amount_is_dropped() {
        val result = IngestTransform.transform(
            notif("com.bancomer.mbanking", text = "Bienvenido de nuevo a BBVA Movil"),
            zone = mexicoCity,
        )
        assertTrue(result is TransformResult.Dropped)
    }

    @Test
    fun reference_and_occurred_at_are_stable_for_identical_input() {
        val a = ingestable(
            IngestTransform.transform(notif("com.bancomer.mbanking", text = "Compra por \$77.00 en UBER"), zone = mexicoCity)
        )
        val b = ingestable(
            IngestTransform.transform(notif("com.bancomer.mbanking", text = "Compra por \$77.00 en UBER"), zone = mexicoCity)
        )
        assertEquals("reference must be deterministic (idempotency)", a.source.reference, b.source.reference)
        assertEquals("occurred_at must be deterministic (idempotency)", a.occurred_at, b.occurred_at)
    }

    @Test
    fun serialized_body_carries_only_the_allowed_keys() {
        // The server model is extra="forbid"; a stray key would be a 422.
        val p = ingestable(
            IngestTransform.transform(notif("com.bancomer.mbanking", text = "Compra por \$10.00 en OXXO"), zone = mexicoCity)
        )
        val json = JsonParser.parseString(p.toJson()).asJsonObject
        assertEquals(
            setOf("account", "amount", "currency", "description", "occurred_at", "source"),
            json.keySet(),
        )
        val allowedSourceKeys = setOf("reference", "app", "raw_text")
        assertTrue(json.getAsJsonObject("source").keySet().all { it in allowedSourceKeys })
        assertTrue("reference is required", json.getAsJsonObject("source").has("reference"))
    }
}
