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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private val AvatarSize = 60.dp
private val EdgeMargin = 12.dp
private val AvatarShape = RoundedCornerShape(24.dp)

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
            moveTo(10f, 18.3f)
            lineTo(7.5f, 21f)
            moveTo(15f, 18f)
            lineTo(17.5f, 21f)
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

    var hatDown = false
    var hatJob: Job? = null
    var lastLean = 0f

    /** موقع کشیدن: بدن کمی کج می‌شود و کلاه با تأخیر، نرم و خلاف جهت کج می‌شود */
    suspend fun lean(target: Float) = coroutineScope {
        launch { tilt.animateTo(target, spring(0.8f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(-target * 1.9f, spring(0.55f, Spring.StiffnessVeryLow)) }
        launch { hatY.animateTo(-abs(target) * 0.3f, spring(0.6f, Spring.StiffnessLow)) }
    }

    /** بعد از رها کردن: با چند نوسان آرام صاف می‌شود */
    suspend fun relax() = coroutineScope {
        launch { tilt.animateTo(0f, spring(0.35f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(0f, spring(0.28f, Spring.StiffnessVeryLow)) }
        launch { hatY.animateTo(0f, spring(0.4f, Spring.StiffnessLow)) }
    }

    /** کلاه می‌افتد روی نوار پایینی */
    suspend fun dropHat(dir: Float, sidePx: Float, fallPx: Float) = coroutineScope {
        launch { hatX.animateTo(dir * sidePx, spring(0.55f, Spring.StiffnessLow)) }
        launch { hatAngle.animateTo(dir * 68f, spring(0.45f, Spring.StiffnessLow)) }
        launch { hatY.animateTo(fallPx, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) }
    }

    /** دکتر یه جهش کوچیک می‌کنه و کلاه رو برمی‌داره */
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

// ───────────────────────── جمله‌های از پیش تعیین‌شده ─────────────────────────

private object AssistantPhrases {
    private val general = listOf(
        "سلام! اگه بازی خاصی تو ذهنته، از دکمه‌ی جستجو اسمش رو بزن 🔍",
        "خسته نشدی؟ یه نفس عمیق بکش، بعد برگرد سراغ دانلود 😄",
        "هر وقت خواستی با من چت کن؛ نسخه‌ی بازی هم می‌پیچم!",
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
        "تنها نیستی؛ من همیشه همین گوشه منتظرم 🩺",
        "حالت شب و روز رو هم می‌تونی از تنظیمات عوض کنی."
    )

    private val games = listOf(
        "بازی‌ها دسته‌بندی دارن؛ اگه سبک خاصی دوست داری دسته‌ها رو ببین.",
        "قبل از دانلود، حجم بازی رو با فضای گوشیت مقایسه کن.",
        "بازی آفلاین می‌خوای یا آنلاین؟ از من بپرس!",
        "بعضی بازی‌ها فایل دیتای جدا دارن؛ توضیحات پست رو حتماً بخون.",
        "بازی نصب شد ولی باز نمی‌شه؟ فایل دیتا رو چک کن.",
        "اینجا کلی بازی هست؛ اگه گیج شدی بگو یکی برات انتخاب کنم 🎲",
        "بازی بزرگ؟ اول دانلود، بعد نصب؛ صبور باش.",
        "یادت نره استراحت هم بکنی، چشمات حق دارن 👀"
    )

    private val apps = listOf(
        "دنبال یه برنامه‌ی خاص می‌گردی؟ جستجو سریع‌ترین راهه.",
        "قبل از نصب، نسخه‌ی اندرویدی که برنامه لازم داره رو چک کن.",
        "برای هر کاری برنامه هست؛ بگو چی لازم داری تا دنبالش بگردم.",
        "برنامه‌هات رو به‌روز نگه دار، امنیتشون بیشتر می‌شه.",
        "نصب از منبع ناشناس؟ یه بار اجازه‌اش رو بده و تمام.",
        "برنامه‌ی جدید نصب می‌کنی؟ دسترسی‌هاش رو یه نگاه بنداز 🔐"
    )

    private val blog = listOf(
        "اینجا مقاله‌های آموزشیه؛ جواب خیلی از مشکلات نصب و دانلود هم توشه.",
        "وبلاگ رو ورق بزن، شاید چیز جالبی پیدا کنی 📰",
        "اخبار تازه‌ی دنیای بازی و برنامه همین‌جاست.",
        "سؤالی داشتی؟ قبل از گشتن بین مقاله‌ها از من هم بپرس."
    )

    private val downloads = listOf(
        "دانلودهات اینجاست؛ هر وقت خواستی نصبشون کن.",
        "دانلود کامل شد؟ دکمه‌ی «نصب» رو بزن.",
        "فایل‌های نصب‌شده رو از اینجا پاک کن تا حافظه خالی بشه.",
        "دانلود گیر کرده؟ پاکش کن و دوباره شروع کن.",
        "دانلود کند شد؟ اینترنت رو عوض کن."
    )

    private val search = listOf(
        "اسم بازی رو فارسی یا انگلیسی امتحان کن؛ گاهی یکیش بهتر جواب می‌ده.",
        "نتیجه‌ای نبود؟ یه کلمه‌ی کوتاه‌تر بنویس.",
        "املا رو چک کن، گاهی یه حرف همه‌چی رو عوض می‌کنه 😅",
        "جستجوهای قبلیت همین‌جا ذخیره می‌شه."
    )

    private val detail = listOf(
        "قبل از دانلود، توضیحات و نیازمندی‌ها رو بخون.",
        "نظرها رو هم ببین؛ تجربه‌ی بقیه کمکت می‌کنه.",
        "پایین صفحه مطالب مشابه هست؛ شاید بهتر باشه.",
        "دکمه‌ی دانلود آماده‌ست؛ فقط فضای گوشی رو چک کن."
    )

    private val recent = ArrayDeque<String>()

    fun next(route: String?): String {
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
        // جمله‌های مخصوص صفحه کمی بیشتر شانس دارن
        val pool = (general + extra + extra).filter { it !in recent }.ifEmpty { general }
        val pick = pool.random()
        recent.addLast(pick)
        while (recent.size > 10) recent.removeFirst()
        return pick
    }
}

// ───────────────────────── کامپوننت اصلی ─────────────────────────

/**
 * دستیار شناور برای کل اپ. باید روی کل صفحه (کنار Scaffold) قرار بگیرد.
 * bottomBarHeight: ارتفاع نوار پایینی وقتی دیده می‌شود (۰ وقتی نیست).
 */
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
    val motion = remember { AvatarMotion() }
    val routeNow by rememberUpdatedState(currentRoute)
    val visibleNow by rememberUpdatedState(visible)

    var ready by remember { mutableStateOf(false) }
    var onRight by remember { mutableStateOf(false) }
    var lowerHalf by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleText by remember { mutableStateOf<String?>(null) }
    var lastBubble by remember { mutableStateOf("") }
    var activity by remember { mutableIntStateOf(0) }

    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با دکتر", onChatClick),
        Triple(Icons.Outlined.Info, "راهنما", onHelpClick),
        Triple(Icons.Outlined.Download, "دانلودها", onDownloadsClick),
        Triple(Icons.Outlined.Search, "جستجو", onSearchClick)
    )

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

            // اگه نوار پایینی ظاهر/ناپدید شد یا صفحه عوض شد، جای دکتر رو داخل محدوده نگه دار
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

            // اگه ۱۲ ثانیه کاری نشد، بره قایم بشه
            LaunchedEffect(activity, expanded, bubbleText, dragging, hidden, ready, visible) {
                if (ready && visible && !expanded && bubbleText == null && !dragging && !hidden) {
                    delay(12_000)
                    hidden = true
                }
            }

            // هر ~۳۰ ثانیه یه جمله می‌گه
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                delay(7_000)
                while (true) {
                    if (visibleNow && !expanded && !dragging && bubbleText == null) {
                        val phrase = AssistantPhrases.next(routeNow)
                        hidden = false
                        lastBubble = phrase
                        bubbleText = phrase
                        delay(7_500)
                        bubbleText = null
                    }
                    delay((28_000L..38_000L).random())
                }
            }

            // انیمیشن‌های بی‌نهایت: شناور بودن و بالا-پایین شدن موقع حرف زدن
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
                targetValue = if (bubbleText != null) 1f else 0f,
                animationSpec = tween(300),
                label = "speak"
            )
            val bobPx = with(density) { 4.dp.toPx() }
            val talkPx = with(density) { 5.dp.toPx() }

            if (ready && visible) {
                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        // ── پیام و دکمه‌ها (همیشه داخل صفحه‌ی کاربر قرار می‌گیرن) ──
                        Box(modifier = Modifier.layoutId("extras")) {
                            Column(
                                horizontalAlignment = if (onRight) Alignment.End else Alignment.Start,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = bubbleText != null && !expanded,
                                    enter = fadeIn(tween(220)) + scaleIn(
                                        spring(0.6f, Spring.StiffnessMediumLow),
                                        initialScale = 0.7f,
                                        transformOrigin = TransformOrigin(if (onRight) 1f else 0f, if (lowerHalf) 1f else 0f)
                                    ),
                                    exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f)
                                ) {
                                    SpeechBubble(
                                        text = lastBubble,
                                        onRight = onRight,
                                        lowerHalf = lowerHalf,
                                        onDismiss = { bubbleText = null }
                                    )
                                }

                                val ordered = if (lowerHalf) items else items.reversed()
                                ordered.forEachIndexed { index, (icon, label, action) ->
                                    val enterOrder = if (lowerHalf) ordered.size - 1 - index else index
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = fadeIn(tween(180, delayMillis = enterOrder * 45)) +
                                            slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) {
                                                if (lowerHalf) it / 2 else -it / 2
                                            } +
                                            scaleIn(tween(220, delayMillis = enterOrder * 45), initialScale = 0.7f),
                                        exit = fadeOut(tween(120)) +
                                            slideOutVertically(tween(150)) { if (lowerHalf) it / 2 else -it / 2 } +
                                            scaleOut(tween(150), targetScale = 0.7f)
                                    ) {
                                        AssistantMenuItem(
                                            icon = icon,
                                            label = label,
                                            onClick = {
                                                expanded = false
                                                activity++
                                                action()
                                            }
                                        )
                                    }
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
                                            } else {
                                                bubbleText = null
                                                expanded = !expanded
                                            }
                                        }
                                    )
                                }
                                .pointerInput(wPx, hPx, topLimit, bottomLimit, barVisible) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = true
                                            expanded = false
                                            bubbleText = null
                                            hidden = false
                                            activity++
                                            pickUpNow()
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
                                                val touchedBar = barVisible &&
                                                    ty >= bottomLimit - with(density) { 14.dp.toPx() }
                                                launch { motion.offX.animateTo(tx, spring(0.65f, Spring.StiffnessLow)) }
                                                launch { motion.offY.animateTo(ty, spring(0.75f, Spring.StiffnessMediumLow)) }
                                                if (touchedBar) dropHatNow()
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
                                        translationY = bob * bobPx - talk * speakAmt * talkPx + motion.hop.value
                                        scaleY = 1f + 0.035f * talk * speakAmt
                                        scaleX = 1f - 0.02f * talk * speakAmt
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    shape = AvatarShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, primary),
                                    shadowElevation = 10.dp,
                                    content = {}
                                )
                                Icon(
                                    imageVector = DoctorHeadIcon,
                                    contentDescription = "دستیار",
                                    tint = primary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Icon(
                                    imageVector = DoctorHatIcon,
                                    contentDescription = null,
                                    tint = primary,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .graphicsLayer {
                                            transformOrigin = TransformOrigin(0.5f, 0.28f)
                                            translationX = motion.hatX.value
                                            translationY = motion.hatY.value - talk * speakAmt * 1.5f * density.density
                                            rotationZ = motion.hatAngle.value
                                        }
                                )
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val avatarM = measurables.first { it.layoutId == "avatar" }
                    val extrasM = measurables.first { it.layoutId == "extras" }
                    val avatarSizeInt = sizePx.roundToInt()
                    val marginInt = marginPx.roundToInt()
                    val gapInt = with(density) { 10.dp.roundToPx() }
                    val maxExtrasW = min(
                        constraints.maxWidth - 2 * marginInt,
                        with(density) { 250.dp.roundToPx() }
                    ).coerceAtLeast(0)

                    val avatarP = avatarM.measure(Constraints.fixed(avatarSizeInt, avatarSizeInt))
                    val extrasP = extrasM.measure(Constraints(maxWidth = maxExtrasW))

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        val baseX = motion.offX.value.roundToInt()
                        val ay = motion.offY.value.roundToInt()
                        avatarP.place(baseX + motion.hideShift.value.roundToInt(), ay)

                        // پیام و دکمه‌ها نسبت به جای «عادی» دکتر چیده می‌شن، پس همیشه توی صفحه‌ان
                        val rawX = if (onRight) baseX + avatarP.width - extrasP.width else baseX
                        val maxX = (constraints.maxWidth - marginInt - extrasP.width).coerceAtLeast(marginInt)
                        val ex = rawX.coerceIn(marginInt, maxX)

                        val rawY = if (lowerHalf) ay - extrasP.height - gapInt else ay + avatarP.height + gapInt
                        val minY = topInset.roundToInt()
                        val maxY = (constraints.maxHeight - bottomInset.roundToInt() - extrasP.height)
                            .coerceAtLeast(minY)
                        val ey = rawY.coerceIn(minY, maxY)

                        extrasP.place(ex, ey)
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
    text: String,
    onRight: Boolean,
    lowerHalf: Boolean,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val big = 18.dp
    val small = 6.dp
    // گوشه‌ی تیز بالن، سمت دکتر قرار می‌گیره
    val shape = AbsoluteRoundedCornerShape(
        topLeft = if (!lowerHalf && !onRight) small else big,
        topRight = if (!lowerHalf && onRight) small else big,
        bottomRight = if (lowerHalf && onRight) small else big,
        bottomLeft = if (lowerHalf && !onRight) small else big
    )
    Rtl {
        Surface(
            modifier = Modifier
                .widthIn(max = 230.dp)
                .clip(shape)
                .clickable { onDismiss() },
            color = MaterialTheme.colorScheme.surface,
            shape = shape,
            border = BorderStroke(1.dp, primary.copy(alpha = 0.6f)),
            shadowElevation = 6.dp
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
            )
        }
    }
}

@Composable
private fun AssistantMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Rtl {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = label, tint = primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
