package com.daohoangson.n8n.notificationlistener.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.ui.components.ListenerButton
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerSpace

/**
 * Welcome / permission — the Listener UI kit's OnboardingScreen. Shown until
 * the notification-listener grant is on; the button opens Android settings.
 */
@Composable
fun OnboardingScreen(onGrant: () -> Unit) {
    val c = Listener.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .safeDrawingPadding()
            .padding(start = ListenerSpace.screenGutter, end = ListenerSpace.screenGutter, bottom = 24.dp)
    ) {
        // Wordmark: the name in Geist Medium with an 8px signal dot. No logo.
        Row(Modifier.height(64.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).background(c.accent, CircleShape))
            Text("Listener", style = Listener.type.rowTitle, color = c.text1)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(56.dp).background(c.surface2, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Hearing, contentDescription = null, tint = c.text1, modifier = Modifier.size(28.dp))
            }
            Text("Capture only the apps you choose.", style = Listener.type.display, color = c.text1)
            Text(
                "Listener reads spend notifications from the apps you pick and sends them to Financial Command Center over Tailscale.",
                style = Listener.type.body,
                color = c.text2,
            )
            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Point(Icons.Outlined.Shield, "Apps you have not turned on are never read")
                Point(Icons.Outlined.Tune, "Change your apps any time")
                Point(Icons.Outlined.NotificationsOff, "Original notifications are not touched")
            }
        }
        ListenerButton(text = "Turn on notification access", onClick = onGrant, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Opens Android settings",
            style = Listener.type.bodyS,
            color = c.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    val c = Listener.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = c.text2, modifier = Modifier.size(20.dp))
        Text(text, style = Listener.type.bodyS, color = c.text1)
    }
}
