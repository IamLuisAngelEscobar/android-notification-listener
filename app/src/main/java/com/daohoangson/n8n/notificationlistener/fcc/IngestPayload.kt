package com.daohoangson.n8n.notificationlistener.fcc

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * The exact body the Financial Command Center `/ingest` endpoint accepts.
 *
 * The server model is declared with `extra = "forbid"`, so the JSON must carry
 * *only* these keys — any stray field is a 422. The property names are the
 * snake_case wire names on purpose: Gson serializes them verbatim, so we never
 * emit a camelCase alias the server would reject.
 *
 * `amount` is a plain number and **positive** for a spend (the server classifies
 * by descriptor, not by sign; the canonical payload uses a positive magnitude).
 * `occurred_at` is ISO-8601 *with offset* and, together with `source.reference`,
 * is assigned once at capture and replayed unchanged on every retry — that is
 * what makes a re-drain a `duplicate: true` no-op instead of a second row.
 */
data class IngestPayload(
    val account: String,
    val amount: Double,
    val currency: String,
    val description: String,
    val occurred_at: String,
    val source: IngestSource,
) {
    fun toJson(): String = GSON.toJson(this)

    companion object {
        // Non-null-serializing Gson: `source.app` / `source.raw_text` are simply
        // omitted when null (both are optional server-side) rather than sent as
        // JSON null. account/amount/currency/description/occurred_at are always set.
        private val GSON: Gson = GsonBuilder().create()
    }
}

data class IngestSource(
    val reference: String,
    val app: String? = null,
    val raw_text: String? = null,
)
