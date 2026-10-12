package com.appdony.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColors = darkColorScheme(
    primary = AppdonyGreen,
    onPrimary = Color.White,
    background = AppdonyBg,
    surface = AppdonySurface,
    onBackground = AppdonyTextPrimary,
    onSurface = AppdonyTextPrimary
)

private val LightColors = lightColorScheme(
    primary = AppdonyGreen,
    onPrimary = Color.White,
    background = Color.White,
    surface = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A)
)

@Composable
fun AppdonyTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content
        )
    }
}
