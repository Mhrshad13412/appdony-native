package com.appdony.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AppdonyTheme(
    darkTheme: Boolean = true,
    primaryColor: Long = 0xFF0D7377,
    content: @Composable () -> Unit
) {
    val primary = Color(primaryColor)

    if (AppdonyPalette.isDark != darkTheme) AppdonyPalette.isDark = darkTheme
    if (AppdonyPalette.primary != primary) AppdonyPalette.primary = primary

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.22f),
            onPrimaryContainer = AppdonyTextPrimary,
            secondary = primary,
            onSecondary = Color.White,
            background = AppdonyBg,
            onBackground = AppdonyTextPrimary,
            surface = AppdonySurface,
            onSurface = AppdonyTextPrimary,
            surfaceVariant = AppdonyCardBg,
            onSurfaceVariant = AppdonyTextMuted,
            outline = AppdonyOutline,
            outlineVariant = AppdonyOutline,
            error = Color(0xFFFF6B6B)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.14f),
            onPrimaryContainer = AppdonyTextPrimary,
            secondary = primary,
            onSecondary = Color.White,
            background = AppdonyBg,
            onBackground = AppdonyTextPrimary,
            surface = AppdonySurface,
            onSurface = AppdonyTextPrimary,
            surfaceVariant = AppdonyCardBg,
            onSurfaceVariant = AppdonyTextMuted,
            outline = AppdonyOutline,
            outlineVariant = AppdonyOutline,
            error = Color(0xFFD93025)
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colors,
            shapes = AppShapes,
            content = content
        )
    }
}
