package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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

// ───────────────────────── آیکن دکتر طاعون: کلاه و سر جدا از هم ─────────────────────────

private val DoctorHatIcon: ImageVector by lazy {
    ImageVector.Builder("DoctorHat", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 8f)
            horizontalLineTo(20f)
            moveTo(7.5f, 8f)
            verticalLineTo(5.5f)
            quadTo(7.5f, 3.5f, 9.5f, 3.5f)
            horizontalLineTo(14.5f)
            quadTo(16.5f, 3.5f, 16.5f, 5.5f)
            verticalLineTo(8f)
        }
    }.build()
}

private val DoctorHeadIcon: ImageVector by lazy {
    ImageVector.Builder("DoctorHead", 24.dp, 24.dp, 24f, 24f).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8.5f, 8f)
            verticalLineTo(11f)
            quadTo(8.5f, 12f, 7.5f, 12.8f)
            lineTo(3f, 17.5f)
            lineTo(9.5f, 16.3f)
            quadTo(11.5f, 18f, 13.5f, 17.8f)
            quadTo(16.5f, 17f, 16.5f, 13.5f)
            verticalLineTo(8f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.5f, 11.5f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, 2.2f, 0f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, -2.2f, 0f)
            close()
        }
    }.build()
}

// ───────────────────────── حرکت‌ها ─────────────────────────

private class AvatarMotion {
    val offX = Animatable(0f)
    val offY = Animatable(0f)
    val hideShift = Animatable(0f)
    val tilt = Animatable(0f)
    val hatAngle = Animatable(0f)
    val hatX = Animatable(0f)
    val hatY = Animatable(0f)
    val hop = Animatable(0f)

    // کلاه وقتی از سر جدا شده (مختصات روی صفحه)
    val wx = Animatable(0f)
    val wy = Animatable(0f)
    val wAngle = Animatable(0f)

    // پیشرفت باز شدن چهار بطری منو
    val menuProg = List(3) { Animatable(0f) }

    var hatDown = false
    var hatJob: Job? = null
    var retrieveJob: Job? = null
    var busy = false
    var ignoreDrag = false
    var fastCount = 0
    var lastLean = 0f

    suspend fun lean(target: Float) = coroutineScope {
        launch { tilt.animateTo(target, spring(0.8f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(-target * 1.9f, spring(0.55f, Spring.StiffnessVeryLow)) }
        launch { hatY.animateTo(-abs(target) * 0.3f, spring(0.6f, Spring.StiffnessLow)) }
    }

    suspend fun relax() = coroutineScope {
        launch { tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(0f, spring(0.28f, Spring.StiffnessVeryLow)) }
        launch { hatY.animateTo(0f, spring(0.4f, Spring.StiffnessLow)) }
    }

    suspend fun dropHat(dir: Float, sidePx: Float, fallPx: Float) = coroutineScope {
        launch { hatX.animateTo(dir * sidePx, spring(0.55f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(dir * 68f, spring(0.45f, Spring.StiffnessLow)) }
        launch { hatY.animateTo(fallPx, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    suspend fun pickUpHat(hopPx: Float) = coroutineScope {
        launch {
            hop.animateTo(-hopPx, spring(0.5f, Spring.StiffnessMedium))
            hop.animateTo(0f, spring(0.45f, Spring.StiffnessLow))
        }
        launch { hatX.animateTo(0f, spring(0.7f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(0f, spring(0.5f, Spring.StiffnessLow)) }
        launch { hatY.animateTo(0f, spring(0.55f, Spring.StiffnessLow)) }
    }
}

// ───────────────────────── جمله‌ها ─────────────────────────

private fun String.stripEmoji(): String =
    replace(Regex("[\\p{So}\\uFE0F\\u200D]"), "").replace(Regex("\\s+"), " ").trim()

private object AssistantPhrases {
    private val general = listOf(
        "سلام! اگه بازی خاصی تو ذهنته، از دکمه‌ی جستجو اسمش رو بزن 🔍",
        "خسته نشدی؟ یه نفس عمیق بکش، بعد برگرد سراغ دانلود 😄",
        "هر وقت خواستی با من چت کن؛ نسخه‌ی بازی هم می‌پیچم!",
        "فضای گوشیت پره؟ قبل از نصب بازی‌های بزرگ یه نگاهی بهش بنداز.",
        "اگه نصب نشد، گزینه‌ی «نصب از منابع ناشناس» رو چک کن.",
        "منو بگیر و هر جا خواستی ببر؛ ناراحت نمی‌شم 😉 فقط خیلی تند نبر، کلاهم می‌افته!",
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
        "تنها نیستی؛ من همیشه همین گوشه منتظرم 🩺",
        "حالت شب و روز رو هم می‌تونی از تنظیمات عوض کنی."
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

// ───────────────────────── رسم بطری معجون ─────────────────────────

private fun mix(a: Offset, b: Offset, f: Float) = Offset(a.x + (b.x - a.x) * f, a.y + (b.y - a.y) * f)

private fun DrawScope.drawPotion(center: Offset, size: Float, color: Color, alpha: Float) {
    if (alpha <= 0.01f) return
    val strokeW = size * 0.07f
    val bodyR = size * 0.34f
    val bodyC = Offset(center.x, center.y + size * 0.14f)
    val neckW = size * 0.24f
    val neckTop = center.y - size * 0.34f
    val neckBottom = bodyC.y - bodyR * 0.75f

    // مایع داخل بطری
    drawCircle(color.copy(alpha = 0.30f * alpha), bodyR, bodyC)
    drawRoundRect(
        color.copy(alpha = 0.14f * alpha),
        topLeft = Offset(center.x - neckW / 2f, neckTop),
        size = Size(neckW, neckBottom - neckTop),
        cornerRadius = CornerRadius(neckW * 0.2f)
    )
    // بدنه و گردن
    drawCircle(color.copy(alpha = alpha), bodyR, bodyC, style = Stroke(width = strokeW))
    drawRoundRect(
        color.copy(alpha = alpha),
        topLeft = Offset(center.x - neckW / 2f, neckTop),
        size = Size(neckW, neckBottom - neckTop),
        cornerRadius = CornerRadius(neckW * 0.2f),
        style = Stroke(width = strokeW)
    )
    // درپوش
    drawRoundRect(
        color.copy(alpha = alpha),
        topLeft = Offset(center.x - neckW * 0.7f, neckTop - size * 0.09f),
        size = Size(neckW * 1.4f, size * 0.1f),
        cornerRadius = CornerRadius(size * 0.03f)
    )
    // برق شیشه
    drawCircle(Color.White.copy(alpha = 0.55f * alpha), size * 0.045f, Offset(bodyC.x - bodyR * 0.45f, bodyC.y - bodyR * 0.35f))
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
    var hatDetached by remember { mutableStateOf(false) }
    var bubbleSay by remember { mutableStateOf<AssistantSay?>(null) }
    var lastSay by remember { mutableStateOf(AssistantSay("")) }
    var activity by remember { mutableIntStateOf(0) }

    // ترتیب: چت، راهنما، دانلودها، جستجو  ←  زاویه‌ی هر بطری نسبت به جهت «داخل صفحه»
    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با دکتر", onChatClick),
        Triple(Icons.Outlined.Download, "دانلودها", onDownloadsClick),
        Triple(Icons.Outlined.Search, "جستجو", onSearchClick)
    )
    // رنگ مایع هر بطری (آیکن‌ها با رنگ متن می‌شینن و همیشه خوانان)
    val potionColors = listOf(Color(0xFF4FC3F7), Color(0xFF66BB6A), Color(0xFFFFB300))
    val angles = listOf(0f, 62f, -62f)

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val sizePx = with(density) { AvatarSize.toPx() }
            val marginPx = with(density) { EdgeMargin.toPx() }
            val barPx = with(density) { bottomBarHeight.toPx() }
            val barVisible = bottomBarHeight > 0.dp
            val topInset = WindowInsets.statusBars.getTop(density).toFloat()
            val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()

            val topLimit = topInset + marginPx / 2f
            val bottomLimit = (hPx - bottomInset - barPx - sizePx - with(density) { 6.dp.toPx() })
                .coerceAtLeast(topLimit)

            val hopPx = with(density) { 8.dp.toPx() }
            val sidePx = with(density) { 46.dp.toPx() }
            val fallPx = with(density) { 42.dp.toPx() }
            val iconPx = with(density) { 34.dp.toPx() }
            val iconInsetPx = with(density) { 13.dp.toPx() }

            fun dropHatNow() {
                motion.hatJob?.cancel()
                motion.hatJob = scope.launch {
                    motion.hatDown = true
                    motion.dropHat(if (onRight) -1f else 1f, sidePx, fallPx)
                    delay(1500)
                    motion.pickUpHat(hopPx)
                    motion.hatDown = false
                }
            }

            fun pickUpNow() {
                if (!motion.hatDown) return
                motion.hatJob?.cancel()
                motion.hatJob = scope.launch {
                    motion.pickUpHat(hopPx)
                    motion.hatDown = false
                }
            }

            /**
             * حرکت خیلی تند: کلاه می‌پره، می‌افته روی نوار پایینی (یا پایین صفحه)،
             * بعد دکتر می‌دوه و برش می‌داره و برمی‌گرده کنار صفحه.
             */
            fun flingHat(dirX: Float) {
                if (motion.busy) return
                motion.hatJob?.cancel()
                motion.hatDown = false
                motion.busy = true
                motion.retrieveJob = scope.launch {
                    expanded = false
                    bubbleSay = null

                    val startX = motion.offX.value + motion.hideShift.value + iconInsetPx
                    val startY = motion.offY.value + iconInsetPx
                    motion.wx.snapTo(startX)
                    motion.wy.snapTo(startY)
                    motion.wAngle.snapTo(motion.hatAngle.value)
                    motion.hatX.snapTo(0f)
                    motion.hatY.snapTo(0f)
                    motion.hatAngle.snapTo(0f)
                    hatDetached = true

                    // کف: روی نوار پایینی، یا نزدیک پایین صفحه
                    val floorY = if (barVisible) {
                        hPx - bottomInset - barPx - iconPx * 0.33f
                    } else {
                        hPx - bottomInset - with(density) { 30.dp.toPx() }
                    }
                    val flyX = (70..150).random() * density.density
                    val targetX = (startX + dirX * flyX).coerceIn(marginPx, wPx - iconPx - marginPx)
                    val up = with(density) { 50.dp.toPx() }
                    val bounce1 = with(density) { 16.dp.toPx() }
                    val bounce2 = with(density) { 6.dp.toPx() }

                    coroutineScope {
                        launch { motion.wx.animateTo(targetX, tween(950, easing = LinearOutSlowInEasing)) }
                        launch {
                            motion.wAngle.animateTo(
                                motion.wAngle.value + dirX * 540f,
                                tween(950, easing = LinearOutSlowInEasing)
                            )
                        }
                        launch {
                            motion.wy.animateTo((startY - up).coerceAtLeast(0f), tween(260, easing = LinearOutSlowInEasing))
                            motion.wy.animateTo(floorY, tween(620, easing = FastOutLinearInEasing))
                            motion.wy.animateTo(floorY - bounce1, tween(150, easing = LinearOutSlowInEasing))
                            motion.wy.animateTo(floorY, tween(150, easing = FastOutLinearInEasing))
                            motion.wy.animateTo(floorY - bounce2, tween(90, easing = LinearOutSlowInEasing))
                            motion.wy.animateTo(floorY, tween(90, easing = FastOutLinearInEasing))
                        }
                    }

                    delay(500)
                    while (dragging) delay(60)

                    // دکتر می‌دوه کنار کلاه
                    hidden = false
                    val hatCenterX = motion.wx.value + iconPx / 2f
                    val myCenterX = motion.offX.value + sizePx / 2f
                    val runDir = if (hatCenterX >= myCenterX) 1f else -1f
                    val standX = (hatCenterX - sizePx / 2f - runDir * sizePx * 0.55f).coerceIn(0f, wPx - sizePx)
                    coroutineScope {
                        launch { motion.tilt.animateTo(runDir * 12f, spring(0.6f, Spring.StiffnessMedium)) }
                        launch { motion.offX.animateTo(standX, spring(0.9f, Spring.StiffnessLow)) }
                        launch { motion.offY.animateTo(bottomLimit, spring(0.8f, Spring.StiffnessMediumLow)) }
                    }
                    launch { motion.tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow)) }

                    // کلاه رو برمی‌داره
                    val ax = motion.offX.value
                    val ay = motion.offY.value
                    var ang = motion.wAngle.value % 360f
                    if (ang > 180f) ang -= 360f
                    if (ang < -180f) ang += 360f
                    motion.hatX.snapTo(motion.wx.value - (ax + iconInsetPx))
                    motion.hatY.snapTo(motion.wy.value - (ay + iconInsetPx))
                    motion.hatAngle.snapTo(ang)
                    hatDetached = false
                    motion.pickUpHat(hopPx)
                    delay(300)

                    // برمی‌گرده کنار صفحه
                    val right = motion.offX.value + sizePx / 2f > wPx / 2f
                    onRight = right
                    lowerHalf = motion.offY.value + sizePx / 2f > hPx / 2f
                    motion.offX.animateTo(
                        if (right) wPx - sizePx - marginPx else marginPx,
                        spring(0.65f, Spring.StiffnessLow)
                    )
                    motion.busy = false
                    activity++
                }
            }

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
                if (ready && !motion.busy) {
                    val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                    if (ty != motion.offY.value) {
                        motion.offY.animateTo(ty, spring(0.8f, Spring.StiffnessLow))
                    }
                    lowerHalf = ty + sizePx / 2f > hPx / 2f
                }
            }
            LaunchedEffect(ready, wPx) {
                if (ready && !motion.busy) {
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
                if (ready && visible && !expanded && bubbleSay == null && !dragging && !hidden && !motion.busy) {
                    delay(12_000)
                    hidden = true
                }
            }

            // بطری‌های منو: یکی‌یکی بیرون می‌پرن
            LaunchedEffect(expanded) {
                if (expanded) showMenu = true
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

            // خبرهای فوری (مثلاً تموم شدن دانلود)
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                AssistantBus.events.collect { say ->
                    if (visibleNow && !dragging && !motion.busy) {
                        expanded = false
                        speak(say, 9_000)
                    }
                }
            }

            // هر ~۳۰ ثانیه یه جمله
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                delay(7_000)
                while (true) {
                    if (visibleNow && !expanded && !dragging && !motion.busy && bubbleSay == null) {
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
            val speakAmt by animateFloatAsState(
                targetValue = if (bubbleSay != null) 1f else 0f,
                animationSpec = tween(300),
                label = "speak"
            )
            val upW by animateFloatAsState(
                targetValue = if (dragging || hatDetached) 1f else 0f,
                animationSpec = spring(0.6f, Spring.StiffnessLow),
                label = "hands_up"
            )
            val bobPx = with(density) { 4.dp.toPx() }
            val talkPx = with(density) { 5.dp.toPx() }
            val jellyPx = with(density) { 7.dp.toPx() }

            // پرش نرم و ژله‌ای وقتی دکتر حرف می‌زنه
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

                        // ── بطری‌های معجون (منو) ──
                        if (showMenu) {
                            items.forEachIndexed { i, (icon, label, action) ->
                                Box(modifier = Modifier.layoutId("item$i")) {
                                    PotionButton(
                                        icon = icon,
                                        label = label,
                                        tint = potionColors[i],
                                        progress = { motion.menuProg[i].value },
                                        bob = { bob },
                                        flip = if (i % 2 == 0) 1f else -1f,
                                        onClick = {
                                            if (expanded) {
                                                expanded = false
                                                activity++
                                                action()
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // ── خود دکتر طاعون ──
                        Box(
                            modifier = Modifier
                                .layoutId("avatar")
                                .size(AvatarSize)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            activity++
                                            pickUpNow()
                                            if (hidden) {
                                                hidden = false
                                            } else if (!motion.busy) {
                                                bubbleSay = null
                                                expanded = !expanded
                                            }
                                        }
                                    )
                                }
                                .pointerInput(wPx, hPx, topLimit, bottomLimit, barVisible) {
                                    detectDragGestures(
                                        onDragStart = {
                                            motion.ignoreDrag = motion.busy
                                            if (!motion.ignoreDrag) {
                                                dragging = true
                                                expanded = false
                                                bubbleSay = null
                                                hidden = false
                                                activity++
                                                motion.fastCount = 0
                                                pickUpNow()
                                            }
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            if (motion.ignoreDrag) return@detectDragGestures
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

                                            // سرعت: چند رویداد پشت‌سرهم خیلی تند ← کلاه می‌پره
                                            val dt = (change.uptimeMillis - change.previousUptimeMillis).coerceAtLeast(1L)
                                            val speedDpMs = drag.getDistance() / dt / density.density
                                            if (speedDpMs > 2.5f) motion.fastCount++ else motion.fastCount = 0
                                            if (motion.fastCount >= 3 && !motion.busy) {
                                                flingHat(if (drag.x >= 0f) 1f else -1f)
                                            }
                                        },
                                        onDragEnd = {
                                            if (motion.ignoreDrag) {
                                                motion.ignoreDrag = false
                                                return@detectDragGestures
                                            }
                                            dragging = false
                                            activity++
                                            motion.lastLean = 0f
                                            if (motion.busy) {
                                                // برنامه‌ی «برداشتن کلاه» خودش جای دکتر رو مرتب می‌کنه
                                                scope.launch { motion.tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow)) }
                                                return@detectDragGestures
                                            }
                                            scope.launch {
                                                launch { motion.relax() }
                                                val right = motion.offX.value + sizePx / 2f > wPx / 2f
                                                onRight = right
                                                val tx = if (right) wPx - sizePx - marginPx else marginPx
                                                val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                                                lowerHalf = ty + sizePx / 2f > hPx / 2f
                                                val touchedBar = barVisible &&
                                                    ty >= bottomLimit - with(density) { 14.dp.toPx() }
                                                launch { motion.offX.animateTo(tx, spring(0.65f, Spring.StiffnessLow)) }
                                                launch { motion.offY.animateTo(ty, spring(0.75f, Spring.StiffnessMediumLow)) }
                                                if (touchedBar) dropHatNow()
                                            }
                                        },
                                        onDragCancel = {
                                            if (motion.ignoreDrag) {
                                                motion.ignoreDrag = false
                                                return@detectDragGestures
                                            }
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
                                        translationY = bob * bobPx - jelly.value * jellyPx + motion.hop.value
                                        scaleY = 1f + 0.12f * jelly.value
                                        scaleX = 1f - 0.08f * jelly.value
                                    },
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
                                Icon(
                                    imageVector = DoctorHeadIcon,
                                    contentDescription = "دستیار",
                                    tint = primary,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer { translationY = -6.dp.toPx() }
                                )

                                // ── دست‌ها (و بطری معجون موقع حرف زدن) ──
                                Canvas(modifier = Modifier.matchParentSize()) {
                                    val u = 1.dp.toPx()
                                    val offerRight = !onRight
                                    val shoulderL = Offset(24.3f * u, 41.5f * u)
                                    val shoulderR = Offset(35.7f * u, 41.5f * u)
                                    val sway = (bob - 0.5f) * 2f * 1.2f * u
                                    val w = (talk - 0.5f) * 2f

                                    // بدنه (ردا) زیر سر؛ دست‌ها از کنارش بیرون میان و به سر نمی‌چسبن
                                    val robe = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(25.5f * u, 37f * u)
                                        lineTo(34.5f * u, 37f * u)
                                        lineTo(39f * u, 54f * u)
                                        lineTo(21f * u, 54f * u)
                                        close()
                                    }
                                    drawPath(robe, surfaceColor)
                                    drawPath(
                                        robe, primary,
                                        style = Stroke(width = 1.6f * u, join = StrokeJoin.Round)
                                    )

                                    // جیب ردا
                                    drawRoundRect(
                                        color = primary.copy(alpha = 0.85f),
                                        topLeft = Offset(27.5f * u, 45f * u),
                                        size = Size(7.5f * u, 6.5f * u),
                                        cornerRadius = CornerRadius(1.5f * u),
                                        style = Stroke(width = 1.2f * u)
                                    )
                                    // هر بار که بطری‌ای از جیب درمیاد، دست راست می‌ره سراغ جیب
                                    var reach = 0f
                                    motion.menuProg.forEach { p ->
                                        val v = p.value
                                        if (v > 0f && v < 0.45f) {
                                            val r = sin(Math.PI * v / 0.45).toFloat()
                                            if (r > reach) reach = r
                                        }
                                    }
                                    val pocketHand = Offset(33f * u, 47.5f * u)

                                    val restL = Offset(16f * u, 50f * u + sway)
                                    val restR = Offset(44f * u, 50f * u - sway)
                                    val offerPoint = Offset(
                                        (if (offerRight) 51f else 9f) * u,
                                        (37f - 1.5f * talk) * u
                                    )
                                    val upL = Offset(9f * u, (31f + w * 3f) * u)
                                    val upR = Offset(51f * u, (31f - w * 3f) * u)

                                    var handL = mix(restL, if (!offerRight) offerPoint else restL, speakAmt)
                                    var handR = mix(restR, if (offerRight) offerPoint else restR, speakAmt)
                                    handR = mix(handR, pocketHand, reach)
                                    handL = mix(handL, upL, upW)
                                    handR = mix(handR, upR, upW)

                                    // بطری توی دستِ سمت کاربر
                                    val offerHand = if (offerRight) handR else handL
                                    drawPotion(
                                        center = Offset(
                                            offerHand.x + (if (offerRight) 4.5f else -4.5f) * u,
                                            offerHand.y - 8f * u
                                        ),
                                        size = 16f * u,
                                        color = primary,
                                        alpha = speakAmt * (1f - upW)
                                    )

                                    fun arm(shoulder: Offset, hand: Offset) {
                                        drawLine(
                                            color = primary,
                                            start = shoulder,
                                            end = hand,
                                            strokeWidth = 2.2f * u,
                                            cap = StrokeCap.Round
                                        )
                                        drawCircle(surfaceColor, 2.7f * u, hand)
                                        drawCircle(primary, 2.7f * u, hand, style = Stroke(width = 1.6f * u))
                                    }
                                    arm(shoulderL, handL)
                                    arm(shoulderR, handR)
                                }

                                Icon(
                                    imageVector = DoctorHatIcon,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer {
                                            alpha = if (hatDetached) 0f else 1f
                                            transformOrigin = TransformOrigin(0.5f, 0.28f)
                                            translationX = motion.hatX.value
                                            translationY = motion.hatY.value - 6.dp.toPx() - jelly.value * 2f * density.density
                                            rotationZ = motion.hatAngle.value
                                        }
                                )
                            }
                        }

                        // ── کلاهِ افتاده روی نوار پایینی ──
                        if (hatDetached) {
                            Icon(
                                imageVector = DoctorHatIcon,
                                contentDescription = null,
                                tint = primary,
                                modifier = Modifier
                                    .layoutId("hat")
                                    .size(34.dp)
                                    .graphicsLayer { rotationZ = motion.wAngle.value }
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val avatarM = measurables.first { it.layoutId == "avatar" }
                    val extrasM = measurables.first { it.layoutId == "extras" }
                    val hatM = measurables.firstOrNull { it.layoutId == "hat" }
                    val avatarSizeInt = sizePx.roundToInt()
                    val marginInt = marginPx.roundToInt()
                    val gapInt = with(density) { 10.dp.roundToPx() }
                    val maxExtrasW = min(
                        constraints.maxWidth - 2 * marginInt,
                        with(density) { 270.dp.roundToPx() }
                    ).coerceAtLeast(0)
                    val itemMaxW = with(density) { 130.dp.roundToPx() }

                    val avatarP = avatarM.measure(Constraints.fixed(avatarSizeInt, avatarSizeInt))
                    val extrasP = extrasM.measure(Constraints(maxWidth = maxExtrasW))
                    val hatP = hatM?.measure(Constraints())
                    val itemPs = List(3) { i ->
                        measurables.firstOrNull { it.layoutId == "item$i" }
                            ?.measure(Constraints(maxWidth = itemMaxW))
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val baseX = motion.offX.value.roundToInt()
                        val ay = motion.offY.value.roundToInt()

                        // بطری‌ها نسبت به جای «عادی» دکتر دور او پخش می‌شن
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

                        // بطری‌ها از جیبِ ردا شروع می‌کنن، کمی بالا میان و بعد می‌رن سرجاشون
                        val pocketX = baseX + with(density) { 31.25.dp.toPx() }
                        val pocketY = ay + with(density) { 48.25.dp.toPx() }
                        val liftPx = with(density) { 24.dp.toPx() }
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
                                val prog = motion.menuProg[i].value
                                val px = pocketX + (tx - pocketX) * prog
                                val py = pocketY + (ty - pocketY) * prog - sin(Math.PI * prog.coerceIn(0f, 1f)).toFloat() * liftPx
                                p.place((px - p.width / 2f).roundToInt(), (py - p.height / 2f).roundToInt())
                            }
                        }

                        hatP?.place(motion.wx.value.roundToInt(), motion.wy.value.roundToInt())
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

/** یه بطری معجون که آیکن و اسم هر گزینه توشه؛ انگار جنس‌های دکتره */
@Composable
private fun PotionButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    progress: () -> Float,
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
                    drawPotion(
                        center = Offset(size.width / 2f, size.height / 2f),
                        size = size.minDimension,
                        color = tint,
                        alpha = 1f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = 0.dp)
                    .graphicsLayer { translationY = 7.dp.toPx() }
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
