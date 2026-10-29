package com.appdony.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

object AppdonyPalette {
    var isDark by mutableStateOf(true)
    var primary by mutableStateOf(Color(0xFF0D7377))
}

private fun tint(base: Color, amount: Float): Color = lerp(base, AppdonyPalette.primary, amount)

val AppdonyGreen: Color get() = AppdonyPalette.primary
val AppdonyGreenLight: Color get() = lerp(AppdonyPalette.primary, Color.White, 0.25f)

val AppdonyBg: Color
    get() = if (AppdonyPalette.isDark) tint(Color(0xFF0E1113), 0.05f) else tint(Color(0xFFF7F8FA), 0.04f)

val AppdonySurface: Color
    get() = if (AppdonyPalette.isDark) tint(Color(0xFF171B1E), 0.07f) else tint(Color(0xFFFFFFFF), 0.02f)

val AppdonyCardBg: Color
    get() = if (AppdonyPalette.isDark) tint(Color(0xFF1E2327), 0.09f) else tint(Color(0xFFECEFF3), 0.07f)

val AppdonyOutline: Color
    get() = if (AppdonyPalette.isDark) tint(Color(0xFF2C3338), 0.15f) else tint(Color(0xFFD3D9E0), 0.15f)

val AppdonyTextPrimary: Color
    get() = if (AppdonyPalette.isDark) Color(0xFFE8EAED) else Color(0xFF15181B)

val AppdonyTextMuted: Color
    get() = if (AppdonyPalette.isDark) Color(0xFF9AA0A6) else Color(0xFF5F6670)
