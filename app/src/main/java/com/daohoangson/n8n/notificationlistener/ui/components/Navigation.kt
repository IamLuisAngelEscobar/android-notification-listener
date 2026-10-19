package com.daohoangson.n8n.notificationlistener.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerSpace

// components/navigation — TopBar.

/**
 * 64dp top bar. `large` (top-level screens) puts the 34px display title under
 * the bar with an optional [subtitle]; otherwise the 17px title sits in the bar.
 */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    onBack: (() -> Unit)? = null,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Listener.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ListenerSpace.topBarHeight)
                .padding(start = if (onBack != null) 8.dp else ListenerSpace.screenGutter, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (onBack != null) ListenerIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            if (large) {
                Spacer(Modifier.weight(1f))
            } else {
                Text(
                    title,
                    style = Listener.type.title,
                    color = c.text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = if (onBack != null) 4.dp else 0.dp),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = actions)
        }
        if (large) {
            Column(Modifier.padding(start = ListenerSpace.screenGutter, end = ListenerSpace.screenGutter, top = 4.dp, bottom = 16.dp)) {
                Text(title, style = Listener.type.display, color = c.text1)
                if (subtitle != null) {
                    Spacer(Modifier.height(8.dp))
                    subtitle()
                }
            }
        }
    }
}
