package com.daohoangson.n8n.notificationlistener.ui.apps

import com.daohoangson.n8n.notificationlistener.data.database.ListenedApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppListingTest {

    private val installed = listOf(
        "com.bancomer.mbanking" to "BBVA México",
        "com.nu.production" to "Nu",
        "com.whatsapp" to "WhatsApp",
    )

    private fun listing(choices: List<ListenedApp>, pkg: String) =
        buildListings(installed, choices).single { it.packageName == pkg }

    @Test
    fun a_built_in_is_listened_by_default_with_its_shipped_account() {
        val bbva = listing(emptyList(), "com.bancomer.mbanking")
        assertTrue(bbva.listened)
        assertEquals("BBVA Debit", bbva.account)
        assertEquals("BBVA Debit", bbva.builtInAccount)
    }

    @Test
    fun a_disabled_built_in_keeps_its_account_so_it_can_be_switched_back_on() {
        val bbva = listing(listOf(ListenedApp("com.bancomer.mbanking", "BBVA Debit", "MXN", false)), "com.bancomer.mbanking")
        assertFalse(bbva.listened)
        assertEquals("BBVA Debit", bbva.account)
        assertEquals("MXN", bbva.defaultCurrency)
    }

    @Test
    fun an_untracked_app_needs_an_account_before_listening() {
        val whatsapp = listing(emptyList(), "com.whatsapp")
        assertFalse(whatsapp.listened)
        assertNull(whatsapp.account)
        assertNull(whatsapp.builtInAccount)
    }

    @Test
    fun an_added_app_shows_its_chosen_account() {
        val nu = listing(listOf(ListenedApp("com.nu.production", "Nu Credit", "MXN", true)), "com.nu.production")
        assertTrue(nu.listened)
        assertEquals("Nu Credit", nu.account)
    }
}
