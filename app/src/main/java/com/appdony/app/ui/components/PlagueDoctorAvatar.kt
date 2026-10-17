package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // کلاه
            val hatPath = Path().apply {
                moveTo(cx - w * 0.28f, cy - h * 0.05f)
                lineTo(cx - w * 0.2f, cy - h * 0.45f)
                lineTo(cx + w * 0.2f, cy - h * 0.45f)
                lineTo(cx + w * 0.28f, cy - h * 0.05f)
                close()
            }
            drawPath(hatPath, color = Color(0xFF34495E))
            drawRect(
                color = Color(0xFF2C3E50),
                topLeft = Offset(cx - w * 0.35f, cy - h * 0.1f),
                size = Size(w * 0.7f, h * 0.08f)
            )

            // ماسک
            val maskPath = Path().apply {
                moveTo(cx - w * 0.22f, cy - h * 0.02f)
                quadraticTo(cx - w * 0.1f, cy + h * 0.35f, cx + w * 0.05f, cy + h * 0.4f)
                quadraticTo(cx + w * 0.15f, cy + h * 0.15f, cx + w * 0.22f, cy - h * 0.02f)
                close()
            }
            drawPath(maskPath, color = Color(0xFFF5F5F5))
            drawPath(maskPath, color = Color(0xFFB0BEC5), style = Stroke(width = 3f))

            // چشم‌ها
            val eyeRadius = w * 0.09f
            drawCircle(color = Color(0xFF00BCD4), radius = eyeRadius, center = Offset(cx - w * 0.12f, cy + h * 0.05f))
            drawCircle(color = Color(0xFF00BCD4), radius = eyeRadius, center = Offset(cx + w * 0.12f, cy + h * 0.05f))
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(cx - w * 0.14f, cy + h * 0.02f))
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(cx + w * 0.10f, cy + h * 0.02f))
        }
    }
}
