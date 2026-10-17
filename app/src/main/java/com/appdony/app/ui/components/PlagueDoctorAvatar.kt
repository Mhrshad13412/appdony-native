package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true
) {
    // انیمیشن شناور بودن (بالا و پایین رفتن نرم)
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // انیمیشن چشمک زدن
    val blink by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blink"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // ۱. هاله پشت شخصیت
            drawCircle(
                color = Color(0xFF1E2225), // رنگ تیره
                radius = w * 0.45f,
                center = Offset(cx, cy)
            )

            // ۲. کلاه
            val hatPath = Path().apply {
                moveTo(cx - w * 0.28f, cy - h * 0.1f)
                lineTo(cx - w * 0.2f, cy - h * 0.45f)
                lineTo(cx + w * 0.2f, cy - h * 0.45f)
                lineTo(cx + w * 0.28f, cy - h * 0.1f)
                close()
            }
            drawPath(hatPath, color = Color(0xFF2C3E50))
            drawRect( // لبه کلاه
                color = Color(0xFF34495E),
                topLeft = Offset(cx - w * 0.32f, cy - h * 0.12f),
                size = Size(w * 0.64f, h * 0.06f)
            )

            // ۳. ماسک (منقار)
            val maskPath = Path().apply {
                moveTo(cx - w * 0.22f, cy - h * 0.05f)
                quadraticTo(cx - w * 0.1f, cy + h * 0.25f, cx + w * 0.05f, cy + h * 0.3f) // منقار
                quadraticTo(cx + w * 0.15f, cy + h * 0.1f, cx + w * 0.22f, cy - h * 0.05f)
                close()
            }
            drawPath(maskPath, color = Color(0xFFECF0F1)) // رنگ سفید استخوانی
            drawPath(maskPath, color = Color(0xFFBDC3C7), style = Stroke(width = 2.dp.toPx()))

            // ۴. چشم‌ها (شیشه‌ای و آبی)
            val eyeRadius = w * 0.08f
            drawCircle(color = Color(0xFF3498DB), radius = eyeRadius, center = Offset(cx - w * 0.12f, cy + h * 0.02f))
            drawCircle(color = Color(0xFF3498DB), radius = eyeRadius, center = Offset(cx + w * 0.12f, cy + h * 0.02f))
            // هایلایت چشم
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(cx - w * 0.14f, cy - h * 0.01f))
            drawCircle(color = Color.White, radius = eyeRadius * 0.4f, center = Offset(cx + w * 0.10f, cy - h * 0.01f))
            
            // ۵. نوار دور گردن
            drawRoundRect(
                color = Color(0xFF2C3E50),
                topLeft = Offset(cx - w * 0.25f, cy + h * 0.32f),
                size = Size(w * 0.5f, h * 0.08f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
            )
        }
    }
}
