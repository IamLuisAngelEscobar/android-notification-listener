package com.daohoangson.n8n.notificationlistener.ui.apps

import com.daohoangson.n8n.notificationlistener.data.database.ListenedApp
import com.daohoangson.n8n.notificationlistener.fcc.IngestConfig

/**
 * One installed app as the Apps screen shows it. [account] / [defaultCurrency]
 * are what the app maps to while listened, or would map to if switched back on
 * (a built-in default or a remembered choice); null means the owner must pick an
 * account before it can be listened.
 */
data class AppListing(
    val packageName: String,
    val label: String,
    val listened: Boolean,
    val account: String?,
    val defaultCurrency: String?,
    val builtInAccount: String?,
)

/** Pure: installed (packageName → label) + the owner's choices → rows, in label order. */
fun buildListings(installed: List<Pair<String, String>>, choices: List<ListenedApp>): List<AppListing> {
    val active = IngestConfig.effectiveRules(choices).associateBy { it.packageName }
    val byPackage = choices.associateBy { it.packageName }
    return installed.map { (packageName, label) ->
        val rule = active[packageName]
        val choice = byPackage[packageName]
        val builtIn = IngestConfig.ruleFor(packageName)
        AppListing(
            packageName = packageName,
            label = label,
            listened = rule != null,
            account = rule?.account ?: choice?.account ?: builtIn?.account,
            defaultCurrency = rule?.defaultCurrency ?: choice?.defaultCurrency ?: builtIn?.defaultCurrency,
            builtInAccount = builtIn?.account,
        )
    }
}
