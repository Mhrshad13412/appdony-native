package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** مرحله‌ی کارت آپدیت؛ انیمیشن‌ها پشت‌سرهم و پیوسته بین این مرحله‌ها می‌رن */
enum class UpdateStage {
    Coding,   // آپدیت اومده: پشتیبان داره کد می‌زنه
    Confirm,  // کاربر «دانلود و نصب» رو زد: بلند می‌شه، رو به دوربین می‌چرخه و 👌 می‌ده
    Ready     // دانلود تموم شد: دستش رو می‌کنه و می‌گه «بفرما!»
}

/**
 * صحنه‌ی کارت شناور آپدیت: پشتیبان از زاویه‌ی گوشه‌ی پشت اتاق، پشت کامپیوتر، با نور سبز مانیتور روی صورتش.
 * همه‌چیز با کد کشیده شده؛ [progress] (۰ تا ۱، منفی = شروع نشده) روی صفحه‌ی مانیتور نوار دانلود می‌شه.
 */
@Composable
fun UpdateDevScene(stage: UpdateStage, progress: Float, modifier: Modifier = Modifier) {
    val stand by animateFloatAsState(
        if (stage == UpdateStage.Coding) 0f else 1f,
        tween(1700, easing = FastOutSlowInEasing), label = "stand"
    )
    val ok by animateFloatAsState(
        if (stage == UpdateStage.Confirm) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow), label = "ok"
    )
    val offer by animateFloatAsState(
        if (stage == UpdateStage.Ready) 1f else 0f,
        spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow), label = "offer"
    )
    val panel by animateFloatAsState(
        if (stage == UpdateStage.Coding) 0f else 1f,
        tween(600), label = "panel"
    )
    val inf = rememberInfiniteTransition(label = "dev_scene")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "t"
    )

    Canvas(modifier) {
        val k = size.width / 200f
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawDevScene(t, stand, ok, offer, panel, progress)
        }
    }
}

// ───────────────────────── رنگ‌ها و کمک‌ها ─────────────────────────

private val GLOW = Color(0xFF39FF88)
private val SKIN = Color(0xFFD9A683)
private val SKIN_D = Color(0xFFB98562)
private val HAIR = Color(0xFF17110F)
private val FRAME = Color(0xFF4A3A2C)
private val JACKET = Color(0xFF2C2522)
private val JACKET_L = Color(0xFF42362F)
private val PANTS = Color(0xFF1E2430)
private val TAU = (2.0 * PI).toFloat()

private fun sm(x: Float, a: Float, b: Float): Float {
    val v = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return v * v * (3f - 2f * v)
}

private fun lerpF(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun mix(a: Offset, b: Offset, f: Float) = Offset(lerpF(a.x, b.x, f), lerpF(a.y, b.y, f))

// ───────────────────────── صحنه (بوم ۲۰۰×۱۳۵) ─────────────────────────

private fun DrawScope.drawDevScene(t: Float, stand: Float, ok: Float, offer: Float, panel: Float, progress: Float) {
    val flick = 0.92f + 0.06f * sin(TAU * 3f * t) + 0.03f * sin(TAU * 8f * t)
    // 👌 فقط وقتی کامل بلند شده و رو به دوربینه
    val okE = ok * sm(stand, 0.72f, 1f)

    drawRoom(t)
    drawDesk(t, flick, panel, offer, progress)
    drawPerson(t, stand, okE, offer, flick)

    // نور سبز مانیتور روی همه‌چیز (جمع نوری، مثل نور واقعی)
    drawRect(
        brush = Brush.radialGradient(
            listOf(GLOW.copy(alpha = 0.26f * flick), Color.Transparent),
            center = Offset(137f, 48f), radius = 100f
        ),
        size = Size(200f, 135f),
        blendMode = BlendMode.Plus
    )
    // تیره شدن گوشه‌ها؛ حس فیلم و دوربین مداربسته‌ی گوشه‌ی اتاق
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
            center = Offset(100f, 62f), radius = 125f
        ),
        size = Size(200f, 135f)
    )
}

private fun DrawScope.drawRoom(t: Float) {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF15282C), Color(0xFF0A1315)), startY = 0f, endY = 104f),
        size = Size(200f, 135f)
    )
    // کف اتاق کمی کج، چون دوربین از گوشه‌ی پشت اتاقه
    val floor = Path().apply {
        moveTo(0f, 104f); lineTo(200f, 97f); lineTo(200f, 135f); lineTo(0f, 135f); close()
    }
    drawPath(floor, Brush.verticalGradient(listOf(Color(0xFF0E191C), Color(0xFF070D0F)), startY = 97f, endY = 135f))
    drawLine(Color(0xFF1E3A3F), Offset(0f, 104f), Offset(200f, 97f), strokeWidth = 1f)

    // پوستر </> روی دیوار
    drawRoundRect(Color(0xFF0C1A1D), Offset(14f, 16f), Size(34f, 26f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF2A4A4F), Offset(14f, 16f), Size(34f, 26f), CornerRadius(3f, 3f), style = Stroke(1f))
    val sg = Stroke(1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val pc = GLOW.copy(alpha = 0.55f)
    drawPath(Path().apply { moveTo(26f, 24f); lineTo(21f, 29f); lineTo(26f, 34f) }, pc, style = sg)
    drawPath(Path().apply { moveTo(36f, 24f); lineTo(41f, 29f); lineTo(36f, 34f) }, pc, style = sg)
    drawLine(pc, Offset(33f, 22f), Offset(29f, 36f), strokeWidth = 1.6f, cap = StrokeCap.Round)

    // ذره‌های سبز که آروم بالا می‌رن
    for (i in 0..6) {
        val p = (t + i * 0.143f) % 1f
        val x = 108f + (i * 29 % 80)
        val y = 96f - p * 80f
        drawRect(GLOW.copy(alpha = sin(PI.toFloat() * p) * 0.35f), Offset(x, y), Size(1.6f, 1.6f))
    }
}

private fun DrawScope.drawDesk(t: Float, flick: Float, panel: Float, offer: Float, progress: Float) {
    // پایه‌های میز
    drawRect(Color(0xFF0F1B1E), Offset(40f, 100f), Size(4f, 30f))
    drawRect(Color(0xFF0F1B1E), Offset(190f, 94f), Size(4f, 36f))
    // رو و لبه‌ی میز
    val top = Path().apply { moveTo(30f, 84f); lineTo(200f, 77f); lineTo(200f, 91f); lineTo(30f, 98f); close() }
    drawPath(top, Brush.horizontalGradient(listOf(Color(0xFF1A2A2E), Color(0xFF24403F)), startX = 30f, endX = 200f))
    val edge = Path().apply { moveTo(30f, 98f); lineTo(200f, 91f); lineTo(200f, 95f); lineTo(30f, 102f); close() }
    drawPath(edge, Color(0xFF0D1719))

    // پایه‌ی مانیتور
    drawRect(Color(0xFF16252A), Offset(135f, 70f), Size(5f, 12f))
    drawRoundRect(Color(0xFF16252A), Offset(125f, 80f), Size(25f, 3f), CornerRadius(1.5f, 1.5f))

    // قاب و صفحه‌ی مانیتور
    val bezel = Path().apply { moveTo(101f, 19f); lineTo(173f, 26f); lineTo(173f, 77f); lineTo(101f, 71f); close() }
    drawPath(bezel, Color(0xFF0B1416))
    drawPath(bezel, Color(0xFF2B4A4D), style = Stroke(1f))
    val screen = Path().apply { moveTo(104f, 22f); lineTo(170f, 28f); lineTo(170f, 74f); lineTo(104f, 68f); close() }

    clipPath(screen) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF062414), Color(0xFF031109)), startY = 22f, endY = 74f),
            Offset(100f, 18f), Size(75f, 60f)
        )
        // محتوای صفحه هم‌شیب با لبه‌ی کج مانیتور
        rotate(5.2f, Offset(104f, 22f)) {
            val codeA = 1f - 0.85f * panel
            if (codeA > 0.02f) {
                for (i in 0..9) {
                    // خط‌های کد آروم بالا می‌رن؛ ۴ خط در هر چرخه، پس حلقه بی‌درزه
                    val y = 24f + ((i * 6f - t * 24f) % 60f + 60f) % 60f
                    val edgeA = ((y - 24f) / 4f).coerceIn(0f, 1f) * ((72f - y) / 6f).coerceIn(0f, 1f)
                    val indent = (i % 3) * 4f
                    val len = 14f + ((i * 7) % 5) * 7f
                    val c = if (i % 4 == 0) Color(0xFF8CFFC0) else GLOW
                    drawLine(
                        c.copy(alpha = 0.75f * codeA * edgeA),
                        Offset(110f + indent, y), Offset(110f + indent + len, y),
                        strokeWidth = 2f, cap = StrokeCap.Round
                    )
                }
            }
            // مرحله‌ی دانلود: فلش و نوار پیشرفت
            val confirmA = panel * (1f - offer.coerceIn(0f, 1f))
            if (confirmA > 0.02f) {
                val bob = sin(TAU * 3f * t) * 1.5f
                val ac = GLOW.copy(alpha = confirmA)
                drawLine(ac, Offset(137f, 31f + bob), Offset(137f, 40f + bob), strokeWidth = 2.6f, cap = StrokeCap.Round)
                drawPath(
                    Path().apply { moveTo(132.5f, 36f + bob); lineTo(137f, 41f + bob); lineTo(141.5f, 36f + bob) },
                    ac, style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawRoundRect(GLOW.copy(alpha = 0.18f * confirmA), Offset(114f, 47f), Size(46f, 5f), CornerRadius(2.5f, 2.5f))
                if (progress >= 0f) {
                    drawRoundRect(ac, Offset(114f, 47f), Size((46f * progress.coerceIn(0f, 1f)).coerceAtLeast(3f), 5f), CornerRadius(2.5f, 2.5f))
                } else {
                    val x0 = 114f + 32f * (0.5f + 0.5f * sin(TAU * 2f * t))
                    drawRoundRect(ac, Offset(x0, 47f), Size(14f, 5f), CornerRadius(2.5f, 2.5f))
                }
                drawLine(GLOW.copy(alpha = 0.4f * confirmA), Offset(114f, 58f), Offset(146f, 58f), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(GLOW.copy(alpha = 0.25f * confirmA), Offset(114f, 63f), Offset(136f, 63f), strokeWidth = 2f, cap = StrokeCap.Round)
            }
            // تموم شد: تیک بزرگ
            if (offer > 0.02f) {
                val a = offer.coerceIn(0f, 1f)
                val pop = 0.6f + 0.4f * offer.coerceIn(0f, 1.2f)
                withTransform({ scale(pop, pop, Offset(138f, 49f)) }) {
                    drawCircle(GLOW.copy(alpha = 0.16f * a), 17f, Offset(138f, 49f))
                    drawPath(
                        Path().apply { moveTo(127f, 49f); lineTo(135f, 57f); lineTo(150f, 40f) },
                        GLOW.copy(alpha = a), style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
    // براق بودن شیشه
    drawPath(
        screen,
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.06f), Color.Transparent), start = Offset(104f, 22f), end = Offset(150f, 60f))
    )

    // کیبورد
    val kb = Path().apply { moveTo(86f, 87f); lineTo(134f, 83.5f); lineTo(136f, 87.5f); lineTo(88f, 91.5f); close() }
    drawPath(kb, Color(0xFF0B1416))
    for (r in 0..1) {
        for (c in 0..9) {
            val a = 0.15f + 0.25f * (0.5f + 0.5f * sin(TAU * (2f + (c % 4)) * t + c))
            drawRect(
                GLOW.copy(alpha = a * flick),
                Offset(90f + c * 4.3f + r * 1f, 88.6f - c * 0.35f + r * 2.1f), Size(2f, 1.1f)
            )
        }
    }

    // لیوان چای با بخار
    drawRoundRect(Color(0xFF22383C), Offset(172f, 72f), Size(8f, 10f), CornerRadius(2f, 2f))
    drawArc(Color(0xFF22383C), -90f, 180f, false, Offset(177.5f, 74f), Size(5f, 5.5f), style = Stroke(1.4f))
    for (s in 0..1) {
        val ph = (t * 2f + s * 0.5f) % 1f
        val sx = 174f + s * 4f
        drawLine(
            Color.White.copy(alpha = 0.22f * sin(PI.toFloat() * ph)),
            Offset(sx + sin(TAU * ph) * 1.5f, 70f - ph * 9f),
            Offset(sx + sin(TAU * ph + 1f) * 1.5f, 67f - ph * 9f),
            strokeWidth = 1.2f, cap = StrokeCap.Round
        )
    }
}

// ───────────────────────── پشتیبان ─────────────────────────

/**
 * پشت میز می‌شینه و کد می‌زنه (دید ۳/۴ از پشت). با [stand] صندلی عقب می‌ره، از جاش بلند می‌شه،
 * مثل ورق می‌چرخه و رو به دوربین می‌ایسته؛ بعدش [okE] اشاره‌ی 👌 و [offer] اشاره‌ی «بفرما».
 */
private fun DrawScope.drawPerson(t: Float, stand: Float, okE: Float, offer: Float, flick: Float) {
    val chair = sm(stand, 0f, 0.32f)
    val rise = sm(stand, 0.08f, 0.42f)
    val turnP = sm(stand, 0.34f, 0.80f)
    val walk = sm(stand, 0.52f, 1f)

    // صندلی عقب می‌ره و خالی می‌مونه
    withTransform({
        translate(-24f * chair, 3f * chair)
        rotate(-14f * chair, Offset(62f, 100f))
    }) { drawChair() }

    // چرخیدن مثل ورق: عرض کم می‌شه، سمت دیگه‌ی آدم (روبه‌رو) باز می‌شه
    val sx = abs(cos(PI.toFloat() * turnP)).coerceAtLeast(0.02f)
    if (turnP < 0.5f) {
        withTransform({
            translate(0f, -5f * rise)
            scale(sx, 1f + 0.03f * rise, Offset(62f, 100f))
        }) { drawSitting(t, rise, flick) }
    } else {
        val cx = 62f + 18f * walk
        withTransform({
            translate(cx, 126f)
            scale(sx, 1f, Offset.Zero)
        }) { drawStanding(t, okE, offer, flick) }
    }
}

private fun DrawScope.drawChair() {
    val back = Color(0xFF16262A)
    drawRoundRect(back, Offset(46f, 58f), Size(32f, 34f), CornerRadius(8f, 8f))
    drawLine(GLOW.copy(alpha = 0.22f), Offset(78f, 66f), Offset(78f, 86f), strokeWidth = 1.2f, cap = StrokeCap.Round)
    drawLine(Color(0xFF0E181B), Offset(62f, 92f), Offset(62f, 108f), strokeWidth = 3f)
    drawLine(Color(0xFF0E181B), Offset(49f, 110f), Offset(75f, 109f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    drawCircle(Color(0xFF0A1113), 2f, Offset(49f, 111f))
    drawCircle(Color(0xFF0A1113), 2f, Offset(75f, 110f))
    drawCircle(Color(0xFF0A1113), 2f, Offset(62f, 112f))
}

/** نشسته و پشت به ما: فقط گونه و عینکش که نور مانیتور بهش می‌خوره از لبه‌ی سر دیده می‌شه */
private fun DrawScope.drawSitting(t: Float, lift: Float, flick: Float) {
    val br = sin(TAU * t) * 0.5f
    val bob = sin(TAU * 2f * t) * 0.6f

    // دست‌ها پشت تنه‌ن و روی کیبورد تایپ می‌کنن؛ موقع بلند شدن بالا می‌رن
    val tapR = 0.9f * maxOf(0f, sin(TAU * 3f * t + 1f))
    val tapL = 0.9f * maxOf(0f, sin(TAU * 4f * t + 2f))
    val handR = Offset(106f - 10f * lift, 87.5f - tapR - 22f * lift)
    val handL = Offset(96f - 8f * lift, 90.5f - tapL - 22f * lift)
    val sleeve = JACKET_L
    val rimC = GLOW.copy(alpha = 0.35f * flick)
    drawLine(sleeve, Offset(74f, 78f), Offset(85f, 96f), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(sleeve, Offset(85f, 96f), handR, strokeWidth = 6.5f, cap = StrokeCap.Round)
    drawLine(rimC, Offset(86f, 93.5f), Offset(handR.x, handR.y - 2.6f), strokeWidth = 0.9f, cap = StrokeCap.Round)
    drawLine(sleeve, Offset(50f, 80f), Offset(66f, 100f), strokeWidth = 8f, cap = StrokeCap.Round)
    drawLine(sleeve, Offset(66f, 100f), handL, strokeWidth = 6.5f, cap = StrokeCap.Round)

    // تنه (کاپشن تیره)
    val torso = Path().apply {
        moveTo(42f, 103f); cubicTo(41f, 86f, 46f, 72f, 62f, 68f + br * 0.3f)
        cubicTo(78f, 72f, 83f, 86f, 82f, 103f); close()
    }
    drawPath(torso, Brush.horizontalGradient(listOf(JACKET, JACKET_L), startX = 42f, endX = 82f))
    // لبه‌ی روشن سمت مانیتور
    drawPath(
        Path().apply { moveTo(70f, 69.5f); cubicTo(78f, 73f, 82f, 84f, 82f, 98f) },
        GLOW.copy(alpha = 0.55f * flick), style = Stroke(1.4f, cap = StrokeCap.Round)
    )
    // یقه‌ی بلند و گردن
    drawRoundRect(SKIN_D, Offset(57.5f, 56f + bob * 0.3f), Size(9f, 8f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF1A1513), Offset(54f, 61f + br * 0.3f), Size(16f, 10f), CornerRadius(5f, 5f))

    // دست‌ها روی کیبورد
    drawCircle(SKIN, 3.2f, handR)
    drawCircle(SKIN, 3.2f, handL)
    drawCircle(GLOW.copy(alpha = 0.3f * flick), 3.2f, handR)

    // سر: کمی کج به سمت مانیتور
    val hc = Offset(60f + bob * 0.2f, 49f + bob * 0.4f)
    // نیم‌رخ گونه و بینی که از لبه‌ی مو بیرونه
    drawOval(SKIN, Offset(hc.x + 8f, hc.y - 6f), Size(9f, 14f))
    drawPath(
        Path().apply { moveTo(hc.x + 16f, hc.y + 1f); lineTo(hc.x + 19.5f, hc.y + 3.6f); lineTo(hc.x + 16f, hc.y + 6f); close() },
        SKIN
    )
    drawOval(GLOW.copy(alpha = 0.28f * flick), Offset(hc.x + 11f, hc.y - 5f), Size(5.5f, 12f))
    // مو
    drawCircle(HAIR, 13f, hc)
    drawCircle(HAIR, 4.2f, Offset(hc.x - 6f, hc.y - 11f))
    drawCircle(HAIR, 4.6f, Offset(hc.x + 2f, hc.y - 13f))
    drawCircle(HAIR, 3.6f, Offset(hc.x + 9f, hc.y - 10.5f))
    // عینک گرد؛ شیشه‌ش نور کد رو منعکس می‌کنه
    val lens = Offset(hc.x + 14.5f, hc.y + 2f)
    drawCircle(GLOW.copy(alpha = 0.40f * flick), 3.8f, lens)
    drawCircle(FRAME, 3.8f, lens, style = Stroke(1f))
    drawLine(FRAME, Offset(lens.x - 3.4f, lens.y - 2.6f), Offset(hc.x + 5f, hc.y - 4f), strokeWidth = 0.9f, cap = StrokeCap.Round)
    // نور سبز لبه‌ی سر
    drawArc(
        GLOW.copy(alpha = 0.7f * flick), -52f, 100f, false,
        Offset(hc.x - 13f, hc.y - 13f), Size(26f, 26f),
        style = Stroke(1.3f, cap = StrokeCap.Round)
    )
}

/** ایستاده و رو به دوربین؛ مبدأ مختصات وسط پاهاست و بالا منفیه */
private fun DrawScope.drawStanding(t: Float, okE: Float, offer: Float, flick: Float) {
    val br = sin(TAU * t) * 0.5f
    val sway = sin(TAU * t) * 0.8f
    val blink = (t * 3f) % 1f > 0.94f
    val joy = maxOf(okE.coerceIn(0f, 1f), offer.coerceIn(0f, 1f))
    val okW = okE.coerceIn(0f, 1.15f)
    val offW = offer.coerceIn(0f, 1.1f)

    // سایه‌ی کف و پاها
    drawOval(Color.Black.copy(alpha = 0.35f), Offset(-17f, -3f), Size(34f, 7f))
    drawRoundRect(PANTS, Offset(-9f, -28f), Size(8f, 26f), CornerRadius(3f, 3f))
    drawRoundRect(PANTS, Offset(1f, -28f), Size(8f, 26f), CornerRadius(3f, 3f))
    drawRoundRect(Color(0xFF0C0F14), Offset(-10f, -4f), Size(10f, 5f), CornerRadius(2.5f, 2.5f))
    drawRoundRect(Color(0xFF0C0F14), Offset(0f, -4f), Size(10f, 5f), CornerRadius(2.5f, 2.5f))

    // هنگام «بفرما» کمی تعظیم می‌کنه (بالاتنه از کمر خم می‌شه)
    withTransform({ rotate(6f * offer.coerceIn(0f, 1f), Offset(0f, -26f)) }) {
        // تنه
        val body = Path().apply {
            moveTo(-15f, -48f); cubicTo(-15f, -55f, -9f, -57f, 0f, -57f + br * 0.3f)
            cubicTo(9f, -57f, 15f, -55f, 15f, -48f); lineTo(11f, -25f); lineTo(-11f, -25f); close()
        }
        drawPath(body, Brush.horizontalGradient(listOf(JACKET, JACKET_L), startX = -15f, endX = 15f))
        drawLine(GLOW.copy(alpha = 0.5f * flick), Offset(15f, -48f), Offset(11f, -25f), strokeWidth = 1.2f, cap = StrokeCap.Round)
        drawRoundRect(SKIN_D, Offset(-3.6f, -62f), Size(7.2f, 7f), CornerRadius(2.5f, 2.5f))
        drawRoundRect(Color(0xFF1A1513), Offset(-7.5f, -60.5f), Size(15f, 8.5f), CornerRadius(4f, 4f))
        drawLine(Color(0xFF4A413B), Offset(0f, -52f), Offset(0f, -25f), strokeWidth = 0.8f)

        // دست چپ (سمت چپ تصویر): آویزون؛ موقع «بفرما» روی سینه
        val lE = mix(Offset(-18f, -40f), Offset(-20f, -38f), offW)
        val lH = mix(Offset(-19f, -29f - sway), Offset(-8f, -41f), offW)
        drawLine(JACKET_L, Offset(-14f, -52f), lE, strokeWidth = 6.5f, cap = StrokeCap.Round)
        drawLine(JACKET_L, lE, lH, strokeWidth = 5.8f, cap = StrokeCap.Round)
        drawCircle(SKIN, 3.2f, lH)

        // دست راست: آویزون ← 👌 کنار صورت ← «بفرما» به سمت پایین/راست
        val rE = mix(mix(Offset(18f, -40f), Offset(22f, -43f), okW), Offset(25f, -45f), offW)
        val rH = mix(
            mix(Offset(19f, -29f + sway), Offset(25f, -62f + sin(TAU * 2f * t) * 0.8f), okW),
            Offset(40f + sin(TAU * t) * 2f, -36f + sin(TAU * 2f * t) * 1.2f), offW
        )
        drawLine(JACKET_L, Offset(14f, -52f), rE, strokeWidth = 6.5f, cap = StrokeCap.Round)
        drawLine(JACKET_L, rE, rH, strokeWidth = 5.8f, cap = StrokeCap.Round)
        when {
            offer > 0.45f -> drawHandOpen(rH)
            okE > 0.15f -> drawHandOk(rH, sin(TAU * 2f * t) * 6f * okE.coerceIn(0f, 1f))
            else -> drawCircle(SKIN, 3.2f, rH)
        }

        // سر
        val hc = Offset(0f, -76f)
        drawOval(SKIN_D, Offset(-13.8f, -78.5f), Size(3f, 5.6f))
        drawOval(SKIN_D, Offset(10.8f, -78.5f), Size(3f, 5.6f))
        drawCircle(SKIN, 11.5f, hc)
        // نور سبز مانیتور از سمت راست روی صورت
        drawCircle(
            brush = Brush.horizontalGradient(listOf(Color.Transparent, GLOW.copy(alpha = 0.26f * flick)), startX = 0f, endX = 11.5f),
            radius = 11.5f, center = hc
        )
        // مو
        val hair = Path().apply {
            moveTo(-12.5f, -74f); cubicTo(-14f, -90f, -2f, -92f, 2f, -91.5f)
            cubicTo(12f, -91f, 15f, -80f, 12.5f, -74f)
            cubicTo(10f, -79f, 6f, -83f, 0f, -82f); cubicTo(-5f, -83f, -9f, -79f, -12.5f, -74f); close()
        }
        drawPath(hair, HAIR)
        drawCircle(HAIR, 3.2f, Offset(-6f, -90f))
        drawCircle(HAIR, 3.5f, Offset(4f, -91f))
        drawCircle(HAIR, 3f, Offset(10f, -87f))
        // ابروها: با 👌 بالا می‌رن
        val br2 = 0.9f * okE.coerceIn(0f, 1f)
        drawLine(Color(0xFF1A1210), Offset(-7.4f, -80.3f - br2), Offset(-2.4f, -80.9f - br2), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(Color(0xFF1A1210), Offset(2.4f, -80.9f - br2), Offset(7.4f, -80.3f - br2), strokeWidth = 1.5f, cap = StrokeCap.Round)
        // چشم‌ها و عینک گرد؛ با 👌 چشم چپ‌ش چشمک می‌زنه
        for (sg in listOf(-1f, 1f)) {
            val e = Offset(sg * 4.7f, -75.5f)
            drawCircle(GLOW.copy(alpha = 0.10f * flick), 3.9f, e)
            if (blink || (sg < 0f && okE > 0.5f)) {
                drawArc(
                    Color(0xFF1A1210), 200f, 140f, false, Offset(e.x - 2.2f, e.y - 1.6f), Size(4.4f, 3.2f),
                    style = Stroke(0.9f, cap = StrokeCap.Round)
                )
            } else {
                drawCircle(Color(0xFFF3EAE2), 2.2f, e)
                drawCircle(Color(0xFF3A281C), 1.25f, Offset(e.x + 0.3f, e.y + 0.2f))
                drawCircle(Color.White, 0.4f, Offset(e.x + 0.8f, e.y - 0.4f))
            }
            drawCircle(FRAME, 3.9f, e, style = Stroke(0.9f))
        }
        drawLine(FRAME, Offset(-0.9f, -76f), Offset(0.9f, -76f), strokeWidth = 0.9f)
        drawLine(FRAME, Offset(-8.6f, -76f), Offset(-11.8f, -76.6f), strokeWidth = 0.8f, cap = StrokeCap.Round)
        drawLine(FRAME, Offset(8.6f, -76f), Offset(11.8f, -76.6f), strokeWidth = 0.8f, cap = StrokeCap.Round)
        // گونه و دهن: لبخند، با 👌 و «بفرما» بازتر
        drawCircle(Color(0xFFE08A7A).copy(alpha = 0.14f), 2.4f, Offset(-7.2f, -71.3f))
        drawCircle(Color(0xFFE08A7A).copy(alpha = 0.14f), 2.4f, Offset(7.2f, -71.3f))
        if (joy > 0.3f) {
            drawArc(
                Color(0xFF3E1A1A), 0f, 180f, true,
                Offset(-3.8f, -70.8f), Size(7.6f, 3.2f + 2.4f * joy)
            )
        } else {
            drawPath(
                Path().apply { moveTo(-3.6f, -69.6f); quadraticBezierTo(0f, -67.8f, 3.6f, -69.6f) },
                Color(0xFF8A4A44), style = Stroke(1.2f, cap = StrokeCap.Round)
            )
        }
        // نور سبز لبه‌ی سر
        drawArc(
            GLOW.copy(alpha = 0.6f * flick), -55f, 110f, false, Offset(-11.5f, -87.5f), Size(23f, 23f),
            style = Stroke(1.4f, cap = StrokeCap.Round)
        )

        // جلوه‌ی 👌: جرقه‌های کنار دست
        if (okE > 0.3f) {
            val a = okE.coerceIn(0f, 1f)
            val pulse = 0.5f + 0.5f * sin(TAU * 2f * t)
            drawSparkle(Offset(rH.x + 7f, rH.y - 14f), 2.6f + pulse, GLOW.copy(alpha = a * (0.5f + 0.5f * pulse)))
            drawSparkle(Offset(rH.x - 5f, rH.y - 17f), 2f + (1f - pulse), GLOW.copy(alpha = a * (1f - 0.5f * pulse)))
        }
        // جلوه‌ی «بفرما»: نشان سبز تأیید بالای کف دست و جرقه‌های رو به بالا
        if (offer > 0.1f) {
            val a = offer.coerceIn(0f, 1f)
            val bc = Offset(rH.x + 2f, rH.y - 14f + sin(TAU * 2f * t) * 1.4f)
            drawCircle(GLOW.copy(alpha = 0.18f * a), 8.5f, bc)
            drawCircle(GLOW.copy(alpha = 0.9f * a), 6f, bc)
            drawPath(
                Path().apply { moveTo(bc.x - 2.6f, bc.y + 0.2f); lineTo(bc.x - 0.6f, bc.y + 2.4f); lineTo(bc.x + 3f, bc.y - 2.2f) },
                Color(0xFF052E16).copy(alpha = a), style = Stroke(1.7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            for (i in 0..2) {
                val ph = (t * 2f + i / 3f) % 1f
                drawSparkle(
                    Offset(rH.x + (i - 1) * 9f, rH.y - 22f - ph * 8f),
                    2.4f + (1f - ph) * 1.8f, GLOW.copy(alpha = a * (1f - ph))
                )
            }
        }
    }
}

// ───────────────────────── دست‌ها و جرقه ─────────────────────────

/** 👌 : حلقه‌ی شست و اشاره + سه انگشت باز */
private fun DrawScope.drawHandOk(c: Offset, wig: Float) {
    withTransform({ rotate(wig, c) }) {
        drawLine(SKIN, Offset(c.x + 1.2f, c.y - 2f), Offset(c.x + 3.2f, c.y - 10.5f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(SKIN, Offset(c.x + 3.4f, c.y - 1f), Offset(c.x + 8f, c.y - 8.5f), strokeWidth = 2.5f, cap = StrokeCap.Round)
        drawLine(SKIN, Offset(c.x + 4.6f, c.y + 1.6f), Offset(c.x + 10.8f, c.y - 4.2f), strokeWidth = 2.4f, cap = StrokeCap.Round)
        drawCircle(SKIN, 4.6f, c)
        val ring = Offset(c.x - 3.4f, c.y - 4.6f)
        drawCircle(SKIN, 3.3f, ring)
        drawCircle(SKIN_D, 1.35f, ring)
        drawCircle(SKIN_D, 3.3f, ring, style = Stroke(0.5f))
    }
}

/** کف دست باز، رو به بالا: «بفرما» */
private fun DrawScope.drawHandOpen(c: Offset) {
    drawOval(SKIN, Offset(c.x - 4.5f, c.y - 2.6f), Size(9f, 5.2f))
    drawRoundRect(SKIN, Offset(c.x + 3f, c.y - 2.1f), Size(7.5f, 3.2f), CornerRadius(1.6f, 1.6f))
    drawRoundRect(SKIN, Offset(c.x + 3.2f, c.y + 0.6f), Size(6.5f, 2.8f), CornerRadius(1.4f, 1.4f))
    drawLine(SKIN, Offset(c.x - 1f, c.y - 2f), Offset(c.x + 3.5f, c.y - 5f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    drawOval(Color.White.copy(alpha = 0.12f), Offset(c.x - 3.2f, c.y - 1.6f), Size(6f, 2.6f))
}

private fun DrawScope.drawSparkle(c: Offset, s: Float, color: Color) {
    val k = s * 0.28f
    val p = Path().apply {
        moveTo(c.x, c.y - s)
        quadraticBezierTo(c.x + k, c.y - k, c.x + s, c.y)
        quadraticBezierTo(c.x + k, c.y + k, c.x, c.y + s)
        quadraticBezierTo(c.x - k, c.y + k, c.x - s, c.y)
        quadraticBezierTo(c.x - k, c.y - k, c.x, c.y - s)
        close()
    }
    drawPath(p, color)
}
