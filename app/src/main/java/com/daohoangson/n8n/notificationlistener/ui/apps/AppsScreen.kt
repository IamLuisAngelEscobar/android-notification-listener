package com.daohoangson.n8n.notificationlistener.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.data.repository.ListenedAppRepository
import com.daohoangson.n8n.notificationlistener.ui.components.AppIcon
import com.daohoangson.n8n.notificationlistener.ui.components.AppRow
import com.daohoangson.n8n.notificationlistener.ui.components.BadgeTone
import com.daohoangson.n8n.notificationlistener.ui.components.ButtonVariant
import com.daohoangson.n8n.notificationlistener.ui.components.EmptyState
import com.daohoangson.n8n.notificationlistener.ui.components.FieldLabel
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerBottomSheet
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerButton
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerChip
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerIconButton
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerSnackbar
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerTextField
import com.daohoangson.n8n.notificationlistener.ui.components.MonoCaption
import com.daohoangson.n8n.notificationlistener.ui.components.SearchField
import com.daohoangson.n8n.notificationlistener.ui.components.SectionHeader
import com.daohoangson.n8n.notificationlistener.ui.components.StatusBadge
import com.daohoangson.n8n.notificationlistener.ui.components.TopBar
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerSpace
import com.daohoangson.n8n.notificationlistener.utils.InstalledApp
import com.daohoangson.n8n.notificationlistener.utils.InstalledApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class Snack(val message: String, val undo: (() -> Unit)? = null)

/** Which sheet is open: picking an account to start listening, or editing one. */
private data class SheetTarget(val app: AppListing, val starting: Boolean)

/**
 * Apps — the Listener UI kit's AppsScreen. Every installed launcher app, split
 * into "Listening" and "Other apps". Switching an app on that has no account yet
 * asks for one in a sheet; tapping a listened row edits its account; switching
 * one off offers Undo.
 */
@Composable
fun AppsScreen(
    listenedApps: ListenedAppRepository,
    reviewCount: Int,
    onReviewCaptures: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val c = Listener.colors

    val installed by produceState<List<InstalledApp>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { InstalledApps.load(context) }
    }
    val choices by listenedApps.observeChoices().collectAsState(initial = null)
    val icons = remember(installed) { installed.orEmpty().associate { it.packageName to it.icon } }
    val listings = remember(installed, choices) {
        val apps = installed ?: return@remember null
        buildListings(apps.map { it.packageName to it.label }, choices ?: return@remember null)
    }

    var query by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<SheetTarget?>(null) }
    var snack by remember { mutableStateOf<Snack?>(null) }
    LaunchedEffect(snack) {
        if (snack != null) {
            delay(3_500)
            snack = null
        }
    }

    fun listen(app: AppListing, account: String, currency: String) {
        scope.launch { listenedApps.listen(app.packageName, account, currency) }
    }

    fun setListening(app: AppListing, on: Boolean) {
        if (on) {
            val account = app.account
            val currency = app.defaultCurrency
            if (account != null && currency != null) listen(app, account, currency)
            else sheet = SheetTarget(app, starting = true)
            return
        }
        scope.launch { listenedApps.stopListening(app.packageName) }
        val account = app.account
        val currency = app.defaultCurrency
        snack = Snack(
            message = "Stopped listening to ${app.label}",
            undo = if (account != null && currency != null) ({ listen(app, account, currency) }) else null,
        )
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            val listeningCount = listings?.count { it.listened } ?: 0
            TopBar(
                title = "Apps",
                large = true,
                subtitle = {
                    StatusBadge(
                        text = if (listeningCount == 0) "Not listening" else "Listening to $listeningCount ${if (listeningCount == 1) "app" else "apps"}",
                        tone = if (listeningCount > 0) BadgeTone.Accent else BadgeTone.Neutral,
                        pulse = listeningCount > 0,
                    )
                },
                actions = {
                    ListenerIconButton(
                        icon = Icons.Outlined.Inbox,
                        label = if (reviewCount > 0) "Review $reviewCount captures" else "Review captures",
                        onClick = onReviewCaptures,
                        badge = reviewCount > 0,
                    )
                },
            )
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search installed apps",
                modifier = Modifier.fillMaxWidth().padding(start = ListenerSpace.screenGutter, end = ListenerSpace.screenGutter, bottom = 8.dp),
            )

            if (listings == null) {
                EmptyState(icon = Icons.Outlined.Apps, title = "Loading apps")
                return@Column
            }

            val q = query.trim().lowercase()
            val matches = listings.filter { q.isEmpty() || q in it.label.lowercase() || q in it.packageName }
            val on = matches.filter { it.listened }
            val off = matches.filterNot { it.listened }

            // No item keys on purpose: a keyed list anchors its scroll to the first
            // visible row, so switching an app on at the top would insert the
            // Listening section above the viewport and the app would vanish.
            LazyColumn(
                modifier = Modifier.weight(1f).imePadding(),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            ) {
                if (matches.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No matches",
                            body = "No installed app matches “${query.trim()}”.",
                        )
                    }
                }
                if (on.isNotEmpty()) item { SectionHeader("Listening") }
                itemsIndexed(on) { i, app ->
                    AppRow(
                        name = app.label,
                        meta = "${app.account} · ${app.defaultCurrency}",
                        icon = icons[app.packageName],
                        checked = true,
                        onCheckedChange = { setListening(app, it) },
                        onClick = { sheet = SheetTarget(app, starting = false) },
                        divider = i < on.lastIndex,
                    )
                }
                if (off.isNotEmpty()) item { SectionHeader("Other apps") }
                itemsIndexed(off) { i, app ->
                    AppRow(
                        name = app.label,
                        meta = app.packageName,
                        metaIsMono = true,
                        icon = icons[app.packageName],
                        checked = false,
                        onCheckedChange = { setListening(app, it) },
                        divider = i < off.lastIndex,
                    )
                }
                item {
                    MonoCaption(
                        "Only apps you turn on are read. Their spends go to Financial Command Center.",
                        modifier = Modifier.padding(horizontal = ListenerSpace.screenGutter, vertical = 24.dp),
                    )
                }
            }
        }

        snack?.let { s ->
            ListenerSnackbar(
                message = s.message,
                action = if (s.undo != null) "Undo" else null,
                onAction = {
                    s.undo?.invoke()
                    snack = null
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp),
            )
        }
    }

    sheet?.let { target ->
        AccountSheet(
            target = target,
            icon = icons[target.app.packageName],
            onDismiss = { sheet = null },
            onSave = { account, currency ->
                listen(target.app, account, currency)
                sheet = null
            },
        )
    }
}

private val Currencies = listOf("MXN", "USD")

@Composable
private fun AccountSheet(
    target: SheetTarget,
    icon: ImageBitmap?,
    onDismiss: () -> Unit,
    onSave: (account: String, currency: String) -> Unit,
) {
    val app = target.app
    val c = Listener.colors
    val keyboard = LocalSoftwareKeyboardController.current
    var account by rememberSaveable(app.packageName) { mutableStateOf(app.account.orEmpty()) }
    var currency by rememberSaveable(app.packageName) { mutableStateOf(app.defaultCurrency ?: "MXN") }
    val canSave = account.isNotBlank()

    fun close() {
        keyboard?.hide()
        onDismiss()
    }

    fun save() {
        keyboard?.hide()
        onSave(account, currency)
    }

    ListenerBottomSheet(onDismiss = ::close) {
        Column(Modifier.padding(horizontal = ListenerSpace.screenGutter)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppIcon(name = app.label, icon = icon, size = 36.dp)
                Column {
                    Text(app.label, style = Listener.type.rowTitle, color = c.text1)
                    Text(app.packageName, style = Listener.type.mono, color = c.text3)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                if (target.starting) "Choose an account" else "Account",
                style = Listener.type.title,
                color = c.text1,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Spends from ${app.label} are recorded under this account. Use its exact name in Financial Command Center, or they are rejected.",
                style = Listener.type.bodyS,
                color = c.text2,
            )
            Spacer(Modifier.height(20.dp))

            FieldLabel("Account")
            ListenerTextField(
                value = account,
                onValueChange = { account = it },
                placeholder = app.builtInAccount ?: "e.g. Nu Credit",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) save() }),
                modifier = Modifier.fillMaxWidth(),
            )
            if (app.builtInAccount != null && account.trim() != app.builtInAccount) {
                Spacer(Modifier.height(8.dp))
                Text("Built-in default: ${app.builtInAccount}", style = Listener.type.mono, color = c.text3)
            }
            Spacer(Modifier.height(20.dp))

            FieldLabel("Default currency")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (Currencies + listOfNotNull(app.defaultCurrency)).distinct().forEach { code ->
                    ListenerChip(text = code, selected = code == currency, onClick = { currency = code })
                }
            }
            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ListenerButton(
                    text = "Cancel",
                    onClick = ::close,
                    variant = ButtonVariant.Secondary,
                    modifier = Modifier.weight(1f),
                )
                ListenerButton(
                    text = if (target.starting) "Start listening" else "Save",
                    onClick = ::save,
                    variant = if (target.starting) ButtonVariant.Accent else ButtonVariant.Primary,
                    enabled = canSave,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
