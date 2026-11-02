package com.appdony.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.appdony.app.ui.theme.AppdonyGreen

// ───────────────────────── آیکن هوش مصنوعی اپدونی ─────────────────────────
// ربات سیاه‌وسفید با هدفون، دقیقاً مثل عکس مرجع و بدون هیچ رنگ تم.
// اسم‌های CatPose و drawCat عمداً همون قبلی موندن تا FloatingAssistant، CatOverlayService
// و SupportFloating دست نخورن؛ ولی چیزی که کشیده می‌شه فقط آیکن ربات هوش مصنوعیه.

/** حالت لحظه‌ای آیکن؛ همه‌ی مقدارها بین ۰ و ۱ (یا ‎-۱ تا ۱) هستن */
internal class CatPose(
    val face: Float,     // ۱ = رو به راست، ‎-۱ = برعکس
    val blink: Float,    // پلک
    val ear: Float,      // تکون خوردن گوشی‌ها
    val tail: Float,     // قدیمی؛ استفاده نمی‌شه
    val mouth: Float,    // تپش چشم‌ها موقع حرف زدن
    val paw: Float,      // قدیمی؛ استفاده نمی‌شه
    val lookX: Float,    // نگاه چپ/راست
    val lookY: Float,    // نگاه بالا/پایین
    val scare: Float,    // ترسیدن: چشم‌های درشت‌تر
    val happy: Float,    // چشم‌های خندون ^ ^
    val wiggle: Float,   // قدیمی؛ استفاده نمی‌شه
    val yarn: Color = AppdonyGreen, // قدیمی؛ آیکن به رنگ تم وابسته نیست
    val fur: Color = catFurOf(yarn), // قدیمی؛ دیگه استفاده نمی‌شه
    val dizzy: Float = 0f,      // گیجی: چشم‌های مارپیچ
    val spin: Float = 0f,       // زاویه‌ی چرخش مارپیچ (درجه)
    val ballOff: Float = 0f,    // ۱ = حباب از کنار آیکن جدا شده
    val ballX: Float = 0f,      // جای حباب آزاد نسبت به آیکن (dp)
    val ballY: Float = 0f,
    val ballSpin: Float = 0f,
    val wave: Float = 0f,       // قدیمی؛ استفاده نمی‌شه
    val waveT: Float = 0f,      // قدیمی؛ استفاده نمی‌شه
    val tilt: Float = 0f,       // کج شدن سر (‎-۱ تا ۱)
    val breath: Float = 0f      // نفس کشیدن ۰ تا ۱
)

internal fun catFurOf(theme: Color): Color = lerp(theme, Color.White, 0.30f)

private val RobotPlate = Color(0xFF0B0B0C)
private val RobotInk = Color(0xFFF4F4F4)

/** آیکن ربات روی بوم ۶۰×۶۰ (واحد dp)؛ کادر مشکی، سر سفید گرد، گوشی‌های دو طرف و دو چشم عمودی */
internal fun DrawScope.drawCat(p: CatPose) {
    val u = 1.dp.toPx()
    val c = Offset(30f * u, 30f * u)

    drawRoundRect(RobotPlate, Offset.Zero, Size(60f * u, 60f * u), CornerRadius(24f * u))

    val sway = Math.sin(Math.toRadians(p.spin.toDouble())).toFloat()
    val grow = 1f + 0.025f * (p.breath - 0.5f) + 0.04f * p.scare
    withTransform({
        rotate(p.tilt * 6f + p.dizzy * sway * 8f, c)
        scale(grow, grow, c)
    }) {
        val half = 14.2f * u
        val ring = 2.3f * u

        // گوشی‌ها (موقع تکون خوردن کمی بیرون می‌زنن)
        val cupW = 5.5f * u
        val cupH = 12.4f * u
        val cupY = c.y + 1.2f * u - cupH / 2f
        val push = p.ear * 0.8f * u
        drawRoundRect(RobotInk, Offset(c.x - half - 2.9f * u - push, cupY), Size(cupW, cupH), CornerRadius(2.8f * u))
        drawRoundRect(RobotInk, Offset(c.x + half + 2.9f * u - cupW + push, cupY), Size(cupW, cupH), CornerRadius(2.8f * u))

        // سر: حلقه‌ی سفید دور صفحه‌ی مشکی
        drawRoundRect(RobotInk, Offset(c.x - half, c.y - half), Size(half * 2f, half * 2f), CornerRadius(11.5f * u))
        val inner = half - ring
        drawRoundRect(RobotPlate, Offset(c.x - inner, c.y - inner), Size(inner * 2f, inner * 2f), CornerRadius(9.2f * u))

        // چشم‌ها
        val lx = p.lookX.coerceIn(-1f, 1f) * 1.8f * u
        val ey = c.y + p.lookY.coerceIn(-1f, 1f) * 1.4f * u
        val w = 4.7f * u * (1f + 0.15f * p.scare)
        val h = 10.2f * u * (1f + 0.25f * p.scare + 0.18f * p.mouth) * (1f - 0.9f * p.blink.coerceIn(0f, 1f))
        val happyA = ((p.happy - 0.3f) / 0.4f).coerceIn(0f, 1f)
        val dizzyA = p.dizzy.coerceIn(0f, 1f)
        val pillA = (1f - happyA) * (1f - dizzyA)
        for (side in intArrayOf(-1, 1)) {
            val ex = c.x + side * 4.6f * u + lx
            if (pillA > 0.01f) {
                drawRoundRect(
                    RobotInk.copy(alpha = pillA),
                    Offset(ex - w / 2f, ey - h / 2f),
                    Size(w, h),
                    CornerRadius(w / 2f)
                )
            }
            if (happyA > 0.01f && dizzyA < 0.5f) {
                drawArc(
                    color = RobotInk.copy(alpha = happyA),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(ex - 3.4f * u, ey - 1.5f * u),
                    size = Size(6.8f * u, 6.8f * u),
                    style = Stroke(2.6f * u, cap = StrokeCap.Round)
                )
            }
            if (dizzyA > 0.01f) {
                drawArc(
                    color = RobotInk.copy(alpha = dizzyA),
                    startAngle = p.spin + side * 40f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = Offset(ex - 3.4f * u, ey - 3.4f * u),
                    size = Size(6.8f * u, 6.8f * u),
                    style = Stroke(2f * u, cap = StrokeCap.Round)
                )
            }
        }
    }

    // حباب آزاد (وقتی از کنار آیکن جدا شده)؛ بیرون از انیمیشن سر کشیده می‌شه تا جاش روی صفحه درست بمونه
    if (p.ballOff > 0.5f) {
        drawBubble(Offset(p.ballX * u, p.ballY * u), 5.2f * u, Color.White, p.ballSpin)
    }
}
