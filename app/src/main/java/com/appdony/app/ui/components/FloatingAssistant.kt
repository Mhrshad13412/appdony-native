package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.util.AssistantBus
import com.appdony.app.util.AssistantSay
import com.appdony.app.util.DownloadPhase
import com.appdony.app.util.SayKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val AvatarSize = 60.dp
private val EdgeMargin = 12.dp
private val AvatarShape = RoundedCornerShape(24.dp)
private val GoldColor = Color(0xFFFFB300)
private val GreenColor = Color(0xFF34C759)
private val RedColor = Color(0xFFFF6B6B)

// رنگ‌های گربه
private val FurColor = Color(0xFFFFB566)
private val FurDark = Color(0xFFE48A3E)
private val CreamColor = Color(0xFFFFF1DC)
private val OutlineColor = Color(0xFF6B3E1E)
private val PinkColor = Color(0xFFFF8FA3)
private val EyeColor = Color(0xFF2A1A14)

// رنگ کاموای هر دکمه (چت، دانلودها، جستجو) و کاموای خودِ گربه
private val YarnColors = listOf(Color(0xFFEC6B8E), Color(0xFF4DB6AC), Color(0xFFFFB74D))
private val CatYarn = Color(0xFFEC6B8E)

// ───────────────────────── رسم کاموا ─────────────────────────

/** گلوله‌ی کاموا: چند دور نخ دور هم می‌پیچه و با [spin] می‌چرخه */
private fun DrawScope.drawYarnBall(center: Offset, r: Float, color: Color, spin: Float) {
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

// ───────────────────────── رسم گربه ─────────────────────────

/** حالت لحظه‌ای گربه؛ همه‌ی مقدارها بین ۰ و ۱ (یا ‎-۱ تا ۱) هستن */
private class CatPose(
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
    val wiggle: Float    // قل خوردن گلوله‌ی کاموای کنار پا
)

private fun DrawScope.drawCat(p: CatPose) {
    val u = 1.dp.toPx()
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    val line = 1.5f * u

    withTransform({ scale(p.face, 1f, pt(30f, 30f)) }) {

        // ── دم (پشت بدن) ──
        withTransform({ rotate(p.tail * 14f, pt(20f, 52f)) }) {
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
            drawPath(wrap, lerp(CatYarn, Color.Black, 0.22f), style = Stroke(width = 2.6f * u, cap = StrokeCap.Round))
            drawPath(wrap, CatYarn, style = Stroke(width = 1.8f * u, cap = StrokeCap.Round))
        }

        // ── گلوله‌ی کاموای کنار پا + نخی که به بدن وصله ──
        val ballC = pt(46f + p.wiggle * 1.2f, 55f)
        val link = Path().apply {
            moveTo(40.5f * u, 49f * u)
            quadraticBezierTo(47f * u, 47f * u, ballC.x, ballC.y - 4.5f * u)
        }
        drawPath(link, CatYarn, style = Stroke(width = 1.5f * u, cap = StrokeCap.Round))
        drawYarnBall(ballC, 5.2f * u, CatYarn, p.wiggle * 70f)

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
        listOf(22.3f, 37.7f).forEach { cx ->
            val cy = 29.5f
            if (eyeAlpha > 0.02f) {
                drawOval(
                    color = EyeColor.copy(alpha = eyeAlpha),
                    topLeft = pt(cx - eyeR, cy - eyeR * open),
                    size = Size(eyeR * 2f * u, eyeR * 2f * open * u)
                )
                if (open > 0.45f) {
                    drawCircle(
                        Color(0xFF6B4A3A).copy(alpha = 0.55f * eyeAlpha), 3.4f * u,
                        pt(cx + p.lookX * 1.4f, cy + 1.6f + p.lookY * 1.4f)
                    )
                    drawCircle(
                        Color.White.copy(alpha = eyeAlpha), 2.4f * u,
                        pt(cx - 1.9f + p.lookX * 1.8f, cy - 2.3f + p.lookY * 1.8f)
                    )
                    drawCircle(
                        Color.White.copy(alpha = 0.85f * eyeAlpha), 1.1f * u,
                        pt(cx + 2.3f + p.lookX * 1.2f, cy + 2.4f + p.lookY * 1.2f)
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
    }
}

// ───────────────────────── حرکت‌ها ─────────────────────────

private class AvatarMotion {
    val offX = Animatable(0f)
    val offY = Animatable(0f)
    val hideShift = Animatable(0f)
    val tilt = Animatable(0f)

    val blink = Animatable(0f)
    val ear = Animatable(0f)
    val pounce = Animatable(0f)
    val land = Animatable(0f)

    // پیشرفت بیرون اومدن کاموا‌های منو
    val menuProg = List(3) { Animatable(0f) }

    // مسیر کاموا‌ها موقع بررسی لینک: ۰ بالای صفحه، ۱ آویزون و بازیگوش، ۲ قل‌خوران سمت راست، ۳ برگشته پیش گربه
    val dlPos = List(3) { Animatable(0f) }
    var dlJob: Job? = null

    var lastLean = 0f

    suspend fun lean(target: Float) {
        tilt.animateTo(target, spring(0.8f, Spring.StiffnessLow))
    }

    suspend fun relax() {
        tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow))
    }

    suspend fun blinkOnce() {
        blink.animateTo(1f, tween(80))
        blink.animateTo(0f, tween(130))
    }

    suspend fun twitchEar() {
        ear.animateTo(1f, tween(70))
        ear.animateTo(-0.5f, tween(70))
        ear.animateTo(0f, tween(140))
    }

    suspend fun hop(strength: Float = 1f) {
        pounce.animateTo(strength, tween(130))
        pounce.animateTo(0f, spring(0.35f, Spring.StiffnessMedium))
    }

    suspend fun landSquash() {
        land.snapTo(1f)
        land.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
    }
}

// ───────────────────────── جمله‌ها ─────────────────────────

private fun String.stripEmoji(): String =
    replace(Regex("[\\p{So}\\uFE0F\\u200D]"), "").replace(Regex("\\s+"), " ").trim()

private object AssistantPhrases {
    private val general = listOf(
        "میو! اگه بازی خاصی تو ذهنته، از دکمه‌ی جستجو اسمش رو بزن 🔍",
        "خسته نشدی؟ یه نفس عمیق بکش، بعد برگرد سراغ دانلود 😄",
        "هر وقت خواستی با من چت کن؛ بازی خوب هم پیدا می‌کنم!",
        "فضای گوشیت پره؟ قبل از نصب بازی‌های بزرگ یه نگاهی بهش بنداز.",
        "اگه نصب نشد، گزینه‌ی «نصب از منابع ناشناس» رو چک کن.",
        "منو بگیر و هر جا خواستی ببر؛ ناراحت نمی‌شم 😉",
        "از تنظیمات می‌تونی رنگ برنامه و اندازه‌ی نوشته‌ها رو عوض کنی.",
        "اگه مزاحمتم، از تنظیمات می‌تونی منو مخفی کنی... ولی دلم برات تنگ می‌شه.",
        "علاقه‌مندی‌هات رو از منوی حساب کاربری پیدا می‌کنی.",
        "فایل‌هایی که دانلود کردی توی تب «دانلودها» منتظر نصبن.",
        "یه بازی خوب مثل یه چای داغه؛ عجله نکن، خوب انتخابش کن ☕",
        "قبل از دانلود، نظر بقیه‌ی کاربرها پایین صفحه‌ی هر پست رو بخون.",
        "دانلود قطع شد؟ اینترنت یا VPN رو عوض کن و دوباره بزن.",
        "اینترنتت ضعیفه؟ دانلودهای سنگین رو با وای‌فای انجام بده.",
        "بعد از نصب، فایل نصبی رو پاک کن تا حافظه آزاد بشه.",
        "نسخه‌ی اندروید گوشی‌ت باید با نیازمندی بازی بخونه، وگرنه نصب نمی‌شه.",
        "پیشنهاد می‌خوای؟ از من بپرس، یکی جور می‌کنم 🎮",
        "من همین‌جام؛ اگه جایی گیر کردی فقط بزن روم.",
        "حوصله‌ات سر رفته؟ یه چرخی بین بازی‌ها بزن، حتماً چیزی پیدا می‌شه.",
        "بازی‌های سنگین رو با شارژ کافی نصب کن 🔋",
        "گاهی کش برنامه‌ها رو پاک کن، گوشی سبک‌تر می‌شه.",
        "اگه بازی باز نشد، یه بار گوشی رو ری‌استارت کن.",
        "تنها نیستی؛ من همیشه همین گوشه منتظرم 🐾",
        "حالت شب و روز رو هم می‌تونی از تنظیمات عوض کنی.",
        "کاموام رو دیدی؟ بدون اون خوابم نمی‌بره 🧶"
    ).map { AssistantSay(it.stripEmoji(), SayKind.TIP) }

    private val plus = listOf(
        "با خرید اپدونی پلاس به بازی‌های با کیفیت دست پیدا کن.",
        "بازی‌های ویژه و باکیفیت توی اپدونی پلاس منتظرتن.",
        "می‌خوای بازی‌های درجه‌یک داشته باشی؟ اپدونی پلاس رو بخر.",
        "اپدونی پلاس رو امتحان کن؛ بازی‌های باکیفیت‌تر اون‌جاست."
    ).map { AssistantSay(it, SayKind.PLUS) }

    private val games = listOf(
        "بازی‌ها دسته‌بندی دارن؛ اگه سبک خاصی دوست داری دسته‌ها رو ببین.",
        "قبل از دانلود، حجم بازی رو با فضای گوشیت مقایسه کن.",
        "بازی آفلاین می‌خوای یا آنلاین؟ از من بپرس!",
        "بعضی بازی‌ها فایل دیتای جدا دارن؛ توضیحات پست رو حتماً بخون.",
        "بازی نصب شد ولی باز نمی‌شه؟ فایل دیتا رو چک کن.",
        "اینجا کلی بازی هست؛ اگه گیج شدی بگو یکی برات انتخاب کنم 🎲",
        "بازی بزرگ؟ اول دانلود، بعد نصب؛ صبور باش.",
        "یادت نره استراحت هم بکنی، چشمات حق دارن 👀"
    ).map { AssistantSay(it.stripEmoji(), SayKind.TIP) }

    private val apps = listOf(
        "دنبال یه برنامه‌ی خاص می‌گردی؟ جستجو سریع‌ترین راهه.",
        "قبل از نصب، نسخه‌ی اندرویدی که برنامه لازم داره رو چک کن.",
        "برای هر کاری برنامه هست؛ بگو چی لازم داری تا دنبالش بگردم.",
        "برنامه‌هات رو به‌روز نگه دار، امنیتشون بیشتر می‌شه.",
        "نصب از منبع ناشناس؟ یه بار اجازه‌اش رو بده و تمام.",
        "برنامه‌ی جدید نصب می‌کنی؟ دسترسی‌هاش رو یه نگاه بنداز."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val blog = listOf(
        "اینجا مقاله‌های آموزشیه؛ جواب خیلی از مشکلات نصب و دانلود هم توشه.",
        "وبلاگ رو ورق بزن، شاید چیز جالبی پیدا کنی.",
        "اخبار تازه‌ی دنیای بازی و برنامه همین‌جاست.",
        "سؤالی داشتی؟ قبل از گشتن بین مقاله‌ها از من هم بپرس."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val downloads = listOf(
        "دانلودهات اینجاست؛ هر وقت خواستی نصبشون کن.",
        "دانلود کامل شد؟ دکمه‌ی «نصب» رو بزن.",
        "فایل‌های نصب‌شده رو از اینجا پاک کن تا حافظه خالی بشه.",
        "دانلود گیر کرده؟ پاکش کن و دوباره شروع کن.",
        "دانلود کند شد؟ اینترنت رو عوض کن."
    ).map { AssistantSay(it, SayKind.DOWNLOAD) }

    private val search = listOf(
        "اسم بازی رو فارسی یا انگلیسی امتحان کن؛ گاهی یکیش بهتر جواب می‌ده.",
        "نتیجه‌ای نبود؟ یه کلمه‌ی کوتاه‌تر بنویس.",
        "املا رو چک کن، گاهی یه حرف همه‌چی رو عوض می‌کنه 😅",
        "جستجوهای قبلیت همین‌جا ذخیره می‌شه."
    ).map { AssistantSay(it.stripEmoji(), SayKind.SEARCH) }

    private val detail = listOf(
        "قبل از دانلود، توضیحات و نیازمندی‌ها رو بخون.",
        "نظرها رو هم ببین؛ تجربه‌ی بقیه کمکت می‌کنه.",
        "پایین صفحه مطالب مشابه هست؛ شاید بهتر باشه.",
        "دکمه‌ی دانلود آماده‌ست؛ فقط فضای گوشی رو چک کن."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val recent = ArrayDeque<String>()

    fun next(route: String?): AssistantSay {
        // گاهی (حدود ۱۴٪) تبلیغ ملایم اپدونی پلاس
        val pool = if (Random.nextFloat() < 0.14f) {
            plus
        } else {
            val r = route.orEmpty()
            val extra = when {
                r.startsWith("games") -> games
                r.startsWith("apps") -> apps
                r.startsWith("blog") -> blog
                r.startsWith("downloads") -> downloads
                r.startsWith("search") -> search
                r.startsWith("detail") -> detail
                else -> emptyList()
            }
            general + extra + extra
        }
        val fresh = pool.filter { it.text !in recent }.ifEmpty { pool }
        val pick = fresh.random()
        recent.addLast(pick.text)
        while (recent.size > 10) recent.removeFirst()
        return pick
    }
}

private fun shortName(title: String): String {
    val quoted = Regex("[“”\"«»](.+?)[“”\"«»]").find(title)?.groupValues?.getOrNull(1)?.trim()
    if (!quoted.isNullOrBlank()) return quoted.take(45)
    return title.removePrefix("دانلود").trim().take(45)
}

/** یه اسم تصادفی از پست‌های خود اپدونی پیشنهاد می‌ده (اگه اینترنت نبود null) */
private suspend fun fetchSuggestion(): AssistantSay? = try {
    val titles = ApiClient.api.getPosts(perPage = 12)
        .map { shortName(it.cleanTitle()) }
        .filter { it.length > 2 }
    val pick = titles.randomOrNull()
    if (pick == null) null
    else AssistantSay("این رو دیدی؟ «$pick» رو توی جستجو بزن.", SayKind.SUGGEST)
} catch (e: Exception) {
    null
}

private fun kindIcon(kind: SayKind): ImageVector = when (kind) {
    SayKind.TIP -> Icons.Outlined.Lightbulb
    SayKind.SEARCH, SayKind.SUGGEST -> Icons.Outlined.Search
    SayKind.DOWNLOAD -> Icons.Outlined.Download
    SayKind.DONE -> Icons.Outlined.CheckCircle
    SayKind.FAIL -> Icons.Outlined.ErrorOutline
    SayKind.PLUS -> Icons.Outlined.WorkspacePremium
}

private fun kindColor(kind: SayKind, fallback: Color): Color = when (kind) {
    SayKind.PLUS -> GoldColor
    SayKind.DONE -> GreenColor
    SayKind.FAIL -> RedColor
    else -> fallback
}

// ───────────────────────── مسیر کاموا‌ها موقع بررسی لینک ─────────────────────────

/** قل خوردن: ۱ دور و نیم تا وسط مسیر و بقیه موقع برگشتن */
private fun dlSpin(q: Float): Float = q * 540f

/** کاموا تا وقتی از بالا نیومده نامرئیه و آخر مسیر پیش گربه محو می‌شه */
private fun dlAlpha(q: Float): Float = when {
    q <= 0f -> 0f
    q < 2.6f -> 1f
    else -> ((3f - q) / 0.4f).coerceIn(0f, 1f)
}

/** پرش‌های کوچیک موقع افتادن از بالا: هر بار پایین می‌رسه یه کم بالا می‌پره */
private fun dlBounce(q: Float): Float {
    if (q <= 0f || q >= 1f) return 0f
    return abs(sin(q * Math.PI * 3.0)).toFloat() * (1f - q) * 0.9f
}

// ───────────────────────── کامپوننت اصلی ─────────────────────────

@Composable
fun FloatingAssistant(
    currentRoute: String?,
    visible: Boolean,
    bottomBarHeight: Dp,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val motion = remember { AvatarMotion() }
    val routeNow by rememberUpdatedState(currentRoute)
    val visibleNow by rememberUpdatedState(visible)

    var ready by remember { mutableStateOf(false) }
    var onRight by remember { mutableStateOf(false) }
    var lowerHalf by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleSay by remember { mutableStateOf<AssistantSay?>(null) }
    var lastSay by remember { mutableStateOf(AssistantSay("")) }
    var activity by remember { mutableIntStateOf(0) }
    var dlActive by remember { mutableStateOf(false) }
    var offerOnly by remember { mutableStateOf(false) }

    // ترتیب: چت، دانلودها، جستجو
    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با من", onChatClick),
        Triple(Icons.Outlined.Download, "دانلودها", onDownloadsClick),
        Triple(Icons.Outlined.Search, "جستجو", onSearchClick)
    )
    val angles = listOf(0f, 62f, -62f)

    // نقطه‌ی شروع و پایان نخ هر گلوله؛ توی Layout پر می‌شه و Canvas نخ‌ها می‌خونه
    // هر گلوله ۶ عدد: ballX, ballY, anchorX, anchorY, alpha, sag
    val threadData = remember { FloatArray(18) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val sizePx = with(density) { AvatarSize.toPx() }
            val marginPx = with(density) { EdgeMargin.toPx() }
            val barPx = with(density) { bottomBarHeight.toPx() }
            val topInset = WindowInsets.statusBars.getTop(density).toFloat()
            val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()

            val topLimit = topInset + marginPx / 2f
            val bottomLimit = (hPx - bottomInset - barPx - sizePx - with(density) { 6.dp.toPx() })
                .coerceAtLeast(topLimit)

            // جای اولیه: پایین-چپ، بالای نوار پایینی
            LaunchedEffect(wPx, hPx) {
                if (!ready && wPx > 0f && hPx > 0f) {
                    motion.offX.snapTo(marginPx)
                    motion.offY.snapTo(
                        (bottomLimit - with(density) { 24.dp.toPx() }).coerceIn(topLimit, bottomLimit)
                    )
                    onRight = false
                    lowerHalf = true
                    ready = true
                }
            }

            LaunchedEffect(ready, topLimit, bottomLimit) {
                if (ready) {
                    val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                    if (ty != motion.offY.value) {
                        motion.offY.animateTo(ty, spring(0.8f, Spring.StiffnessLow))
                    }
                    lowerHalf = ty + sizePx / 2f > hPx / 2f
                }
            }
            LaunchedEffect(ready, wPx) {
                if (ready) {
                    motion.offX.snapTo(if (onRight) wPx - sizePx - marginPx else marginPx)
                }
            }

            // قایم شدن کنار صفحه و کم‌رنگ شدن
            LaunchedEffect(hidden, onRight, ready) {
                val target = if (!hidden) 0f else (if (onRight) 1f else -1f) * (sizePx * 0.52f + marginPx)
                motion.hideShift.animateTo(target, spring(0.8f, Spring.StiffnessLow))
            }
            val bodyAlpha by animateFloatAsState(if (hidden) 0.38f else 1f, tween(500), label = "assistant_alpha")

            LaunchedEffect(activity, expanded, bubbleSay, dragging, hidden, ready, visible) {
                if (ready && visible && !expanded && bubbleSay == null && !dragging && !hidden) {
                    delay(12_000)
                    hidden = true
                }
            }

            // پلک زدن (گاهی دوبار پشت هم)
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                while (true) {
                    delay((2_200L..4_800L).random())
                    motion.blinkOnce()
                    if (Random.nextFloat() < 0.25f) {
                        delay(160)
                        motion.blinkOnce()
                    }
                }
            }

            // تکون خوردن گوش
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                while (true) {
                    delay((3_500L..8_000L).random())
                    motion.twitchEar()
                }
            }

            // کاموا‌های منو: یکی‌یکی از کنار گربه قل می‌خورن بیرون
            LaunchedEffect(expanded) {
                if (expanded) { offerOnly = false; showMenu = true }
                coroutineScope {
                    motion.menuProg.forEachIndexed { i, a ->
                        launch {
                            delay(if (expanded) 120L + i * 230L else (2 - i) * 120L)
                            a.animateTo(if (expanded) 1f else 0f, spring(0.62f, Spring.StiffnessLow))
                        }
                    }
                }
                if (!expanded) showMenu = false
            }

            suspend fun speak(say: AssistantSay, holdMs: Long) {
                hidden = false
                lastSay = say
                bubbleSay = say
                delay(holdMs)
                if (bubbleSay === say) bubbleSay = null
            }

            // پرش‌های بازیگوشانه‌ی گربه تا وقتی لینک بررسی می‌شه
            LaunchedEffect(dlActive) {
                if (dlActive) {
                    delay(400)
                    while (true) {
                        motion.hop(1f)
                        delay((250L..600L).random())
                    }
                }
            }

            // خبرهای فوری (مثلاً تموم شدن دانلود)
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                AssistantBus.events.collect { say ->
                    if (visibleNow && !dragging) {
                        expanded = false
                        speak(say, 9_000)
                    }
                }
            }

            // دکمه‌ی دانلود: کاموا‌ها از بالا آویزون می‌شن و گربه باهاشون بازی می‌کنه،
            // بعد قل می‌خورن سمت راست و آخرش فقط کاموای «دانلودها» تعارف می‌شه
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                var startMs = 0L
                AssistantBus.download.collect { phase ->
                    when (phase) {
                        DownloadPhase.CHECKING -> {
                            if (!visibleNow || dragging) return@collect
                            motion.dlJob?.cancel()
                            hidden = false
                            expanded = false
                            showMenu = false
                            offerOnly = false
                            bubbleSay = null
                            motion.menuProg.forEach { it.snapTo(0f) }
                            motion.dlPos.forEach { it.snapTo(0f) }
                            dlActive = true
                            startMs = System.currentTimeMillis()
                            scope.launch {
                                val say = AssistantSay("تا لینک رو چک می‌کنم، بیا با کاموا بازی کنیم 🧶", SayKind.DOWNLOAD)
                                AssistantBus.noteShown(say.text)
                                speak(say, 6_000)
                            }
                            coroutineScope {
                                motion.dlPos.forEachIndexed { i, a ->
                                    launch {
                                        delay(i * 260L)
                                        a.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
                                        delay(350)
                                        a.animateTo(2f, tween(1100, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        }

                        DownloadPhase.READY, DownloadPhase.FAILED -> {
                            if (dlActive) {
                                val wait = 3_500L - (System.currentTimeMillis() - startMs)
                                if (wait > 0) delay(wait)
                                // یکی‌یکی قل می‌خورن پیش گربه
                                coroutineScope {
                                    motion.dlPos.forEachIndexed { i, a ->
                                        launch {
                                            delay(i * 380L)
                                            a.animateTo(3f, tween(900, easing = FastOutSlowInEasing))
                                        }
                                    }
                                }
                                dlActive = false
                                motion.dlPos.forEach { it.snapTo(0f) }
                            }
                            if (!visibleNow) return@collect
                            if (phase == DownloadPhase.READY) {
                                // فقط کاموای «دانلودها» تعارف می‌شه
                                offerOnly = true
                                showMenu = true
                                motion.menuProg.forEach { it.snapTo(0f) }
                                scope.launch {
                                    val say = AssistantSay("بیا، این کاموا تو رو می‌بره بخش دانلودها 🧶", SayKind.DOWNLOAD)
                                    AssistantBus.noteShown(say.text)
                                    speak(say, 9_000)
                                }
                                motion.dlJob = scope.launch {
                                    motion.menuProg[1].animateTo(1f, spring(0.55f, Spring.StiffnessLow))
                                    delay(8_500)
                                    if (offerOnly) {
                                        motion.menuProg[1].animateTo(0f, spring(0.6f, Spring.StiffnessMediumLow))
                                        offerOnly = false
                                        showMenu = false
                                    }
                                }
                            } else {
                                scope.launch {
                                    val say = AssistantSay("این لینک جواب نداد. یه لینک دیگه امتحان کن.", SayKind.FAIL)
                                    AssistantBus.noteShown(say.text)
                                    speak(say, 6_000)
                                }
                            }
                        }
                    }
                }
            }

            // هر ~۳۰ ثانیه یه جمله
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                delay(7_000)
                while (true) {
                    if (visibleNow && !expanded && !dragging && bubbleSay == null) {
                        val say = if (Random.nextFloat() < 0.15f) {
                            fetchSuggestion() ?: AssistantPhrases.next(routeNow)
                        } else {
                            AssistantPhrases.next(routeNow)
                        }
                        AssistantBus.noteShown(say.text)
                        speak(say, 7_500)
                    }
                    delay((28_000L..38_000L).random())
                }
            }

            val infinite = rememberInfiniteTransition(label = "assistant_infinite")
            val bob by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bob"
            )
            val talk by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "talk"
            )
            val tailSlow by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "tail_slow"
            )
            val tailFast by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "tail_fast"
            )
            val swat by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(330, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "swat"
            )
            val speakAmt by animateFloatAsState(
                targetValue = if (bubbleSay != null) 1f else 0f,
                animationSpec = tween(300),
                label = "speak"
            )
            // بغل گرفته شدن (موقع کشیدن)
            val scare by animateFloatAsState(
                targetValue = if (dragging) 1f else 0f,
                animationSpec = spring(0.6f, Spring.StiffnessLow),
                label = "scare"
            )
            val playAmt by animateFloatAsState(
                targetValue = if (dlActive) 1f else 0f,
                animationSpec = tween(350),
                label = "play"
            )
            val offerAmt by animateFloatAsState(
                targetValue = if (offerOnly) 1f else 0f,
                animationSpec = tween(300),
                label = "offer"
            )
            val happyAmt by animateFloatAsState(
                targetValue = if (offerOnly || expanded) 1f else 0f,
                animationSpec = tween(250),
                label = "happy"
            )
            // جهت نگاه: همیشه به داخل صفحه؛ موقع عوض شدن طرف، نرم برمی‌گرده
            val face by animateFloatAsState(
                targetValue = if (onRight) -1f else 1f,
                animationSpec = spring(0.7f, Spring.StiffnessLow),
                label = "face"
            )
            val bobPx = with(density) { 3.dp.toPx() }
            val jellyPx = with(density) { 6.dp.toPx() }
            val hopPx = with(density) { 12.dp.toPx() }

            // پرش نرم و ژله‌ای وقتی گربه حرف می‌زنه
            val jelly = remember { Animatable(0f) }
            LaunchedEffect(bubbleSay != null) {
                if (bubbleSay != null) {
                    while (true) {
                        jelly.animateTo(1f, spring(0.3f, Spring.StiffnessLow))
                        jelly.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
                    }
                } else {
                    jelly.animateTo(0f, spring(0.4f, Spring.StiffnessLow))
                }
            }

            if (ready && visible) {
                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        // ── نخ کاموا‌ها (پشت همه چیز) ──
                        Canvas(modifier = Modifier.layoutId("threads").fillMaxSize()) {
                            // این‌ها فقط برای اینه که با هر حرکت دوباره کشیده بشه
                            motion.menuProg.forEach { it.value }
                            motion.dlPos.forEach { it.value }
                            motion.offX.value
                            motion.offY.value
                            val u = 1.dp.toPx()
                            for (i in 0..2) {
                                val a = threadData[i * 6 + 4]
                                if (a <= 0.02f) continue
                                val bx = threadData[i * 6]
                                val by = threadData[i * 6 + 1]
                                val ax = threadData[i * 6 + 2]
                                val ay = threadData[i * 6 + 3]
                                val sag = threadData[i * 6 + 5] * u
                                val path = Path().apply {
                                    moveTo(ax, ay)
                                    quadraticBezierTo((ax + bx) / 2f, (ay + by) / 2f + sag, bx, by)
                                }
                                drawPath(
                                    path, YarnColors[i].copy(alpha = a),
                                    style = Stroke(width = 1.7f * u, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // ── بالن پیام ──
                        Box(modifier = Modifier.layoutId("extras")) {
                            AnimatedVisibility(
                                visible = bubbleSay != null && !expanded,
                                enter = fadeIn(tween(220)) + scaleIn(
                                    spring(0.6f, Spring.StiffnessMediumLow),
                                    initialScale = 0.7f,
                                    transformOrigin = TransformOrigin(if (onRight) 1f else 0f, if (lowerHalf) 1f else 0f)
                                ),
                                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f)
                            ) {
                                SpeechBubble(
                                    say = lastSay,
                                    onRight = onRight,
                                    lowerHalf = lowerHalf,
                                    onClick = {
                                        val s = lastSay
                                        bubbleSay = null
                                        activity++
                                        when (s.kind) {
                                            SayKind.DONE -> onDownloadsClick()
                                            SayKind.SUGGEST -> onSearchClick()
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                        }

                        // ── گلوله‌های کاموا (منو) ──
                        if (showMenu || dlActive) {
                            items.forEachIndexed { i, (icon, label, action) ->
                                if (dlActive || !offerOnly || i == 1) {
                                    Box(modifier = Modifier.layoutId("item$i")) {
                                        YarnButton(
                                            icon = icon,
                                            label = label,
                                            tint = YarnColors[i],
                                            progress = {
                                                if (dlActive) dlAlpha(motion.dlPos[i].value)
                                                else motion.menuProg[i].value
                                            },
                                            spin = {
                                                if (dlActive) dlSpin(motion.dlPos[i].value)
                                                else motion.menuProg[i].value * 600f
                                            },
                                            bob = { bob },
                                            flip = if (i % 2 == 0) 1f else -1f,
                                            onClick = {
                                                if (!dlActive) {
                                                    if (expanded) {
                                                        expanded = false
                                                        activity++
                                                        action()
                                                    } else if (offerOnly) {
                                                        offerOnly = false
                                                        showMenu = false
                                                        scope.launch { motion.menuProg[1].snapTo(0f) }
                                                        activity++
                                                        action()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ── خود گربه ──
                        Box(
                            modifier = Modifier
                                .layoutId("avatar")
                                .size(AvatarSize)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            activity++
                                            scope.launch { motion.hop(0.6f) }
                                            if (hidden) {
                                                hidden = false
                                            } else {
                                                bubbleSay = null
                                                expanded = !expanded
                                            }
                                        }
                                    )
                                }
                                .pointerInput(wPx, hPx, topLimit, bottomLimit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = true
                                            expanded = false
                                            bubbleSay = null
                                            hidden = false
                                            activity++
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            scope.launch {
                                                motion.offX.snapTo(
                                                    (motion.offX.value + drag.x).coerceIn(0f, wPx - sizePx)
                                                )
                                                motion.offY.snapTo(
                                                    (motion.offY.value + drag.y).coerceIn(topLimit, bottomLimit)
                                                )
                                            }
                                            val target = (drag.x * 1.6f).coerceIn(-16f, 16f)
                                            if (abs(target - motion.lastLean) > 1.5f) {
                                                motion.lastLean = target
                                                scope.launch { motion.lean(target) }
                                            }
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            activity++
                                            motion.lastLean = 0f
                                            scope.launch {
                                                launch { motion.relax() }
                                                val right = motion.offX.value + sizePx / 2f > wPx / 2f
                                                onRight = right
                                                val tx = if (right) wPx - sizePx - marginPx else marginPx
                                                val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                                                lowerHalf = ty + sizePx / 2f > hPx / 2f
                                                launch { motion.offX.animateTo(tx, spring(0.65f, Spring.StiffnessLow)) }
                                                launch { motion.offY.animateTo(ty, spring(0.75f, Spring.StiffnessMediumLow)) }
                                                // مثل گربه‌ها روی چهار پا فرود میاد
                                                launch { motion.landSquash() }
                                            }
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            motion.lastLean = 0f
                                            scope.launch { motion.relax() }
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        alpha = bodyAlpha
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        rotationZ = motion.tilt.value
                                        translationY = bob * bobPx - jelly.value * jellyPx - motion.pounce.value * hopPx
                                        scaleY = 1f + 0.10f * jelly.value + 0.10f * scare - 0.14f * motion.land.value
                                        scaleX = 1f - 0.06f * jelly.value - 0.05f * scare + 0.10f * motion.land.value
                                    }
                                    .semantics { contentDescription = "دستیار" },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    shape = AvatarShape,
                                    color = surfaceColor,
                                    border = BorderStroke(1.5.dp, primary),
                                    shadowElevation = 10.dp,
                                    content = {}
                                )

                                Canvas(modifier = Modifier.matchParentSize()) {
                                    // وقتی کاموا از کنارش قل می‌خوره، پنجه‌اش بالا میاد
                                    var reach = 0f
                                    motion.menuProg.forEach { m ->
                                        val v = m.value
                                        if (v > 0f && v < 0.45f) {
                                            val r = sin(Math.PI * v / 0.45).toFloat()
                                            if (r > reach) reach = r
                                        }
                                    }
                                    val tail = ((tailSlow - 0.5f) * 2f) * (1f - playAmt) +
                                        ((tailFast - 0.5f) * 2f) * playAmt
                                    val pose = CatPose(
                                        face = face,
                                        blink = motion.blink.value,
                                        ear = motion.ear.value,
                                        tail = tail,
                                        mouth = maxOf(talk * speakAmt, reach * 0.7f, scare * 0.45f),
                                        paw = maxOf(reach, swat * playAmt, offerAmt * 0.75f),
                                        lookX = (bob - 0.5f) * 0.8f * (1f - playAmt) + (swat - 0.5f) * 2f * playAmt,
                                        lookY = -0.8f * playAmt + scare * 0.4f,
                                        scare = scare,
                                        happy = happyAmt,
                                        wiggle = (bob - 0.5f) * 2f * (1f + playAmt)
                                    )
                                    drawCat(pose)
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val threadsM = measurables.first { it.layoutId == "threads" }
                    val avatarM = measurables.first { it.layoutId == "avatar" }
                    val extrasM = measurables.first { it.layoutId == "extras" }
                    val avatarSizeInt = sizePx.roundToInt()
                    val marginInt = marginPx.roundToInt()
                    val gapInt = with(density) { 10.dp.roundToPx() }
                    val maxExtrasW = min(
                        constraints.maxWidth - 2 * marginInt,
                        with(density) { 270.dp.roundToPx() }
                    ).coerceAtLeast(0)
                    val itemMaxW = with(density) { 130.dp.roundToPx() }

                    val threadsP = threadsM.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
                    val avatarP = avatarM.measure(Constraints.fixed(avatarSizeInt, avatarSizeInt))
                    val extrasP = extrasM.measure(Constraints(maxWidth = maxExtrasW))
                    val itemPs = List(3) { i ->
                        measurables.firstOrNull { it.layoutId == "item$i" }
                            ?.measure(Constraints(maxWidth = itemMaxW))
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        threadsP.place(0, 0)

                        val baseX = motion.offX.value.roundToInt()
                        val ay = motion.offY.value.roundToInt()

                        // کاموا‌ها نسبت به جای «عادی» گربه دورش پخش می‌شن
                        val cx = baseX + avatarP.width / 2f
                        val cy = ay + avatarP.height / 2f
                        val dirIn = if (onRight) -1f else 1f
                        val radius = with(density) { 92.dp.toPx() }
                        val minYf = topInset
                        val maxYf = constraints.maxHeight - bottomInset
                        // پیام (بالن)
                        val rawX = if (onRight) baseX + avatarP.width - extrasP.width else baseX
                        val maxX = (constraints.maxWidth - marginInt - extrasP.width).coerceAtLeast(marginInt)
                        val ex = rawX.coerceIn(marginInt, maxX)
                        val rawY = if (lowerHalf) ay - extrasP.height - gapInt else ay + avatarP.height + gapInt
                        val minY = topInset.roundToInt()
                        val maxY = (constraints.maxHeight - bottomInset.roundToInt() - extrasP.height)
                            .coerceAtLeast(minY)
                        val ey = rawY.coerceIn(minY, maxY)
                        extrasP.place(ex, ey)

                        avatarP.place(baseX + motion.hideShift.value.roundToInt(), ay)

                        // کاموا‌ها از کنار پنجه‌ی گربه شروع می‌کنن، کمی بالا میان و بعد می‌رن سرجاشون
                        val pawX = baseX + with(density) { (30f + 7f * face).dp.toPx() }
                        val pawY = ay + with(density) { 50.dp.toPx() }
                        val liftPx = with(density) { 24.dp.toPx() }
                        val ballCenterFromTop = with(density) { 31.dp.toPx() }
                        val sagMenu = 16f

                        for (i in 0..2) threadData[i * 6 + 4] = 0f

                        itemPs.forEachIndexed { i, p ->
                            if (p != null) {
                                val rad = Math.toRadians(angles[i].toDouble())
                                val tx0 = cx + dirIn * radius * cos(rad).toFloat()
                                val ty0 = cy + radius * sin(rad).toFloat()
                                val loX = marginInt + p.width / 2f
                                val hiX = (constraints.maxWidth - marginInt - p.width / 2f).coerceAtLeast(loX)
                                val loY = minYf + p.height / 2f
                                val hiY = (maxYf - p.height / 2f).coerceAtLeast(loY)
                                val tx = tx0.coerceIn(loX, hiX)
                                val ty = ty0.coerceIn(loY, hiY)
                                var dx: Float
                                var dy: Float
                                if (dlActive) {
                                    // ۰→۱ از بالای صفحه آویزون می‌شه و بازیگوشانه می‌پره،
                                    // ۱→۲ قل می‌خوره سمت راست، ۲→۳ برمی‌گرده پیش گربه
                                    val q = motion.dlPos[i].value
                                    val topX = constraints.maxWidth * (0.25f + 0.25f * i)
                                    val pullY = topInset + with(density) { 150.dp.toPx() }
                                    val startY = -p.height.toFloat()
                                    val rightX = constraints.maxWidth - marginInt - p.width / 2f
                                    val rightY = pullY + i * (p.height + with(density) { 6.dp.toPx() })
                                    val bounceH = with(density) { 46.dp.toPx() }
                                    when {
                                        q <= 1f -> {
                                            dx = topX + sin(q * Math.PI * 4.0).toFloat() * with(density) { 10.dp.toPx() } * (1f - q)
                                            dy = startY + (pullY - startY) * q - dlBounce(q) * bounceH
                                        }
                                        q <= 2f -> {
                                            val f = q - 1f
                                            dx = topX + (rightX - topX) * f
                                            dy = pullY + (rightY - pullY) * f
                                        }
                                        else -> {
                                            val f = q - 2f
                                            dx = rightX + (pawX - rightX) * f
                                            dy = rightY + (pawY - rightY) * f
                                        }
                                    }
                                    // نخ: اول از بالای صفحه آویزونه، آخر مسیر به گربه وصله
                                    val hangAlpha = if (q <= 0f) 0f else (1f - (q - 1f) * 2f).coerceIn(0f, 1f)
                                    val returnAlpha = ((q - 2f)).coerceIn(0f, 1f) * dlAlpha(q)
                                    if (hangAlpha > 0f) {
                                        threadData[i * 6 + 2] = topX
                                        threadData[i * 6 + 3] = -with(density) { 20.dp.toPx() }
                                        threadData[i * 6 + 4] = hangAlpha
                                        threadData[i * 6 + 5] = 0f
                                    } else if (returnAlpha > 0f) {
                                        threadData[i * 6 + 2] = pawX
                                        threadData[i * 6 + 3] = pawY
                                        threadData[i * 6 + 4] = returnAlpha
                                        threadData[i * 6 + 5] = sagMenu
                                    }
                                } else {
                                    val prog = motion.menuProg[i].value
                                    dx = pawX + (tx - pawX) * prog
                                    dy = pawY + (ty - pawY) * prog - sin(Math.PI * prog.coerceIn(0f, 1f)).toFloat() * liftPx
                                    threadData[i * 6 + 2] = pawX
                                    threadData[i * 6 + 3] = pawY
                                    threadData[i * 6 + 4] = (prog * 0.9f).coerceIn(0f, 0.9f)
                                    threadData[i * 6 + 5] = sagMenu
                                }
                                threadData[i * 6] = dx
                                threadData[i * 6 + 1] = dy - p.height / 2f + ballCenterFromTop
                                p.place((dx - p.width / 2f).roundToInt(), (dy - p.height / 2f).roundToInt())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

@Composable
private fun SpeechBubble(
    say: AssistantSay,
    onRight: Boolean,
    lowerHalf: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = kindColor(say.kind, primary)
    val big = 18.dp
    val small = 6.dp
    val shape = AbsoluteRoundedCornerShape(
        topLeft = if (!lowerHalf && !onRight) small else big,
        topRight = if (!lowerHalf && onRight) small else big,
        bottomRight = if (lowerHalf && onRight) small else big,
        bottomLeft = if (lowerHalf && !onRight) small else big
    )
    Rtl {
        Surface(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(shape)
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shape = shape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = kindIcon(say.kind),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = say.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** یه گلوله‌ی کاموا که آیکن و اسم هر گزینه روشه */
@Composable
private fun YarnButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    progress: () -> Float,
    spin: () -> Float,
    bob: () -> Float,
    flip: Float,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                val p = progress()
                alpha = (p * 3f).coerceIn(0f, 1f)
                val s = 0.35f + 0.65f * p
                scaleX = s
                scaleY = s
                val wave = (bob() - 0.5f) * 2f
                translationY = wave * 3.dp.toPx() * flip
                rotationZ = wave * 4f * flip
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .drawBehind {
                    val r = size.minDimension / 2f - 2.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawYarnBall(c, r, tint, spin() + (bob() - 0.5f) * 24f)
                    // سایه‌ی نرم پشت آیکن تا همیشه خوانا باشه
                    drawCircle(Color.Black.copy(alpha = 0.28f), r * 0.46f, c)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Rtl {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.45f)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
