package com.daohoangson.n8n.notificationlistener.fcc

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * When and where the 9 PM daily-summary trigger fires (#80). Pure and unit-tested.
 *
 * The phone owns the clock (ADR-0011 §3): the backend's `POST /whatsapp/send-summary`
 * is passive and takes the phone's local date as a query parameter, so the summary
 * follows the owner across time zones. This replaces the Tasker job ADR-0011
 * specified, and lives here because this app already holds the tailnet URL and the
 * `X-Ingest-Secret`.
 */
object SummarySchedule {
    /** 21:00 on the phone's local clock. */
    val TRIGGER_TIME: LocalTime = LocalTime.of(21, 0)

    enum class Outcome { SENT, RETRY, GIVE_UP }

    /**
     * The next [at] strictly after [now], in [now]'s zone. Built from the local
     * date rather than `now + 24h`, so it stays at 21:00 wall-clock time across a
     * daylight-saving change.
     */
    fun nextTrigger(now: ZonedDateTime, at: LocalTime = TRIGGER_TIME): ZonedDateTime {
        val today = ZonedDateTime.of(now.toLocalDate(), at, now.zone)
        return if (today.isAfter(now)) today else ZonedDateTime.of(now.toLocalDate().plusDays(1), at, now.zone)
    }

    /**
     * The send-summary endpoint on the same box as [ingestUrl]
     * (`http://<tailnet-ip>:8000/ingest`), for [date].
     */
    fun summaryUrl(ingestUrl: String, date: LocalDate): String {
        val base = ingestUrl.trimEnd('/')
        require(base.endsWith("/ingest")) { "can't derive the summary URL from '$ingestUrl'" }
        return base.removeSuffix("/ingest") + "/whatsapp/send-summary?date=$date"
    }

    /**
     * What a send-summary response means, mirroring the capture drain's policy: a
     * bad secret (fixable config) or a server error is retried, and any other client
     * error won't be fixed by sending the same request again.
     */
    fun outcomeFor(httpCode: Int): Outcome = when {
        httpCode in 200..299 -> Outcome.SENT
        httpCode == 401 || httpCode >= 500 -> Outcome.RETRY
        else -> Outcome.GIVE_UP
    }
}
