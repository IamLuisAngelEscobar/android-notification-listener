package com.daohoangson.n8n.notificationlistener.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalListenerColors = staticCompositionLocalOf { LightListenerColors }
private val LocalListenerType = staticCompositionLocalOf { ListenerType }

/** Read the Listener tokens: `Listener.colors.accent`, `Listener.type.title`. */
object Listener {
    val colors: ListenerColors
        @Composable @ReadOnlyComposable get() = LocalListenerColors.current

    val type: ListenerTypography
        @Composable @ReadOnlyComposable get() = LocalListenerType.current
}

/** tokens/spacing.css and tokens/radius-elevation.css. */
object ListenerSpace {
    val screenGutter = 20.dp
    val rowHeight = 64.dp
    val touchTarget = 48.dp
    val topBarHeight = 64.dp
    val radiusSm = 10.dp
    val radiusMd = 14.dp
    val radiusXl = 28.dp
}

/** tokens/motion.css: short and decelerating, no bounce. */
object ListenerMotion {
    val easeStandard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    fun <T> fast() = tween<T>(durationMillis = 120, easing = easeStandard)
    fun <T> base() = tween<T>(durationMillis = 200, easing = easeStandard)
}

/**
 * The Listener Design System theme. Follows the system dark setting; no dynamic
 * color, so the monochrome palette holds on every device. Material components
 * still used elsewhere (the capture list) get a matching neutral scheme and Geist.
 */
@Composable
fun ListenerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkListenerColors else LightListenerColors
    val scheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.accent, onSecondary = colors.onAccent,
            background = colors.bg, onBackground = colors.text1,
            surface = colors.bg, onSurface = colors.text1,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.text2,
            outline = colors.borderStrong, outlineVariant = colors.border,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            secondary = colors.accent, onSecondary = colors.onAccent,
            background = colors.bg, onBackground = colors.text1,
            surface = colors.bg, onSurface = colors.text1,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.text2,
            outline = colors.borderStrong, outlineVariant = colors.border,
            error = colors.danger,
        )
    }
    val type = ListenerType
    val material = Typography(
        headlineMedium = type.headline,
        titleLarge = type.title,
        titleMedium = type.rowTitle,
        bodyLarge = type.body,
        bodyMedium = type.bodyS,
        bodySmall = type.bodyS.copy(fontSize = type.mono.fontSize),
        labelLarge = type.rowTitle,
        labelMedium = type.label,
    )
    CompositionLocalProvider(
        LocalListenerColors provides colors,
        LocalListenerType provides type,
    ) {
        MaterialTheme(colorScheme = scheme, typography = material, content = content)
    }
}
