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

    private fun dropped(result: TransformResult): TransformResult.Dropped {
        assertTrue("expected Dropped but was $result", result is TransformResult.Dropped)
        return result as TransformResult.Dropped
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

    // ── Multi-bank rollout, batch 1 (#66): package → account mapping ──────────
    // Starting-point cases; refine the wording against real captures on-device.

    @Test
    fun mercado_pago_purchase_maps_to_bank_account() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.mercadopago.wallet", text = "Pagaste \$320.00 en RAPPI con tu dinero disponible"),
                zone = mexicoCity,
            )
        )
        assertEquals("Mercado Pago", p.account)
        assertEquals(320.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun santander_credit_charge_maps_to_credit_account() {
        val p = ingestable(
            IngestTransform.transform(
                notif("mx.bancosantander.supermovil", text = "Cargo por \$1,500.00 en LIVERPOOL con tu tarjeta"),
                zone = mexicoCity,
            )
        )
        assertEquals("Santander Credit", p.account)
        assertEquals(1500.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun openbank_credit_charge_maps_to_credit_account() {
        val p = ingestable(
            IngestTransform.transform(
                notif("mx.openbank.modelbank", text = "Compra por \$89.50 en SPOTIFY"),
                zone = mexicoCity,
            )
        )
        assertEquals("Openbank Credit", p.account)
        assertEquals(89.50, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun didi_credit_charge_maps_to_credit_account() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.didiglobal.cashloan", text = "Cargo por \$250.00 a tu línea DiDi"),
                zone = mexicoCity,
            )
        )
        assertEquals("Didi Credit", p.account)
        assertEquals(250.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun google_wallet_taps_are_not_captured() {
        // financial-command-center#65: Wallet capture was removed. It posted the
        // terminal's currency behind a bare "$" (USD abroad, stored as MXN — a
        // $31.64 tap became 31.64 MXN), while the card's own bank app already
        // captured the same purchase correctly. Untracked now, so it is not even
        // recorded as a tracked-source drop.
        val d = dropped(
            IngestTransform.transform(
                notif(
                    "com.google.android.apps.walletnfcrel",
                    text = "CULVERS CHICAGO WRIGLEYV \$31.64 with Like U ••0780",
                ),
                zone = mexicoCity,
            )
        )
        assertTrue("Wallet is untracked and must not flood undecided", !d.fromTrackedSource)
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

    // ── Promotional noise that carries an amount (financial-command-center#66) ─
    // These reached the Inbox as ledger rows because they quote a figure. They are
    // offers and reminders, not money moving. The rules key on the *promotional
    // framing*, never on a product name: "Efectivo Inmediato" is also a real BBVA
    // cash advance, and dropping that would hide a genuine debt.

    @Test
    fun bbva_credit_line_offer_is_dropped() {
        val d = dropped(
            IngestTransform.transform(
                notif(
                    "com.bancomer.mbanking",
                    title = "Usa \$16,500 de tu TDC ¡sin comisión!🤑",
                    text = "Luis Angel, con Efectivo Inmediato retira el dinero de tu TDC *5364 a tu cuenta.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("noise filter matched", d.reason)
    }

    @Test
    fun bbva_credit_limit_increase_offer_is_dropped() {
        val d = dropped(
            IngestTransform.transform(
                notif(
                    "com.bancomer.mbanking",
                    title = "🚀 Tu incremento ya te espera",
                    text = "Aumenta hasta \$17,200 más a tu TDC *5364. Hazlo desde tu app BBVA.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("noise filter matched", d.reason)
    }

    @Test
    fun didi_deposit_promo_is_dropped() {
        val d = dropped(
            IngestTransform.transform(
                notif(
                    "com.didiglobal.cashloan",
                    title = "DiDi Cuenta：[NO LEÍDO] Haz tu primer depósito",
                    text = "Hola Luis. Habilitamos en tu cuenta una tasa de hasta el 15% anual. Deposita \$200 m.n. ahora.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("noise filter matched", d.reason)
    }

    @Test
    fun mercado_pago_bill_reminder_is_dropped() {
        val d = dropped(
            IngestTransform.transform(
                notif(
                    "com.mercadopago.wallet",
                    title = "Tienes un recibo que vence hoy",
                    text = "Paga ahora \$ 428.88 a Telcel - Pospago.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("noise filter matched", d.reason)
    }

    // Look-alikes that MUST survive the new rules: real money, real wording.

    @Test
    fun real_spends_and_transfers_survive_the_noise_rules() {
        val survivors = listOf(
            "mx.openbank.modelbank" to ("Compra exitosa ✅" to "Hiciste una compra en AMAZON MKTPLACE PMTS con tu tarjeta terminación ****4044 por \$220.00."),
            "com.bancomer.mbanking" to ("Aviso de cargo en tu cuenta" to "Tienes un cargo a tu cuenta *3960 de \$4,800.00. Esta información te ayuda a tener control."),
            "com.bancomer.mbanking" to ("Cargo a tu cuenta" to "Cargo a tu cuenta *3960 de \$200.00"),
            "com.bancomer.mbanking" to ("BBVA" to "Abono a tu cuenta *33960 de \$100.00"),
            // Own-account movements: not income, but they must still reach the Inbox.
            "com.mercadopago.wallet" to ("Tu dinero ya está disponible" to "Ingresaste \$ 200.00 desde tu cuenta de BBVA MEXICO."),
            "mx.openbank.modelbank" to ("Recibiste una transferencia ✅" to "Recibiste \$ 100.00 en tu cuenta ****5543."),
            // Cashback stays capturable: the owner resolves it in the Inbox (#66).
            "com.didiglobal.cashloan" to ("Cashback de DiDi Card recibido: MXN\$14.68" to "Puedes agregarlo a tu DiDi Monedero."),
        )
        for ((pkg, texts) in survivors) {
            val (title, body) = texts
            val result = IngestTransform.transform(notif(pkg, title = title, text = body), zone = mexicoCity)
            assertTrue("must stay capturable: $title", result is TransformResult.Ingestable)
        }
    }

    // ── Amount at the end of a sentence (real wording; 18 captures were lost) ──
    // The capture app's own skip log showed these dropped as "no parseable
    // amount" even though the amount is right there: the magnitude group also
    // swallowed the full stop, and "132.57." is not a number.

    @Test
    fun openbank_purchase_with_trailing_period_parses() {
        val p = ingestable(
            IngestTransform.transform(
                notif(
                    "mx.openbank.modelbank",
                    title = "Compra exitosa ✅",
                    text = "Hiciste una compra en AMAZON MKTPLACE PMTS con tu tarjeta terminación ****4044 por \$132.57. Si no la reconoces, llámanos a la Línea Open.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("Openbank Credit", p.account)
        assertEquals(132.57, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
    }

    @Test
    fun bbva_charge_notice_with_trailing_period_parses() {
        val p = ingestable(
            IngestTransform.transform(
                notif(
                    "com.bancomer.mbanking",
                    title = "Aviso de cargo en tu cuenta",
                    text = "Tienes un cargo a tu cuenta *3960 de \$14,694.75. Esta información te ayuda a tener control en tus finanzas.",
                ),
                zone = mexicoCity,
            )
        )
        assertEquals("BBVA Debit", p.account)
        assertEquals(14694.75, p.amount, 0.0001)
    }

    @Test
    fun amount_followed_by_a_comma_parses() {
        val p = ingestable(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", text = "Cargo por \$1,234.56, consulta tu app"),
                zone = mexicoCity,
            )
        )
        assertEquals(1234.56, p.amount, 0.0001)
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

    // ── ADR-0013: SMS source (DiDi balance / cashloan) ───────────────────────

    @Test
    fun didi_sms_charge_maps_to_account_amount_currency() {
        val result = IngestTransform.transformSms(
            sender = "DiDi",
            body = "DiDi: Cargo por \$250.00 de tu linea de credito",
            timestamp = fixedTs,
            zone = mexicoCity,
        )
        val p = ingestable(result)
        assertEquals("Didi Credit", p.account)
        assertEquals(250.0, p.amount, 0.0001)
        assertEquals("MXN", p.currency)
        assertEquals("sms:DiDi", p.source.app)
        assertTrue("sms reference is namespaced", p.source.reference.startsWith("sms-"))
        assertEquals("2026-08-14T20:15:00-06:00", p.occurred_at)
    }

    @Test
    fun sms_reference_is_stable_and_independent_of_timestamp() {
        // The live broadcast and the READ_SMS sweep can see different timestamps
        // for the same message; the reference must ignore the clock so the sweep
        // re-reading a broadcast-captured SMS is a duplicate no-op, not a 2nd row.
        val a = ingestable(
            IngestTransform.transformSms("DiDi", "Cargo por \$99.00 en UBER", fixedTs, mexicoCity)
        )
        val b = ingestable(
            IngestTransform.transformSms("DiDi", "Cargo por \$99.00 en UBER", fixedTs + 5_000L, mexicoCity)
        )
        assertEquals("sms reference must not depend on timestamp", a.source.reference, b.source.reference)
    }

    @Test
    fun untracked_sms_sender_is_dropped_and_not_from_tracked_source() {
        // Body has a parseable amount, but the sender isn't ours: dropped before
        // parsing, and NOT recorded to undecided (would flood on every OTP/promo).
        val d = dropped(IngestTransform.transformSms("VERIZON", "You used \$50 of data", fixedTs, mexicoCity))
        assertTrue("untracked sender must not be a tracked-source drop", !d.fromTrackedSource)
    }

    @Test
    fun didi_sms_without_amount_is_a_tracked_source_drop() {
        // A DiDi SMS we can't parse a spend from IS worth recording (miss-rate).
        val d = dropped(IngestTransform.transformSms("DiDi", "Bienvenido a DiDi, tu app de movilidad", fixedTs, mexicoCity))
        assertTrue("a tracked sender with no amount should be recorded", d.fromTrackedSource)
    }

    // ── ADR-0013 §4: the tracked-source flag on notification drops ────────────

    @Test
    fun tracked_app_noise_drop_is_from_tracked_source() {
        val d = dropped(
            IngestTransform.transform(
                notif("com.bancomer.mbanking", text = "Tu código de verificación es 123456"),
                zone = mexicoCity,
            )
        )
        assertTrue("a tracked app we couldn't parse should be recorded", d.fromTrackedSource)
    }

    @Test
    fun untracked_app_drop_is_not_from_tracked_source() {
        val d = dropped(IngestTransform.transform(notif("com.slack", text = "New message"), zone = mexicoCity))
        assertTrue("an unrelated app must not flood undecided", !d.fromTrackedSource)
    }
}
