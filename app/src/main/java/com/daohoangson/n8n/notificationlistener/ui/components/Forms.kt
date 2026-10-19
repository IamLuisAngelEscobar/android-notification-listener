package com.daohoangson.n8n.notificationlistener.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.daohoangson.n8n.notificationlistener.ui.theme.Listener
import com.daohoangson.n8n.notificationlistener.ui.theme.ListenerMotion

// components/forms — Switch, SearchField (and the plain TextField built on it), Chip.

/** 44×26 pill switch: green track when listening, 120ms track / 200ms thumb. */
@Composable
fun ListenerSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Listener.colors
    val track by animateColorAsState(if (checked) c.accent else c.borderStrong, ListenerMotion.fast(), label = "track")
    val thumbX by animateDpAsState(if (checked) 21.dp else 3.dp, ListenerMotion.base(), label = "thumb")
    Box(
        modifier = modifier
            .size(width = 44.dp, height = 26.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(track)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = onCheckedChange,
            )
            .semantics { contentDescription = label },
    ) {
        Box(
            Modifier
                .offset(x = thumbX, y = 3.dp)
                .size(20.dp)
                .shadow(1.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

/**
 * The pill input: 48dp, `surface-2` fill, 1px `border-strong` ring when focused.
 * With `leadingIcon = Search` it is the system's SearchField.
 */
@Composable
fun ListenerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val c = Listener.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = Listener.type.body.copy(color = c.text1),
        cursorBrush = SolidColor(c.text1),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(c.surface2)
                    .border(1.dp, if (focused) c.borderStrong else Color.Transparent, CircleShape)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, tint = c.text2, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = Listener.type.body, color = c.text3)
                    inner()
                }
                if (value.isNotEmpty()) {
                    ListenerIconButton(
                        icon = Icons.Outlined.Close,
                        label = "Clear",
                        onClick = { onValueChange("") },
                        size = 32.dp,
                    )
                }
            }
        },
    )
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) = ListenerTextField(value, onValueChange, placeholder, modifier, leadingIcon = Icons.Outlined.Search)

/** 32dp pill chip; selected = ink fill. */
@Composable
fun ListenerChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Listener.colors
    val bg by animateColorAsState(if (selected) c.primary else c.surface, ListenerMotion.fast(), label = "chip")
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, if (selected) c.primary else c.border, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = Listener.type.action, color = if (selected) c.onPrimary else c.text1)
    }
}

/** A field label: the system's uppercase 12px label, authored in sentence case. */
@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = Listener.type.label,
        color = Listener.colors.text2,
        modifier = modifier.padding(bottom = 8.dp),
    )
}
