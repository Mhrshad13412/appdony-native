package com.appdony.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * آواتار پشتیبان، کاملاً نقاشی‌شده با کد (بدون عکس).
 * - نرم و پستی بالا و پایین می‌ره
 * - پلک می‌زنه
 * - موقع [talking] لب‌هاش باز و بسته می‌شه
 * - [look] از ۰ تا ۱: ذره‌بین میاد جلوی چشم، چشم‌ها نگاه می‌کنن و سرش کمی کج می‌شه
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

    val blink = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2_200L..4_800L).random())
            blink.animateTo(1f, tween(80))
            blink.animateTo(0f, tween(130))
        }
    }

    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = (bob * 5f - 2.5f).dp)
            .graphicsLayer { rotationZ = look * -6f }
            .semantics { contentDescription = "پشتیبان" }
    ) {
        // خود آدم: روی بوم ۱۰۰×۱۰۰ کشیده شده و داخل دایره بریده می‌شه
        Canvas(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(2.dp, primary, CircleShape)
        ) {
            val k = size.width / 100f
            withTransform({ scale(k, k, Offset.Zero) }) {
                drawPerson(open = talkT * talkAmt, blink = blink.value, look = look)
            }
        }
        // ذره‌بین روی همه‌چیز، بدون بریدگی
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val k = size.width / 100f
            withTransform({ scale(k, k, Offset.Zero) }) {
                drawMagnifier(look)
            }
        }
    }
}

private val SkinColor = Color(0xFFD9A683)
private val SkinDark = Color(0xFFB98562)
private val HairColor = Color(0xFF17110F)
private val FrameColor = Color(0xFF4A3A2C)

private fun DrawScope.drawPerson(open: Float, blink: Float, look: Float) {
    // ── پس‌زمینه: کوچه‌ی تاریک با نورهای رنگی
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF4A3150), Color(0xFF1E1522)), startY = 0f, endY = 100f),
        size = Size(100f, 100f)
    )
    drawCircle(Color(0xFFFF5A4D).copy(alpha = 0.35f), 9f, Offset(84f, 22f))
    drawCircle(Color(0xFFFFD9A0).copy(alpha = 0.28f), 5f, Offset(14f, 16f))
    drawCircle(Color(0xFFFF5A4D).copy(alpha = 0.18f), 6f, Offset(92f, 62f))

    // ── لباس: کاپشن تیره و یقه‌ی بلند
    val jacket = Path().apply {
        moveTo(2f, 100f)
        cubicTo(4f, 86f, 16f, 79f, 34f, 75f)
        lineTo(66f, 75f)
        cubicTo(84f, 79f, 96f, 86f, 98f, 100f)
        close()
    }
    drawPath(jacket, Color(0xFF2C2522))
    // برق شونه‌ها
    drawLine(Color(0xFF4A3F38), Offset(14f, 84f), Offset(30f, 77f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(Color(0xFF4A3F38), Offset(86f, 84f), Offset(70f, 77f), strokeWidth = 1.2f, cap = StrokeCap.Round)

    // ── گردن
    drawRoundRect(SkinDark, Offset(42f, 56f), Size(16f, 20f), CornerRadius(5f, 5f))
    // یقه
    drawRoundRect(Color(0xFF1B1715), Offset(37f, 67f), Size(26f, 14f), CornerRadius(6.5f, 6.5f))
    drawLine(Color(0xFF3E3530), Offset(36f, 77f), Offset(50f, 83f), strokeWidth = 1f, cap = StrokeCap.Round)
    drawLine(Color(0xFF3E3530), Offset(64f, 77f), Offset(50f, 83f), strokeWidth = 1f, cap = StrokeCap.Round)
    // زیپ
    drawLine(Color(0xFF4A413B), Offset(50f, 83f), Offset(50f, 100f), strokeWidth = 0.9f)

    // ── گوش‌ها
    drawOval(Color(0xFFC48F6B), Offset(28f, 43f), Size(5.5f, 9.5f))
    drawOval(Color(0xFFC48F6B), Offset(66.5f, 43f), Size(5.5f, 9.5f))

    // ── صورت
    val head = Path().apply {
        moveTo(31f, 44f)
        cubicTo(31f, 30f, 40f, 23f, 50f, 23f)
        cubicTo(60f, 23f, 69f, 30f, 69f, 44f)
        cubicTo(69f, 57f, 60f, 70f, 50f, 70f)
        cubicTo(40f, 70f, 31f, 57f, 31f, 44f)
        close()
    }
    drawPath(head, SkinColor)
    clipPath(head) {
        // سایه‌ی سمت راست صورت
        drawRect(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, Color(0x40663F26)), startX = 46f, endX = 70f),
            topLeft = Offset(31f, 23f),
            size = Size(39f, 48f)
        )
        // سایه‌ی زیر چانه
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.Transparent, Color(0x33663F26)), startY = 58f, endY = 71f),
            topLeft = Offset(31f, 58f),
            size = Size(39f, 13f)
        )
    }
    // گونه‌ها
    drawCircle(Color(0xFFE08A7A).copy(alpha = 0.13f), 4.2f, Offset(38f, 56f))
    drawCircle(Color(0xFFE08A7A).copy(alpha = 0.13f), 4.2f, Offset(62f, 56f))

    // ── مو: تیره و پخش‌وپلا
    val hair = Path().apply {
        moveTo(28.5f, 47f)
        cubicTo(24f, 30f, 34f, 15.5f, 51f, 16.5f)
        cubicTo(68f, 17f, 77f, 31f, 71.5f, 47f)
        cubicTo(70.5f, 40f, 68f, 33f, 61f, 30.5f)
        cubicTo(56f, 34f, 47f, 31.5f, 41f, 35f)
        cubicTo(36f, 37f, 32f, 41f, 28.5f, 47f)
        close()
    }
    drawPath(hair, HairColor)
    // تارهای روشن‌تر مو
    val strand1 = Path().apply {
        moveTo(38f, 24f); cubicTo(44f, 19.5f, 55f, 19.5f, 63f, 24.5f)
    }
    drawPath(strand1, Color(0xFF3A2B26).copy(alpha = 0.8f), style = Stroke(0.9f, cap = StrokeCap.Round))
    val strand2 = Path().apply {
        moveTo(33f, 33f); cubicTo(35f, 27f, 41f, 23f, 47f, 22f)
    }
    drawPath(strand2, Color(0xFF3A2B26).copy(alpha = 0.7f), style = Stroke(0.8f, cap = StrokeCap.Round))
    val strand3 = Path().apply {
        moveTo(52f, 25f); cubicTo(59f, 24f, 66f, 28f, 69f, 36f)
    }
    drawPath(strand3, Color(0xFF3A2B26).copy(alpha = 0.6f), style = Stroke(0.8f, cap = StrokeCap.Round))

    // ── ابروها
    drawLine(Color(0xFF1A1210), Offset(36.5f, 40.8f), Offset(46.5f, 38.9f), strokeWidth = 2.8f, cap = StrokeCap.Round)
    drawLine(Color(0xFF1A1210), Offset(53.5f, 38.9f), Offset(63.5f, 40.8f), strokeWidth = 2.8f, cap = StrokeCap.Round)

    // ── چشم‌ها
    fun eye(cx: Float) {
        val cy = 46.5f
        val ry = 2.5f * (1f - blink * 0.92f)
        drawOval(Color(0xFFF3EAE2), Offset(cx - 3.6f, cy - ry), Size(7.2f, ry * 2f))
        if (blink < 0.5f) {
            val ix = cx + look * 1.2f
            val iy = cy + look * 0.5f
            drawCircle(Color(0xFF4A3426), 1.95f, Offset(ix, iy))
            drawCircle(Color(0xFF0E0807), 0.9f, Offset(ix, iy))
            drawCircle(Color.White.copy(alpha = 0.9f), 0.5f, Offset(ix + 0.6f, iy - 0.7f))
        }
        drawArc(
            color = Color(0xFF2A1A14),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(cx - 3.8f, cy - ry - 0.2f),
            size = Size(7.6f, ry * 2f + 0.4f),
            style = Stroke(0.9f, cap = StrokeCap.Round)
        )
    }
    eye(41.5f)
    eye(58.5f)

    // ── عینک گرد
    for (cx in listOf(41.5f, 58.5f)) {
        drawCircle(Color.White.copy(alpha = 0.08f), 6.5f, Offset(cx, 46.5f))
        drawCircle(FrameColor, 6.5f, Offset(cx, 46.5f), style = Stroke(1f))
        drawArc(
            color = Color.White.copy(alpha = 0.55f),
            startAngle = 205f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(cx - 4.7f, 46.5f - 4.7f),
            size = Size(9.4f, 9.4f),
            style = Stroke(0.7f, cap = StrokeCap.Round)
        )
    }
    val bridge = Path().apply {
        moveTo(47.9f, 45.4f); cubicTo(49f, 43.9f, 51f, 43.9f, 52.1f, 45.4f)
    }
    drawPath(bridge, FrameColor, style = Stroke(1f, cap = StrokeCap.Round))
    drawLine(FrameColor, Offset(35.1f, 45.6f), Offset(31f, 45f), strokeWidth = 0.9f, cap = StrokeCap.Round)
    drawLine(FrameColor, Offset(64.9f, 45.6f), Offset(69f, 45f), strokeWidth = 0.9f, cap = StrokeCap.Round)

    // ── بینی
    val nose = Path().apply {
        moveTo(50f, 48f); cubicTo(50.4f, 51f, 51.4f, 53.5f, 52.4f, 55.2f)
    }
    drawPath(nose, Color(0xFF9A6A4C).copy(alpha = 0.5f), style = Stroke(1f, cap = StrokeCap.Round))
    drawOval(Color(0xFF8E5B3C).copy(alpha = 0.18f), Offset(46.8f, 55f), Size(6.8f, 2.6f))

    // ── دهن
    val mx = 50f
    val my = 62.5f
    if (open < 0.05f) {
        val closed = Path().apply {
            moveTo(45.5f, my); cubicTo(47.5f, my + 1.2f, 52.5f, my + 1.2f, 54.5f, my)
        }
        drawPath(closed, Color(0xFF8A4A44), style = Stroke(1.5f, cap = StrokeCap.Round))
    } else {
        val h = 1.2f + open * 5f
        val top = my - h * 0.35f
        drawOval(Color(0xFF3E1A1A), Offset(mx - 4.2f, top), Size(8.4f, h))
        // دندان‌های بالا
        drawRoundRect(
            Color(0xFFF5EDE6).copy(alpha = 0.9f),
            Offset(mx - 3f, top + 0.3f),
            Size(6f, (h * 0.32f).coerceAtMost(1.5f)),
            CornerRadius(0.5f, 0.5f)
        )
        // لب‌ها
        drawOval(
            Color(0xFFB5645B),
            Offset(mx - 4.6f, top - 0.5f),
            Size(9.2f, h + 1f),
            style = Stroke(1.2f)
        )
    }
}

private fun DrawScope.drawMagnifier(look: Float) {
    if (look < 0.01f) return
    val cx = 92f - 32f * look
    val cy = 100f - 58f * look
    val r = 14f
    val d = 0.7071f
    val center = Offset(cx, cy)
    val handleStart = Offset(cx + r * d, cy + r * d)
    val handleEnd = Offset(cx + (r + 17f) * d, cy + (r + 17f) * d)

    // دسته و دست
    drawLine(Color(0xFF6B4423).copy(alpha = look), handleStart, handleEnd, strokeWidth = 3.6f, cap = StrokeCap.Round)
    drawCircle(SkinColor.copy(alpha = look), 4f, handleEnd)
    // شیشه
    drawCircle(Color(0xFFBFE3F5).copy(alpha = 0.16f * look), r, center)
    drawCircle(Color(0xFFD4AF37).copy(alpha = look), r, center, style = Stroke(2.6f))
    drawArc(
        color = Color.White.copy(alpha = 0.75f * look),
        startAngle = 200f,
        sweepAngle = 55f,
        useCenter = false,
        topLeft = Offset(cx - r * 0.66f, cy - r * 0.66f),
        size = Size(r * 1.32f, r * 1.32f),
        style = Stroke(1.2f, cap = StrokeCap.Round)
    )
}
