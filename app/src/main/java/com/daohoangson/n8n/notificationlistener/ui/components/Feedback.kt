package com.daohoangson.n8n.notificationlistener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerSpace

// components/feedback — BottomSheet, Snackbar, EmptyState.

/** Surface sheet with a 28dp top radius, a 32×4 handle and the 40% scrim. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenerBottomSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Listener.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = ListenerSpace.radiusXl, topEnd = ListenerSpace.radiusXl),
        containerColor = c.surface,
        contentColor = c.text1,
        scrimColor = c.scrim,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 14.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .background(c.borderStrong, CircleShape)
            )
        },
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 24.dp)) {
            if (title != null) {
                Text(
                    title,
                    style = Listener.type.title,
                    color = c.text1,
                    modifier = Modifier.padding(start = ListenerSpace.screenGutter, end = ListenerSpace.screenGutter, bottom = 8.dp),
                )
            }
            content()
        }
    }
}

/** Inverse floating bar with an optional underlined action ("Undo"). */
@Composable
fun ListenerSnackbar(
    message: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val c = Listener.colors
    val shape = RoundedCornerShape(ListenerSpace.radiusSm)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .shadow(12.dp, shape, ambientColor = c.scrim, spotColor = c.scrim)
            .background(c.surfaceInverse, shape)
            // Swallow taps on the bar itself so they never reach the row underneath.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(message, style = Listener.type.bodyS, color = c.textInverse, modifier = Modifier.weight(1f).padding(vertical = 14.dp))
        if (action != null) {
            Text(
                action,
                style = Listener.type.action.copy(textDecoration = TextDecoration.Underline),
                color = c.textInverse,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .defaultMinSize(minWidth = ListenerSpace.touchTarget, minHeight = ListenerSpace.touchTarget)
                    .wrapContentSize()
                    .padding(horizontal = 10.dp),
            )
        }
    }
}

/** 64dp icon disc, a 2–3 word title and one sentence. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val c = Listener.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.padding(bottom = 4.dp).size(64.dp).background(c.surface2, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.text2, modifier = Modifier.size(30.dp))
        }
        Text(title, style = Listener.type.title, color = c.text1, textAlign = TextAlign.Center)
        if (body != null) {
            Text(body, style = Listener.type.bodyS, color = c.text2, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp))
        }
        if (action != null) {
            Spacer(Modifier.height(8.dp))
            action()
        }
    }
}
