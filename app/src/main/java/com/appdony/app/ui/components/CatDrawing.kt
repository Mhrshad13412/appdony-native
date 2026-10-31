package com.appdony.app.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.appdony.app.ui.theme.AppdonyGreen
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// ───────────────────────── رسم گربه ─────────────────────────

/** حالت لحظه‌ای گربه؛ همه‌ی مقدارها بین ۰ و ۱ (یا ‎-۱ تا ۱) هستن */
internal class CatPose(
    val face: Float,     // ۱ = دم و کاموا سمت راست، ‎-۱ = برعکس
    val blink: Float,    // پلک
    val ear: Float,      // تکون خوردن گوش
    val tail: Float,     // تاب خوردن دم (‎-۱ تا ۱)
    val mouth: Float,    // باز بودن دهن
    val paw: Float,      // بالا آمدن پنجه
    val lookX: Float,    // نگاه چپ/راست
    val lookY: Float,    // نگاه بالا/پایین
    val scare: Float,    // بغل گرفته شدن: چشم درشت‌تر و گوش خوابیده
    val happy: Float,    // چشم‌های خندون ^ ^
    val wiggle: Float,   // قل خوردن گلوله‌ی کاموای کنار پا
    val yarn: Color = AppdonyGreen, // رنگ کاموا؛ پیش‌فرض رنگ تم برنامه
    val fur: Color = catFurOf(yarn), // رنگ خود گربه؛ از رنگ تم برنامه ساخته می‌شه
    val dizzy: Float = 0f,      // گیجی: چشم‌های مارپیچ و ستاره دور سر
    val spin: Float = 0f,       // زاویه‌ی چرخش مارپیچ و ستاره‌ها (درجه)
    val ballOff: Float = 0f,    // ۱ = کاموا از پای گربه جدا شده
    val ballX: Float = 0f,      // جای کاموای آزاد نسبت به گربه (dp)
    val ballY: Float = 0f,
    val ballSpin: Float = 0f
)

/** رنگ پوست گربه از روی رنگ تم برنامه: کمی روشن‌تر تا نرم و گربه‌ای بمونه */
internal fun catFurOf(theme: Color): Color = lerp(theme, Color.White, 0.30f)

internal fun DrawScope.drawCat(p: CatPose) {
    val u = 1.dp.toPx()
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    val line = 1.5f * u

    // رنگ‌های گربه همه از رنگ تم: پوست، راه‌راه، شکم/پنجه‌ها و خط دور
    val FurColor = p.fur
    val FurDark = lerp(p.fur, Color.Black, 0.22f)
    val CreamColor = lerp(p.fur, Color.White, 0.80f)
    val OutlineColor = lerp(p.fur, Color.Black, 0.62f)

    val ballFree = p.ballOff > 0.5f

    withTransform({ scale(p.face, 1f, pt(30f, 30f)) }) {

        // ── دم (پشت بدن) ──
        withTransform({ rotate(p.tail * 14f - p.dizzy * 10f, pt(20f, 52f)) }) {
            val tail = Path().apply {
                moveTo(20f * u, 52f * u)
                cubicTo(6f * u, 52f * u, 2f * u, 38f * u, 9f * u, 31f * u)
            }
            drawPath(tail, OutlineColor, style = Stroke(width = 7f * u, cap = StrokeCap.Round))
            drawPath(tail, FurColor, style = Stroke(width = 4.6f * u, cap = StrokeCap.Round))
            drawCircle(CreamColor, 2.3f * u, pt(9f, 31.2f))
        }

        // ── بدن و شکم ──
        drawOval(FurColor, pt(17f, 38f), Size(26f * u, 22f * u))
        drawOval(OutlineColor, pt(17f, 38f), Size(26f * u, 22f * u), style = Stroke(width = line))
        drawOval(CreamColor, pt(23f, 43f), Size(14f * u, 15f * u))

        // ── کاموا دور گربه: چند دور نخ دور بدن ──
        for (k in 0..2) {
            val y = 44f + k * 5f
            val wrap = Path().apply {
                moveTo((17.4f + k * 0.9f) * u, y * u)
                quadraticBezierTo(30f * u, (y + 5f) * u, (42.6f - k * 0.9f) * u, y * u)
            }
            drawPath(wrap, lerp(p.yarn, Color.Black, 0.22f), style = Stroke(width = 2.6f * u, cap = StrokeCap.Round))
            drawPath(wrap, p.yarn, style = Stroke(width = 1.8f * u, cap = StrokeCap.Round))
        }

        // ── گلوله‌ی کاموای کنار پا + نخی که به بدن وصله (فقط وقتی کاموا پیش گربه‌ست) ──
        if (!ballFree) {
            val ballC = pt(46f + p.wiggle * 1.2f, 55f)
            val link = Path().apply {
                moveTo(40.5f * u, 49f * u)
                quadraticBezierTo(47f * u, 47f * u, ballC.x, ballC.y - 4.5f * u)
            }
            drawPath(link, p.yarn, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
            drawYarnBall(ballC, 5.2f * u, p.yarn, p.wiggle * 70f)
        }

        // ── پنجه‌ها ──
        val dangle = p.scare * 3f
        drawOval(CreamColor, pt(19f, 52.5f + dangle), Size(9f * u, 6.5f * u))
        drawOval(OutlineColor, pt(19f, 52.5f + dangle), Size(9f * u, 6.5f * u), style = Stroke(width = 1.1f * u))
        val px = 32f + p.paw * 4f
        val py = 52.5f - p.paw * 9f + dangle * (1f - p.paw)
        withTransform({ rotate(-p.paw * 25f, pt(px + 4.5f, py + 3f)) }) {
            drawOval(CreamColor, pt(px, py), Size(9f * u, 6.5f * u))
            drawOval(OutlineColor, pt(px, py), Size(9f * u, 6.5f * u), style = Stroke(width = 1.1f * u))
        }

        // ── گوش‌ها ──
        val earL = p.ear * 10f - p.scare * 14f
        val earR = p.scare * 14f
        withTransform({ rotate(earL, pt(19f, 20f)) }) {
            val ear = Path().apply {
                moveTo(14f * u, 25f * u); lineTo(14.5f * u, 6.5f * u); lineTo(27.5f * u, 15f * u); close()
            }
            drawPath(ear, FurColor)
            drawPath(ear, OutlineColor, style = Stroke(width = line, join = StrokeJoin.Round))
            val inner = Path().apply {
                moveTo(17.2f * u, 21.5f * u); lineTo(17.4f * u, 11.5f * u); lineTo(24f * u, 16f * u); close()
            }
            drawPath(inner, PinkColor)
        }
        withTransform({ rotate(earR, pt(41f, 20f)) }) {
            val ear = Path().apply {
                moveTo(46f * u, 25f * u); lineTo(45.5f * u, 6.5f * u); lineTo(32.5f * u, 15f * u); close()
            }
            drawPath(ear, FurColor)
            drawPath(ear, OutlineColor, style = Stroke(width = line, join = StrokeJoin.Round))
            val inner = Path().apply {
                moveTo(42.8f * u, 21.5f * u); lineTo(42.6f * u, 11.5f * u); lineTo(36f * u, 16f * u); close()
            }
            drawPath(inner, PinkColor)
        }

        // ── سر ──
        drawOval(FurColor, pt(13f, 13f), Size(34f * u, 30f * u))
        drawOval(OutlineColor, pt(13f, 13f), Size(34f * u, 30f * u), style = Stroke(width = line))

        // خط‌های پیشونی
        val stripe = 1.6f * u
        drawLine(FurDark, pt(30f, 14f), pt(30f, 18.5f), strokeWidth = stripe, cap = StrokeCap.Round)
        drawLine(FurDark, pt(25.5f, 14.8f), pt(26.6f, 18.6f), strokeWidth = stripe, cap = StrokeCap.Round)
        drawLine(FurDark, pt(34.5f, 14.8f), pt(33.4f, 18.6f), strokeWidth = stripe, cap = StrokeCap.Round)

        // ── چشم‌های درشت و بامزه ──
        val eyeR = 6.3f + p.scare * 0.8f
        val open = (1f - p.blink).coerceAtLeast(0.08f)
        val eyeAlpha = 1f - p.happy
        // هرچی گیج‌تر، مارپیچ پررنگ‌تر و برق چشم کم‌رنگ‌تر
        val sp = ((p.dizzy - 0.25f) / 0.2f).coerceIn(0f, 1f)
        val hl = eyeAlpha * (1f - sp)
        listOf(22.3f, 37.7f).forEach { cx ->
            val cy = 29.5f
            if (eyeAlpha > 0.02f) {
                drawOval(
                    color = EyeColor.copy(alpha = eyeAlpha),
                    topLeft = pt(cx - eyeR, cy - eyeR * open),
                    size = Size(eyeR * 2f * u, eyeR * 2f * open * u)
                )
                if (open > 0.45f && hl > 0.02f) {
                    drawCircle(
                        Color(0xFF6B4A3A).copy(alpha = 0.55f * hl), 3.4f * u,
                        pt(cx + p.lookX * 1.4f, cy + 1.6f + p.lookY * 1.4f)
                    )
                    drawCircle(
                        Color.White.copy(alpha = hl), 2.4f * u,
                        pt(cx - 1.9f + p.lookX * 1.8f, cy - 2.3f + p.lookY * 1.8f)
                    )
                    drawCircle(
                        Color.White.copy(alpha = 0.85f * hl), 1.1f * u,
                        pt(cx + 2.3f + p.lookX * 1.2f, cy + 2.4f + p.lookY * 1.2f)
                    )
                }
                if (sp > 0.02f) {
                    val spiral = Path()
                    val n = 28
                    val base = Math.toRadians(p.spin.toDouble()).toFloat()
                    for (i in 0..n) {
                        val t = i / n.toFloat()
                        val ang = base + t * 2.4f * 6.2832f
                        val r = (eyeR - 1.2f) * t
                        val x = (cx + r * cos(ang)) * u
                        val y = (cy + r * sin(ang)) * u
                        if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
                    }
                    drawPath(
                        spiral, Color.White.copy(alpha = 0.9f * sp * eyeAlpha),
                        style = Stroke(width = 1.15f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
            if (p.happy > 0.02f) {
                val smile = Path().apply {
                    moveTo((cx - 4.6f) * u, (cy + 1.6f) * u)
                    quadraticBezierTo(cx * u, (cy - 4.6f) * u, (cx + 4.6f) * u, (cy + 1.6f) * u)
                }
                drawPath(
                    smile, EyeColor.copy(alpha = p.happy),
                    style = Stroke(width = 1.9f * u, cap = StrokeCap.Round)
                )
            }
        }

        // گونه‌ها
        drawCircle(PinkColor.copy(alpha = 0.5f), 3.2f * u, pt(15.5f, 35.5f))
        drawCircle(PinkColor.copy(alpha = 0.5f), 3.2f * u, pt(44.5f, 35.5f))

        // دماغ
        val nose = Path().apply {
            moveTo(28.3f * u, 33.6f * u); lineTo(31.7f * u, 33.6f * u); lineTo(30f * u, 35.4f * u); close()
        }
        drawPath(nose, PinkColor)
        drawPath(nose, OutlineColor, style = Stroke(width = 0.8f * u, join = StrokeJoin.Round))

        // دهن: «ω» و موقع حرف زدن باز می‌شه
        val mouth = Path().apply {
            moveTo(30f * u, 35.4f * u); lineTo(30f * u, 36.8f * u)
            moveTo(30f * u, 36.8f * u); quadraticBezierTo(28.6f * u, 38.6f * u, 26.8f * u, 37.4f * u)
            moveTo(30f * u, 36.8f * u); quadraticBezierTo(31.4f * u, 38.6f * u, 33.2f * u, 37.4f * u)
        }
        drawPath(mouth, OutlineColor, style = Stroke(width = 1.1f * u, cap = StrokeCap.Round))
        if (p.mouth > 0.05f) {
            val ry = 0.8f + p.mouth * 2.6f
            drawOval(Color(0xFF8B2E3C), pt(27.8f, 37.6f), Size(4.4f * u, ry * 2f * u))
            drawCircle(PinkColor, 1.2f * p.mouth * u, pt(30f, 37.6f + ry * 1.6f))
        }

        // سبیل‌ها
        val wh = CreamColor.copy(alpha = 0.9f)
        val ws = 0.9f * u
        drawLine(wh, pt(14f, 34f), pt(4f, 31.8f), strokeWidth = ws, cap = StrokeCap.Round)
        drawLine(wh, pt(14f, 36.2f), pt(4f, 36.6f), strokeWidth = ws, cap = StrokeCap.Round)
        drawLine(wh, pt(14.5f, 38.2f), pt(5.5f, 41f), strokeWidth = ws, cap = StrokeCap.Round)
        drawLine(wh, pt(46f, 34f), pt(56f, 31.8f), strokeWidth = ws, cap = StrokeCap.Round)
        drawLine(wh, pt(46f, 36.2f), pt(56f, 36.6f), strokeWidth = ws, cap = StrokeCap.Round)
        drawLine(wh, pt(45.5f, 38.2f), pt(54.5f, 41f), strokeWidth = ws, cap = StrokeCap.Round)

        // ── ستاره‌های گیجی که دور سر می‌چرخن ──
        if (p.dizzy > 0.05f) {
            val base = Math.toRadians(p.spin.toDouble()).toFloat()
            for (k in 0..2) {
                val ang = base + k * 2.0944f
                val sx = 30f + 16f * cos(ang)
                val sy = 6f + 4.2f * sin(ang)
                val depth = 0.5f + 0.5f * sin(ang)   // جلوی سر درشت‌تر
                val s = 1.8f + 1.6f * depth
                val star = Path().apply {
                    moveTo(sx * u, (sy - s) * u)
                    quadraticBezierTo(sx * u, sy * u, (sx + s) * u, sy * u)
                    quadraticBezierTo(sx * u, sy * u, sx * u, (sy + s) * u)
                    quadraticBezierTo(sx * u, sy * u, (sx - s) * u, sy * u)
                    quadraticBezierTo(sx * u, sy * u, sx * u, (sy - s) * u)
                    close()
                }
                drawPath(star, Color(0xFFFFD54F).copy(alpha = (0.55f + 0.45f * depth) * p.dizzy.coerceIn(0f, 1f)))
            }
        }
    }

    // ── کاموای آزاد (وقتی افتاده) ──
    // این قسمت بیرون از برعکس‌شدن گربه کشیده می‌شه تا جاش روی صفحه درست بمونه
    if (ballFree) {
        val bx = p.ballX
        val by = p.ballY
        val sx = 30f + 10.5f * p.face
        val sy = 49f
        val dist = hypot(bx - sx, by - sy)
        // هرچی گربه دورتر بشه نخ کشیده‌تر و کم‌رنگ‌تر می‌شه تا پاره بشه
        val a = (1f - (dist - 60f) / 60f).coerceIn(0f, 1f)
        if (a > 0.02f) {
            val sag = 3f + 12f * (1f - (dist / 110f).coerceIn(0f, 1f))
            val thread = Path().apply {
                moveTo(sx * u, sy * u)
                quadraticBezierTo((sx + bx) / 2f * u, ((sy + by) / 2f + sag) * u, bx * u, (by - 4.5f) * u)
            }
            drawPath(thread, p.yarn.copy(alpha = a), style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
        }
        drawYarnBall(pt(bx, by), 5.2f * u, p.yarn, p.ballSpin)
    }
}
