package com.appdony.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
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
import kotlin.math.max
import kotlin.math.sin

// ───────────────────────── رسم آدمک اپدونی ─────────────────────────
// اسم‌ها (CatPose / drawCat) عمداً همون قبلی موندن تا بقیه‌ی فایل‌ها دست نخورن؛
// ولی چیزی که کشیده می‌شه یه پسر بچه‌ی گیمره با هدفون و هودی به رنگ تم برنامه.

/** حالت لحظه‌ای آدمک؛ همه‌ی مقدارها بین ۰ و ۱ (یا ‎-۱ تا ۱) هستن */
internal class CatPose(
    val face: Float,     // ۱ = رو به راست، ‎-۱ = برعکس
    val blink: Float,    // پلک
    val ear: Float,      // تکون خوردن کاسه‌های هدفون
    val tail: Float,     // تاب خوردن بدن، بازوها و قدم برداشتن (‎-۱ تا ۱)
    val mouth: Float,    // باز بودن دهن
    val paw: Float,      // بالا آوردن دست راست
    val lookX: Float,    // نگاه چپ/راست
    val lookY: Float,    // نگاه بالا/پایین
    val scare: Float,    // بغل گرفته شدن: چشم درشت‌تر، ابروی بالا، دست‌ها بالا و عرق سرد
    val happy: Float,    // چشم‌های خندون ^ ^
    val wiggle: Float,   // قل خوردن توپ کنار پا
    val yarn: Color = AppdonyGreen, // رنگ تم برنامه؛ هودی و هدفون از همین ساخته می‌شه
    val fur: Color = catFurOf(yarn), // قدیمی؛ دیگه استفاده نمی‌شه
    val dizzy: Float = 0f,      // گیجی: چشم‌های مارپیچ و ستاره دور سر
    val spin: Float = 0f,       // زاویه‌ی چرخش مارپیچ و ستاره‌ها (درجه)
    val ballOff: Float = 0f,    // ۱ = توپ از پای آدمک جدا شده
    val ballX: Float = 0f,      // جای توپ آزاد نسبت به آدمک (dp)
    val ballY: Float = 0f,
    val ballSpin: Float = 0f,
    val wave: Float = 0f,       // سلام دادن: ۰ تا ۱ (دست بالا میره)
    val waveT: Float = 0f,      // تکون دادن دست موقع سلام (‎-۱ تا ۱)
    val tilt: Float = 0f,       // کج شدن سر (‎-۱ تا ۱)
    val breath: Float = 0f      // نفس کشیدن ۰ تا ۱
)

internal fun catFurOf(theme: Color): Color = lerp(theme, Color.White, 0.30f)

/** دست: خط ضخیم به رنگ هودی با خط دور + کف دست */
private fun DrawScope.drawArm(
    shoulder: Offset,
    hand: Offset,
    width: Float,
    hood: Color,
    ink: Color,
    skin: Color,
    u: Float
) {
    drawLine(ink, shoulder, hand, strokeWidth = width + 1.5f * u, cap = StrokeCap.Round)
    drawLine(hood, shoulder, hand, strokeWidth = width, cap = StrokeCap.Round)
    drawCircle(skin, 2.5f * u, hand)
    drawCircle(ink, 2.5f * u, hand, style = Stroke(width = 1.0f * u))
}

internal fun DrawScope.drawCat(p: CatPose) {
    val u = 1.dp.toPx()
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    val line = 1.3f * u

    // رنگ‌ها: هودی و هدفون از رنگ تم، بقیه ثابت
    val hood = p.yarn
    val hoodDark = lerp(p.yarn, Color.Black, 0.32f)
    val hoodLight = lerp(p.yarn, Color.White, 0.40f)
    val skin = Color(0xFFFFD6B8)
    val skinShade = Color(0xFFF3B28F)
    val hair = Color(0xFF3B2D44)
    val hairLight = Color(0xFF6A5576)
    val pants = Color(0xFF34406A)
    val ink = Color(0xFF3A2430)
    val ballColor = lerp(p.yarn, Color(0xFFFFC857), 0.72f)

    val ballFree = p.ballOff > 0.5f
    val breath = p.breath.coerceIn(0f, 1f)

    withTransform({ scale(p.face, 1f, pt(30f, 30f)) }) {
        // کل بدن روی پاشنه‌ی پا کمی تاب می‌خوره
        withTransform({ rotate(p.tail * 2.2f - p.dizzy * 4f, pt(30f, 59f)) }) {

            // ── سایه‌ی زیر پا ──
            drawOval(Color.Black.copy(alpha = 0.10f), pt(15f, 56.6f), Size(30f * u, 3.4f * u))

            // ── پاها و کفش‌ها (با تاب خوردن، قدم برمی‌دارن) ──
            val stepL = max(0f, p.tail) * 1.6f
            val stepR = max(0f, -p.tail) * 1.6f
            drawRoundRect(pants, pt(23.4f, 50f), Size(5.4f * u, 7f * u), CornerRadius(2.2f * u))
            drawRoundRect(pants, pt(31.2f, 50f), Size(5.4f * u, 7f * u), CornerRadius(2.2f * u))
            drawOval(Color.White, pt(21.4f, 56.2f - stepL), Size(8.4f * u, 3.4f * u))
            drawOval(ink, pt(21.4f, 56.2f - stepL), Size(8.4f * u, 3.4f * u), style = Stroke(width = 1.0f * u))
            drawOval(Color.White, pt(30.2f, 56.2f - stepR), Size(8.4f * u, 3.4f * u))
            drawOval(ink, pt(30.2f, 56.2f - stepR), Size(8.4f * u, 3.4f * u), style = Stroke(width = 1.0f * u))

            // ── بدن: هودی با نشان دانلود روی سینه (نفس می‌کشه) ──
            withTransform({ scale(1f + 0.012f * breath, 1f + 0.03f * breath, pt(30f, 52.5f)) }) {
                drawRoundRect(hood, pt(18.5f, 35.5f), Size(23f * u, 17.5f * u), CornerRadius(8f * u))
                drawRoundRect(ink, pt(18.5f, 35.5f), Size(23f * u, 17.5f * u), CornerRadius(8f * u), style = Stroke(width = line))
                // جیب هودی
                drawRoundRect(hoodDark.copy(alpha = 0.45f), pt(23.2f, 47f), Size(13.6f * u, 4.6f * u), CornerRadius(2.3f * u))
                // نشان دانلود
                drawCircle(Color.White, 3.7f * u, pt(30f, 42f))
                drawCircle(ink, 3.7f * u, pt(30f, 42f), style = Stroke(width = 0.9f * u))
                val arrow = Path().apply {
                    moveTo(30f * u, 40.1f * u); lineTo(30f * u, 43.2f * u)
                    moveTo(28.4f * u, 41.8f * u); lineTo(30f * u, 43.4f * u); lineTo(31.6f * u, 41.8f * u)
                }
                drawPath(arrow, hood, style = Stroke(width = 1.1f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            // ── دست‌ها ──
            // دست چپ: آویزون، با تاب خوردن؛ موقع ترس و شادی بالا میاد
            val angL = Math.toRadians((10f + p.tail * 12f + p.scare * 60f + p.happy * 10f).toDouble())
            val shL = pt(20f, 40f)
            val handL = Offset(shL.x - 10.5f * u * sin(angL).toFloat(), shL.y + 10.5f * u * cos(angL).toFloat())
            drawArm(shL, handL, 4.2f * u, hood, ink, skin, u)

            // دست راست: سلام دادن، بالا آوردن موقع بازی و ترس
            val raise = max(p.paw, max(p.wave * 0.9f, p.scare * 0.55f)).coerceIn(0f, 1f)
            val angRdeg = 10f + raise * 130f + p.waveT * 14f * p.wave - p.tail * 10f * (1f - raise)
            val angR = Math.toRadians(angRdeg.toDouble())
            val shR = pt(40f, 40f)
            val handR = Offset(shR.x + 10.5f * u * sin(angR).toFloat(), shR.y + 10.5f * u * cos(angR).toFloat())
            drawArm(shR, handR, 4.2f * u, hood, ink, skin, u)

            // ── توپ کنار پا (وقتی پیش آدمکه) ──
            if (!ballFree) {
                drawYarnBall(pt(47.5f + p.wiggle * 1.2f, 54.5f), 5.2f * u, ballColor, p.wiggle * 70f)
            }

            // ── سر (با نفس کشیدن بالا و پایین میره و کج می‌شه) ──
            withTransform({
                translate(0f, -breath * 0.7f * u)
                rotate(p.tilt * 5f - p.tail * 1.2f, pt(30f, 37f))
            }) {
                // صورت
                drawOval(skin, pt(13.5f, 8f), Size(33f * u, 30f * u))
                drawOval(ink, pt(13.5f, 8f), Size(33f * u, 30f * u), style = Stroke(width = line))

                // مو
                val hairPath = Path().apply {
                    moveTo(13.4f * u, 22.5f * u)
                    cubicTo(11.8f * u, 9.5f * u, 20.5f * u, 4.6f * u, 30f * u, 4.6f * u)
                    cubicTo(39.5f * u, 4.6f * u, 48.2f * u, 9.5f * u, 46.6f * u, 22.5f * u)
                    cubicTo(45.6f * u, 18.5f * u, 43.2f * u, 15.2f * u, 39.5f * u, 13.8f * u)
                    cubicTo(36f * u, 17.2f * u, 28.5f * u, 17.4f * u, 24.2f * u, 13.4f * u)
                    cubicTo(21.4f * u, 15.8f * u, 17.6f * u, 17.6f * u, 14.6f * u, 18.2f * u)
                    close()
                }
                drawPath(hairPath, hair)
                drawPath(hairPath, ink, style = Stroke(width = line, join = StrokeJoin.Round))
                val shine = Path().apply {
                    moveTo(20.5f * u, 9.4f * u)
                    quadraticBezierTo(26f * u, 6.4f * u, 32.5f * u, 6.6f * u)
                }
                drawPath(shine, hairLight.copy(alpha = 0.75f), style = Stroke(width = 1.3f * u, cap = StrokeCap.Round))

                // هدفون: کمربند روی سر
                val band = Path().apply {
                    moveTo(12.6f * u, 20.5f * u)
                    cubicTo(10.5f * u, -2f * u, 49.5f * u, -2f * u, 47.4f * u, 20.5f * u)
                }
                drawPath(band, ink, style = Stroke(width = 3.6f * u, cap = StrokeCap.Round))
                drawPath(band, hood, style = Stroke(width = 2.4f * u, cap = StrokeCap.Round))

                // هدفون: کاسه‌ها (با ear کمی از سر فاصله می‌گیرن و برمی‌گردن)
                val cupShift = p.ear * 1.1f
                val cupL = Offset((10f - cupShift) * u, 19f * u)
                val cupR = Offset((45f + cupShift) * u, 19f * u)
                for (cup in listOf(cupL, cupR)) {
                    drawRoundRect(hood, cup, Size(5f * u, 10f * u), CornerRadius(2.4f * u))
                    drawRoundRect(ink, cup, Size(5f * u, 10f * u), CornerRadius(2.4f * u), style = Stroke(width = line))
                    drawRoundRect(
                        hoodLight.copy(alpha = 0.85f),
                        Offset(cup.x + 0.9f * u, cup.y + 1.6f * u),
                        Size(3.2f * u, 6.8f * u),
                        CornerRadius(1.6f * u)
                    )
                }

                // ── چشم‌ها ──
                val eyeR = 3.5f + p.scare * 0.7f
                val open = (1f - p.blink).coerceAtLeast(0.1f)
                val eyeAlpha = 1f - p.happy
                // هرچی گیج‌تر، مارپیچ پررنگ‌تر و برق چشم کم‌رنگ‌تر
                val sp = ((p.dizzy - 0.25f) / 0.2f).coerceIn(0f, 1f)
                val hl = eyeAlpha * (1f - sp)
                listOf(23.2f, 36.8f).forEach { cx ->
                    val cy = 24.6f
                    if (eyeAlpha > 0.02f) {
                        val h = eyeR * 1.12f
                        drawOval(
                            color = EyeColor.copy(alpha = eyeAlpha),
                            topLeft = pt(cx - eyeR, cy - h * open),
                            size = Size(eyeR * 2f * u, h * 2f * open * u)
                        )
                        if (open > 0.45f && hl > 0.02f) {
                            drawCircle(
                                Color(0xFF6B4A3A).copy(alpha = 0.55f * hl), 2.1f * u,
                                pt(cx + p.lookX * 1.0f, cy + 0.9f + p.lookY * 1.0f)
                            )
                            drawCircle(
                                Color.White.copy(alpha = hl), 1.35f * u,
                                pt(cx - 1.1f + p.lookX * 1.0f, cy - 1.4f + p.lookY * 1.0f)
                            )
                            drawCircle(
                                Color.White.copy(alpha = 0.85f * hl), 0.65f * u,
                                pt(cx + 1.3f + p.lookX * 0.8f, cy + 1.5f + p.lookY * 0.8f)
                            )
                        }
                        if (sp > 0.02f) {
                            val spiral = Path()
                            val n = 24
                            val base = Math.toRadians(p.spin.toDouble()).toFloat()
                            for (i in 0..n) {
                                val t = i / n.toFloat()
                                val ang = base + t * 2.2f * 6.2832f
                                val r = (eyeR - 0.8f) * t
                                val x = (cx + r * cos(ang)) * u
                                val y = (cy + r * sin(ang)) * u
                                if (i == 0) spiral.moveTo(x, y) else spiral.lineTo(x, y)
                            }
                            drawPath(
                                spiral, Color.White.copy(alpha = 0.9f * sp * eyeAlpha),
                                style = Stroke(width = 0.9f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                    if (p.happy > 0.02f) {
                        val smile = Path().apply {
                            moveTo((cx - 2.9f) * u, (cy + 1.0f) * u)
                            quadraticBezierTo(cx * u, (cy - 3.0f) * u, (cx + 2.9f) * u, (cy + 1.0f) * u)
                        }
                        drawPath(
                            smile, EyeColor.copy(alpha = p.happy),
                            style = Stroke(width = 1.5f * u, cap = StrokeCap.Round)
                        )
                    }

                    // ابرو: موقع ترس و شادی میره بالا
                    val by = 19.9f - p.scare * 1.6f - p.happy * 0.7f - p.ear * 0.4f
                    val brow = Path().apply {
                        moveTo((cx - 3.0f) * u, (by + 0.7f) * u)
                        quadraticBezierTo(cx * u, (by - 0.8f) * u, (cx + 3.0f) * u, (by + 0.7f) * u)
                    }
                    drawPath(brow, hair, style = Stroke(width = 1.15f * u, cap = StrokeCap.Round))
                }

                // گونه‌ها و بینی
                drawCircle(PinkColor.copy(alpha = 0.5f), 2.5f * u, pt(18.2f, 29.2f))
                drawCircle(PinkColor.copy(alpha = 0.5f), 2.5f * u, pt(41.8f, 29.2f))
                drawCircle(skinShade, 0.8f * u, pt(30f, 27.4f))

                // دهن: لبخند؛ موقع حرف زدن باز می‌شه
                if (p.mouth <= 0.05f) {
                    val smile = Path().apply {
                        moveTo(26.9f * u, 30.2f * u)
                        quadraticBezierTo(30f * u, 33f * u, 33.1f * u, 30.2f * u)
                    }
                    drawPath(smile, ink, style = Stroke(width = 1.2f * u, cap = StrokeCap.Round))
                } else {
                    val h = 1.0f + p.mouth * 3.6f
                    drawOval(Color(0xFF7A2335), pt(27.4f, 30f), Size(5.2f * u, h * u))
                    if (p.mouth > 0.35f) {
                        drawOval(PinkColor, pt(28.6f, 30f + h - 1.5f), Size(2.8f * u, 1.5f * u))
                    }
                }
            }

            // ── عرق سرد موقع ترس ──
            if (p.scare > 0.05f) {
                val drop = Path().apply {
                    moveTo(47.5f * u, 9.5f * u)
                    quadraticBezierTo(50.2f * u, 13.5f * u, 47.5f * u, 15.5f * u)
                    quadraticBezierTo(44.8f * u, 13.5f * u, 47.5f * u, 9.5f * u)
                    close()
                }
                drawPath(drop, Color(0xFF7CC8FF).copy(alpha = p.scare.coerceIn(0f, 1f)))
            }

            // ── ستاره‌های گیجی که دور سر می‌چرخن ──
            if (p.dizzy > 0.05f) {
                val base = Math.toRadians(p.spin.toDouble()).toFloat()
                for (k in 0..2) {
                    val ang = base + k * 2.0944f
                    val sx = 30f + 17f * cos(ang)
                    val sy = 5f + 4.5f * sin(ang)
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
    }

    // ── توپ آزاد (وقتی افتاده) ──
    // این قسمت بیرون از برعکس‌شدن آدمک کشیده می‌شه تا جاش روی صفحه درست بمونه
    if (ballFree) {
        drawYarnBall(pt(p.ballX, p.ballY), 5.2f * u, ballColor, p.ballSpin)
    }
}
