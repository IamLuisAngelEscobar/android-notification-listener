package com.daohoangson.n8n.notificationlistener.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener

// components/core — Button, IconButton. Icons are Material Icons Outlined, the
// vector stand-in for the system's Material Symbols Rounded (weight 300, FILL 0).

enum class ButtonVariant { Primary, Accent, Secondary, Ghost, Danger }

/**
 * Pill button, 48dp. Primary is ink; Accent (green) is reserved for turning
 * listening on. Press = 8% overlay + scale(0.98), no color shift.
 */
@Composable
fun ListenerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val c = Listener.colors
    val (bg, fg, border) = when (variant) {
        ButtonVariant.Primary -> Triple(c.primary, c.onPrimary, Color.Transparent)
        ButtonVariant.Accent -> Triple(c.accent, c.onAccent, Color.Transparent)
        ButtonVariant.Secondary -> Triple(Color.Transparent, c.text1, c.borderStrong)
        ButtonVariant.Ghost -> Triple(Color.Transparent, c.text1, Color.Transparent)
        ButtonVariant.Danger -> Triple(Color.Transparent, c.danger, c.borderStrong)
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .height(48.dp)
            .scale(if (pressed && enabled) 0.98f else 1f)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(bg)
            .border(BorderStroke(1.dp, border), CircleShape)
            .background(if (pressed && enabled) c.statePressed else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = Listener.type.rowTitle, color = fg)
    }
}

/** Round 40dp icon button; `badge` draws the signal dot used for "something to review". */
@Composable
fun ListenerIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    badge: Boolean = false,
) {
    val c = Listener.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pressed) c.statePressed else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = c.text1, modifier = Modifier.size(size * 0.55f))
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(c.accent)
            )
        }
    }
}
