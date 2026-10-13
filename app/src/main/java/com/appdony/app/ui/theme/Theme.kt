package com.appdony.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.appdony.app.data.preferences.FontSize

@Composable
fun AppdonyTheme(
    darkTheme: Boolean = true,
    primaryColor: Long = 0xFF0D7377,
    fontSize: FontSize = FontSize.MEDIUM,
    content: @Composable () -> Unit
) {
    val primary = Color(primaryColor)

    val darkColors = darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        background = Color(0xFF121517),
        surface = Color(0xFF1E2225),
        onBackground = Color(0xFFE6E1E5),
        onSurface = Color(0xFFE6E1E5)
    )

    val lightColors = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        background = Color(0xFFFAFAFA),
        surface = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1A1A1A),
        onSurface = Color(0xFF1A1A1A)
    )

    val colors = if (darkTheme) darkColors else lightColors

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography.withScale(fontSize.scale),
            content = content
        )
    }
}
