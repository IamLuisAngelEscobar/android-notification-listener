package com.daohoangson.n8n.notificationlistener.data.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A captured spend waiting to reach `/ingest` — the fail-safe buffer (Branch 17).
 *
 * Every capture is written here *before* any network call, and a WorkManager job
 * drains it. `payloadJson` is the fully-formed `/ingest` body, frozen at capture:
 * the drain worker only *reads* it, so `source.reference` and `occurred_at` are
 * replayed byte-for-byte on every retry and a re-drain stays a `duplicate: true`
 * no-op. `reference` carries a UNIQUE index so an OS re-post of the same
 * notification is ignored locally (insert-on-conflict-ignore) rather than queued
 * twice.
 */
@Entity(
    tableName = "pending_captures",
    indices = [Index(value = ["reference"], unique = true)],
)
data class PendingCapture(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val reference: String,       // == source.reference, the idempotency key
    val payloadJson: String,     // the frozen /ingest body, replayed verbatim
    val account: String,         // for display / debugging
    val createdAt: Long = System.currentTimeMillis(),
    val attemptCount: Int = 0,
)
