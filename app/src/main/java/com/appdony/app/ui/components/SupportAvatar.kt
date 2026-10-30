package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.appdony.app.R

/**
 * آواتار پشتیبان (عکس خودت).
 * - نرم و پستی بالا و پایین می‌ره
 * - موقع [talking] لب‌هاش تکون می‌خوره
 * - [look] از ۰ تا ۱: ذره‌بین میاد جلوی چشم و سرش کمی کج می‌شه (نگاه به صفحه / طرف مقابل)
 */
@Composable
fun SupportAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 56,
    talking: Boolean = false,
    look: Float = 0f
) {
    val infinite = rememberInfiniteTransition(label = "support_avatar")
    val bob by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val talkT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(190, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "talk"
    )
    val talkAmt by animateFloatAsState(if (talking) 1f else 0f, tween(200), label = "talk_amt")
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = (bob * 5f - 2.5f).dp)
            .graphicsLayer { rotationZ = look * -6f }
            .semantics { contentDescription = "پشتیبان" }
    ) {
        Image(
            painter = painterResource(R.drawable.support_avatar),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(2.dp, primary, CircleShape)
        )
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val w = size.width
            val h = size.height

            // لب‌ها: روی دهن عکس، با باز و بسته شدن نرم
            val open = talkT * talkAmt
            if (open > 0.04f) {
                val mw = w * (0.11f + 0.02f * open)
                val mh = h * (0.012f + 0.045f * open)
                drawRoundRect(
                    color = Color(0xFF2B1210).copy(alpha = 0.88f),
                    topLeft = Offset(w * 0.557f - mw / 2f, h * 0.505f - mh / 2f),
                    size = Size(mw, mh),
                    cornerRadius = CornerRadius(mh, mh)
                )
            }

            // ذره‌بین: از گوشه‌ی پایین میاد بالا جلوی چشم
            if (look > 0.01f) {
                val cx = w * (0.95f - 0.33f * look)
                val cy = h * (1.0f - 0.62f * look)
                val r = w * 0.20f
                val glass = Color(0xFFE8E8E8)
                // دسته
                drawLine(
                    color = Color(0xFF7A4A24).copy(alpha = look),
                    start = Offset(cx + r * 0.70f, cy + r * 0.70f),
                    end = Offset(cx + r * 1.45f, cy + r * 1.45f),
                    strokeWidth = w * 0.045f,
                    cap = StrokeCap.Round
                )
                // شیشه
                drawCircle(color = Color.White.copy(alpha = 0.16f * look), radius = r, center = Offset(cx, cy))
                drawCircle(
                    color = glass.copy(alpha = look),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = w * 0.035f)
                )
                // برق شیشه
                drawArc(
                    color = Color.White.copy(alpha = 0.8f * look),
                    startAngle = 200f,
                    sweepAngle = 50f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.62f, cy - r * 0.62f),
                    size = Size(r * 1.24f, r * 1.24f),
                    style = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
