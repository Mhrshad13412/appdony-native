package com.appdony.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp

// ───────────────────────── رسم حباب ─────────────────────────

/** حباب شیشه‌ای: بدنه‌ی نیمه‌شفاف، لبه‌ی روشن و دو برق؛ با [spin] برقش کمی می‌چرخه */
internal fun DrawScope.drawBubble(center: Offset, r: Float, color: Color, spin: Float) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.10f), color.copy(alpha = 0.28f), color.copy(alpha = 0.62f)),
            center = center,
            radius = r
        ),
        radius = r,
        center = center
    )
    drawCircle(Color.White.copy(alpha = 0.65f), r, center, style = Stroke(width = r * 0.07f))
    drawCircle(
        lerp(color, Color.White, 0.4f).copy(alpha = 0.45f),
        r * 0.86f,
        center,
        style = Stroke(width = r * 0.05f)
    )
    val shift = Math.sin(Math.toRadians(spin.toDouble())).toFloat() * 8f
    val hr = r * 0.74f
    drawArc(
        color = Color.White.copy(alpha = 0.85f),
        startAngle = 200f + shift,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(center.x - hr, center.y - hr),
        size = Size(hr * 2f, hr * 2f),
        style = Stroke(width = r * 0.11f, cap = StrokeCap.Round)
    )
    drawArc(
        color = Color.White.copy(alpha = 0.35f),
        startAngle = 20f - shift,
        sweepAngle = 30f,
        useCenter = false,
        topLeft = Offset(center.x - hr, center.y - hr),
        size = Size(hr * 2f, hr * 2f),
        style = Stroke(width = r * 0.07f, cap = StrokeCap.Round)
    )
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.09f, Offset(center.x - r * 0.3f, center.y - r * 0.52f))
}
