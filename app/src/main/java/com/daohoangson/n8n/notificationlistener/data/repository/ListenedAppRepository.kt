package com.daohoangson.n8n.notificationlistener.data.repository

import com.daohoangson.n8n.notificationlistener.data.database.ListenedApp
import com.daohoangson.n8n.notificationlistener.data.database.ListenedAppDao
import com.daohoangson.n8n.notificationlistener.fcc.AccountRule
import com.daohoangson.n8n.notificationlistener.fcc.IngestConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The owner's choice of which apps the listener captures, persisted in
 * `listened_apps` and merged over the built-in rules by
 * [IngestConfig.effectiveRules].
 */
@Singleton
class ListenedAppRepository @Inject constructor(
    private val dao: ListenedAppDao,
) {
    fun observeChoices(): Flow<List<ListenedApp>> = dao.observeAll()

    /** Read per capture: the table is tiny and a fresh read never serves a stale choice. */
    suspend fun activeRules(): List<AccountRule> = IngestConfig.effectiveRules(dao.getAll())

    suspend fun listen(packageName: String, account: String, defaultCurrency: String) {
        dao.upsert(
            ListenedApp(
                packageName = packageName,
                account = account.trim(),
                defaultCurrency = defaultCurrency.trim().uppercase(),
                enabled = true,
            )
        )
    }

    /**
     * Stop listening. Always leaves a disabled row rather than deleting: for a
     * built-in, deleting would bring the shipped rule back; for any app, the row
     * remembers the account so switching it back on needs no re-entry.
     */
    suspend fun stopListening(packageName: String) {
        val current = dao.getAll().firstOrNull { it.packageName == packageName }
        val builtIn = IngestConfig.ruleFor(packageName)
        val account = current?.account ?: builtIn?.account ?: return
        val currency = current?.defaultCurrency ?: builtIn?.defaultCurrency ?: return
        dao.upsert(
            ListenedApp(
                packageName = packageName,
                account = account,
                defaultCurrency = currency,
                enabled = false,
            )
        )
    }
}
