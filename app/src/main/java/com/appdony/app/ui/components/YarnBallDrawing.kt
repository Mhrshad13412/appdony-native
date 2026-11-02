package com.appdony.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp

// ───────────────────────── رسم توپ ─────────────────────────
// اسم تابع همون قبلی مونده تا بقیه‌ی فایل‌ها دست نخورن؛ حالا یه توپ براق با درزهاست.

/** توپ براق: دو درز دور هم می‌چرخن و با [spin] می‌چرخه */
internal fun DrawScope.drawYarnBall(center: Offset, r: Float, color: Color, spin: Float) {
    val dark = lerp(color, Color.Black, 0.30f)
    val light = lerp(color, Color.White, 0.45f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(light, color, dark),
            center = Offset(center.x - r * 0.35f, center.y - r * 0.4f),
            radius = r * 1.5f
        ),
        radius = r,
        center = center
    )
    val clip = Path().apply { addOval(Rect(center, r)) }
    clipPath(clip) {
        for (k in 0..1) {
            withTransform({ rotate(spin + k * 90f, center) }) {
                drawOval(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = Offset(center.x - r * 0.42f, center.y - r * 1.05f),
                    size = Size(r * 0.84f, r * 2.1f),
                    style = Stroke(width = r * 0.12f)
                )
            }
        }
    }
    drawCircle(dark, r, center, style = Stroke(width = r * 0.07f))
    drawCircle(Color.White.copy(alpha = 0.65f), r * 0.16f, Offset(center.x - r * 0.38f, center.y - r * 0.42f))
}
