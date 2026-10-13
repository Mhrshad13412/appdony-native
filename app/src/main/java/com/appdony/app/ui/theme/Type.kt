package com.appdony.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp
    )
)

fun Typography.withScale(scale: Float): Typography = Typography(
    titleLarge = titleLarge.copy(fontSize = (20 * scale).sp),
    titleMedium = titleMedium.copy(fontSize = (16 * scale).sp),
    bodyLarge = bodyLarge.copy(fontSize = (15 * scale).sp),
    bodyMedium = bodyMedium.copy(fontSize = (13 * scale).sp),
    labelSmall = labelSmall.copy(fontSize = (11 * scale).sp)
)
