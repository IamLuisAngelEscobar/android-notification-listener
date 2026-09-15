package com.daohoangson.n8n.notificationlistener.fcc

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SummaryScheduleTest {

    private val mexicoCity = ZoneId.of("America/Mexico_City")
    private val chicago = ZoneId.of("America/Chicago")

    private fun at(zone: ZoneId, y: Int, mo: Int, d: Int, h: Int, mi: Int = 0): ZonedDateTime =
        ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi), zone)

    // --- nextTrigger: the next 21:00 on the phone's local clock ---------------

    @Test
    fun before_nine_pm_triggers_today_at_nine_pm() {
        val next = SummarySchedule.nextTrigger(at(mexicoCity, 2026, 9, 15, 18, 30))
        assertEquals(at(mexicoCity, 2026, 9, 15, 21), next)
    }

    @Test
    fun exactly_nine_pm_triggers_tomorrow() {
        // The alarm that just fired re-arms from 21:00 itself; it must not re-fire today.
        val next = SummarySchedule.nextTrigger(at(mexicoCity, 2026, 9, 15, 21))
        assertEquals(at(mexicoCity, 2026, 9, 16, 21), next)
    }

    @Test
    fun after_nine_pm_triggers_tomorrow() {
        val next = SummarySchedule.nextTrigger(at(mexicoCity, 2026, 9, 15, 23, 59))
        assertEquals(at(mexicoCity, 2026, 9, 16, 21), next)
    }

    @Test
    fun rolls_over_the_end_of_the_year() {
        val next = SummarySchedule.nextTrigger(at(mexicoCity, 2026, 12, 31, 22))
        assertEquals(at(mexicoCity, 2027, 1, 1, 21), next)
    }

    @Test
    fun keeps_nine_pm_local_across_a_daylight_saving_change() {
        // US spring-forward is 2027-03-14: the day is 23 hours long, so "now + 24h"
        // would land at 22:00. The trigger must stay at 21:00 wall-clock time.
        val next = SummarySchedule.nextTrigger(at(chicago, 2027, 3, 13, 21, 30))
        assertEquals(at(chicago, 2027, 3, 14, 21), next)
    }

    @Test
    fun follows_the_zone_it_is_given() {
        // After a time-zone change the re-arm passes the new zone: 21:00 there.
        val madrid = ZoneId.of("Europe/Madrid")
        val next = SummarySchedule.nextTrigger(at(madrid, 2026, 9, 15, 10))
        assertEquals(at(madrid, 2026, 9, 15, 21), next)
    }

    // --- summaryUrl: derived from the configured ingest URL -------------------

    @Test
    fun summary_url_replaces_the_ingest_path_and_carries_the_local_date() {
        assertEquals(
            "http://100.72.37.0:8000/whatsapp/send-summary?date=2026-09-15",
            SummarySchedule.summaryUrl("http://100.72.37.0:8000/ingest", LocalDate.of(2026, 9, 15)),
        )
    }

    @Test
    fun summary_url_tolerates_a_trailing_slash_on_the_ingest_url() {
        assertEquals(
            "http://100.72.37.0:8000/whatsapp/send-summary?date=2026-01-02",
            SummarySchedule.summaryUrl("http://100.72.37.0:8000/ingest/", LocalDate.of(2026, 1, 2)),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun summary_url_rejects_an_ingest_url_it_cannot_derive_from() {
        SummarySchedule.summaryUrl("http://100.72.37.0:8000/something-else", LocalDate.of(2026, 9, 15))
    }

    // --- outcomeFor: what a send-summary response means for the worker --------

    @Test
    fun success_codes_mean_sent() {
        assertEquals(SummarySchedule.Outcome.SENT, SummarySchedule.outcomeFor(200))
        assertEquals(SummarySchedule.Outcome.SENT, SummarySchedule.outcomeFor(204))
    }

    @Test
    fun a_bad_secret_or_a_server_error_is_retried() {
        assertEquals(SummarySchedule.Outcome.RETRY, SummarySchedule.outcomeFor(401))
        assertEquals(SummarySchedule.Outcome.RETRY, SummarySchedule.outcomeFor(500))
        assertEquals(SummarySchedule.Outcome.RETRY, SummarySchedule.outcomeFor(503))
    }

    @Test
    fun other_client_errors_give_up() {
        // A 422 (e.g. a malformed date) won't be fixed by sending it again.
        assertEquals(SummarySchedule.Outcome.GIVE_UP, SummarySchedule.outcomeFor(422))
        assertEquals(SummarySchedule.Outcome.GIVE_UP, SummarySchedule.outcomeFor(404))
    }
}
