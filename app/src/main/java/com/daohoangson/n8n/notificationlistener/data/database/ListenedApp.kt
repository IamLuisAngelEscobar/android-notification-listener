package com.daohoangson.n8n.notificationlistener.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The owner's on-device choice for one app: listen to it or not, and which
 * seeded account its spends land in.
 *
 * A row for a built-in package ([com.daohoangson.n8n.notificationlistener.fcc.IngestConfig.rules])
 * overrides that rule's switch and account; a row for any other package adds a
 * rule that uses the generic amount/noise regexes. A built-in package with no
 * row keeps its shipped behaviour (listened), so a fresh install captures
 * exactly what it did before this table existed.
 *
 * `account` must match a **seeded account name** exactly, or `/ingest` 422s.
 */
@Entity(tableName = "listened_apps")
data class ListenedApp(
    @PrimaryKey
    val packageName: String,
    val account: String,
    val defaultCurrency: String,
    val enabled: Boolean,
)
