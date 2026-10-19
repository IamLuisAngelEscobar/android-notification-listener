package com.daohoangson.n8n.notificationlistener.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daohoangson.n8n.notificationlistener.ui.theme.AppTints
import com.daohoangson.n8n.notificationlistener.ui.theme.Geist
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerMotion
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerSpace

// components/data — AppIcon, AppRow, Badge, SectionHeader.

private val Grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * The app's real launcher icon in a 30%-radius squircle, or a tinted monogram
 * when there is none. `muted` (not listening) greys it out at 50%.
 */
@Composable
fun AppIcon(
    name: String,
    icon: ImageBitmap?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    muted: Boolean = false,
) {
    val alpha by animateFloatAsState(if (muted) 0.5f else 1f, ListenerMotion.base(), label = "iconAlpha")
    val shape = RoundedCornerShape(size * 0.3f)
    val base = modifier.size(size).alpha(alpha).clip(shape)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            colorFilter = if (muted) Grayscale else null,
            modifier = base,
        )
        return
    }
    val c = Listener.colors
    val tint = if (muted) c.text3 else AppTints[(name.hashCode() and Int.MAX_VALUE) % AppTints.size]
    Box(base.background(lerp(c.surface, tint, 0.14f)), contentAlignment = Alignment.Center) {
        Text(
            name.trim().take(1).uppercase(),
            style = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = (size.value * 0.42f).sp),
            color = tint,
        )
    }
}

/**
 * The starting-point list row: icon, name, meta, switch. 64dp, hairline
 * divider inset to the text column. Tapping the row calls [onClick] when given,
 * otherwise toggles.
 */
@Composable
fun AppRow(
    name: String,
    meta: String?,
    icon: ImageBitmap?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    metaIsMono: Boolean = false,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val c = Listener.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ListenerSpace.rowHeight)
            .clickable(
                interactionSource = null,
                indication = ripple(color = c.text1),
                onClick = onClick ?: { onCheckedChange(!checked) },
            )
            .drawBehind {
                if (divider) {
                    val start = (ListenerSpace.screenGutter + 54.dp).toPx()
                    drawLine(c.divider, Offset(start, size.height), Offset(size.width, size.height), 1.dp.toPx())
                }
            }
            .padding(horizontal = ListenerSpace.screenGutter, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AppIcon(name = name, icon = icon, muted = !checked)
        Column(Modifier.weight(1f)) {
            Text(name, style = Listener.type.rowTitle, color = c.text1, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta != null) {
                Text(
                    meta,
                    style = if (metaIsMono) Listener.type.mono else Listener.type.bodyS,
                    color = c.text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        ListenerSwitch(checked = checked, onCheckedChange = onCheckedChange, label = "Listen to $name")
    }
}

enum class BadgeTone { Neutral, Accent, Danger, Warning }

/** Status badge: 24dp pill with a 6dp dot; `pulse` is the one allowed loop (live "Listening"). */
@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    pulse: Boolean = false,
) {
    val c = Listener.colors
    val (bg, fg) = when (tone) {
        BadgeTone.Neutral -> c.surface2 to c.text2
        BadgeTone.Accent -> c.accentSoft to c.accentPressed
        BadgeTone.Danger -> c.dangerSoft to c.danger
        BadgeTone.Warning -> c.warningSoft to c.warning
    }
    val dotAlpha = if (pulse) {
        val t = rememberInfiniteTransition(label = "pulse")
        t.animateFloat(1f, 0.35f, infiniteRepeatable(tween(1000, easing = ListenerMotion.easeStandard), RepeatMode.Reverse), label = "dot").value
    } else 1f
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(start = 8.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).alpha(dotAlpha).background(fg, CircleShape))
        Text(text, style = Listener.type.label.copy(letterSpacing = 0.sp), color = fg)
    }
}

/** Uppercase section label with an optional text action on the right. */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val c = Listener.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 40.dp)
            .padding(start = ListenerSpace.screenGutter, end = ListenerSpace.screenGutter, top = 16.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text.uppercase(), style = Listener.type.label, color = c.text2)
        if (action != null) {
            Text(
                action,
                style = Listener.type.action,
                color = c.text1,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onAction)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/** A quiet monospace caption line, e.g. a footnote under a list. */
@Composable
fun MonoCaption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = Listener.type.mono, color = Listener.colors.text3, modifier = modifier)
}
