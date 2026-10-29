package com.appdony.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp

// ───────────────────────── رسم کاموا ─────────────────────────

/** گلوله‌ی کاموا: چند دور نخ دور هم می‌پیچه و با [spin] می‌چرخه */
internal fun DrawScope.drawYarnBall(center: Offset, r: Float, color: Color, spin: Float) {
    val dark = lerp(color, Color.Black, 0.28f)
    val light = lerp(color, Color.White, 0.35f)
    drawCircle(color, r, center)
    val clip = Path().apply { addOval(Rect(center, r)) }
    clipPath(clip) {
        val sw = r * 0.09f
        for (k in 0..4) {
            withTransform({ rotate(spin + k * 36f, center) }) {
                drawOval(
                    color = if (k % 2 == 0) dark else light,
                    topLeft = Offset(center.x - r * 1.05f, center.y - r * 0.42f),
                    size = Size(r * 2.1f, r * 0.84f),
                    style = Stroke(width = sw)
                )
            }
        }
    }
    drawCircle(dark, r, center, style = Stroke(width = r * 0.07f))
    drawCircle(Color.White.copy(alpha = 0.5f), r * 0.13f, Offset(center.x - r * 0.4f, center.y - r * 0.45f))
}
