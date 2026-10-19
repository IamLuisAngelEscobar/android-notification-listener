package com.daohoangson.n8n.notificationlistener.fcc

import com.daohoangson.n8n.notificationlistener.data.database.ListenedApp
import com.daohoangson.n8n.notificationlistener.utils.NotificationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class EffectiveRulesTest {

    private val bbva = "com.bancomer.mbanking"
    private val nubank = "com.nu.production"

    private fun notif(pkg: String, text: String) =
        NotificationData(packageName = pkg, title = null, text = text, timestamp = 1_786_760_100_000L, id = 1, tag = null)

    @Test
    fun no_choices_keeps_every_built_in() {
        assertEquals(IngestConfig.rules, IngestConfig.effectiveRules(emptyList()))
    }

    @Test
    fun disabling_a_built_in_stops_capturing_it() {
        val rules = IngestConfig.effectiveRules(
            listOf(ListenedApp(bbva, "BBVA Debit", "MXN", enabled = false))
        )
        assertNull(rules.firstOrNull { it.packageName == bbva })

        val result = IngestTransform.transform(notif(bbva, "Compra por \$250.00"), rules = rules)
        assertTrue(result is TransformResult.Dropped)
        assertFalse((result as TransformResult.Dropped).fromTrackedSource)
    }

    @Test
    fun a_built_in_choice_overrides_account_but_keeps_its_regexes() {
        val rules = IngestConfig.effectiveRules(
            listOf(ListenedApp(bbva, "BBVA Credit", "USD", enabled = true))
        )
        val rule = rules.single { it.packageName == bbva }
        val shipped = IngestConfig.ruleFor(bbva)!!
        assertEquals("BBVA Credit", rule.account)
        assertEquals("USD", rule.defaultCurrency)
        assertEquals(shipped.amountRegex, rule.amountRegex)
        assertEquals(shipped.dropRegex, rule.dropRegex)
    }

    @Test
    fun an_added_app_is_captured_with_the_generic_rule() {
        val rules = IngestConfig.effectiveRules(
            listOf(ListenedApp(nubank, "Nu Credit", "MXN", enabled = true))
        )
        val result = IngestTransform.transform(
            notif(nubank, "Compraste \$1,234.50 en OXXO."),
            zone = ZoneId.of("-06:00"),
            rules = rules,
        )
        assertTrue("expected Ingestable but was $result", result is TransformResult.Ingestable)
        val payload = (result as TransformResult.Ingestable).payload
        assertEquals("Nu Credit", payload.account)
        assertEquals(1234.50, payload.amount, 0.0)
        assertEquals("MXN", payload.currency)
    }

    @Test
    fun an_added_app_still_drops_noise() {
        val rules = IngestConfig.effectiveRules(
            listOf(ListenedApp(nubank, "Nu Credit", "MXN", enabled = true))
        )
        val result = IngestTransform.transform(notif(nubank, "Tu código de verificación es 123456"), rules = rules)
        assertTrue(result is TransformResult.Dropped)
        assertTrue((result as TransformResult.Dropped).fromTrackedSource)
    }

    @Test
    fun a_disabled_added_app_is_not_captured() {
        val rules = IngestConfig.effectiveRules(
            listOf(ListenedApp(nubank, "Nu Credit", "MXN", enabled = false))
        )
        assertNull(rules.firstOrNull { it.packageName == nubank })
    }
}
