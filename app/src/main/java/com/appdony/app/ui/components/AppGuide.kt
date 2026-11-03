package com.appdony.app.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.preferences.SettingsManager
import kotlinx.coroutines.delay

/**
 * وضعیت راهنما و جای واقعی بخش‌های رابط.
 * هر بخش (تب‌ها، دکمه‌ی هوش مصنوعی، جستجو، منو) با [guideTarget] جای خودش رو ثبت می‌کنه
 * تا دایره‌ی راهنما دقیقاً روی همون بخش بیفته، توی هر دو حالت کلاسیک و شیشه‌ای.
 */
object GuideState {
    /** تا وقتی راهنما بازه true می‌مونه؛ ربات و دستیارها توش حرف تصادفی نمی‌زنن */
    var active by mutableStateOf(false)
    val rects = mutableStateMapOf<String, Rect>()
}

fun Modifier.guideTarget(key: String): Modifier = onGloballyPositioned { c ->
    val r = c.boundsInRoot()
    if (GuideState.rects[key] != r) GuideState.rects[key] = r
}

private enum class GuideTarget {
    POPPY, BOTTOM_NAV, AI_BUTTON, TAB_GAMES, TAB_APPS, TAB_BLOG, TAB_DOWNLOADS, SEARCH, ACCOUNT,
    /** ناحیه‌ی محتوای صفحه؛ فقط وقتی کاربر روی تب می‌زنه و توضیح «داخلش» نشون داده می‌شه */
    CONTENT
}

private class GuideStep(
    val title: String,
    val icon: ImageVector,
    val text: String,
    /** توضیح داخلی؛ با لمس بعدی نشون داده می‌شه */
    val inner: String?,
    /** اگه true باشه، موقع توضیح داخلی دایره می‌ره روی محتوای صفحه */
    val innerContent: Boolean,
    val target: GuideTarget,
    val route: String? = null
)

private fun buildSteps(showPoppy: Boolean, fixedAi: Boolean): List<GuideStep> = buildList {
    if (showPoppy) {
        add(
            GuideStep(
                "من پاپی‌ام", Icons.Outlined.ChatBubbleOutline,
                "سلام! من دستیار اپدونی‌ام. می‌تونی منو بکشی و هر جای صفحه که خواستی بذاری.",
                "روی من که بزنی چند تا گزینه باز می‌شه؛ مثلاً چت با من، رفتن به دانلودها و جستجو. هر وقت لازم داشتی صدام کن.",
                false, GuideTarget.POPPY
            )
        )
    }
    add(
        GuideStep(
            "منوی پایین", Icons.Outlined.Dashboard,
            "با این منو بین چهار بخش اصلی جابه‌جا می‌شی: بازی‌ها، برنامه‌ها، وبلاگ و دانلودها.",
            null, false, GuideTarget.BOTTOM_NAV
        )
    )
    if (fixedAi) {
        add(
            GuideStep(
                "هوش مصنوعی", Icons.Outlined.ChatBubbleOutline,
                "این دکمه‌ی گرد وسط منوئه. با زدنش چت هوش مصنوعی اپدونی باز می‌شه.",
                null, false, GuideTarget.AI_BUTTON
            )
        )
    }
    add(
        GuideStep(
            "بازی‌ها", Icons.Outlined.SportsEsports,
            "اینجا جدیدترین بازی‌های اندروید رو پیدا می‌کنی، با لینک مستقیم.",
            "بازی‌ها دسته‌بندی شدن و هر دسته یه ردیف داره. با «مشاهده همه» کل دسته رو می‌بینی و با زدن روی هر بازی، جزئیات و دکمه‌ی دانلودش باز می‌شه.",
            true, GuideTarget.TAB_GAMES, "games"
        )
    )
    add(
        GuideStep(
            "برنامه‌ها", Icons.Outlined.Apps,
            "برنامه‌های کاربردی اندروید اینجاست.",
            "مثل بازی‌ها دسته‌بندی شدن. روی هر برنامه بزنی، نسخه، حجم و لینک‌های دانلودش رو می‌بینی.",
            true, GuideTarget.TAB_APPS, "apps"
        )
    )
    add(
        GuideStep(
            "وبلاگ", Icons.Outlined.Article,
            "مقاله‌ها و خبرهای تازه‌ی اپدونی اینجان.",
            "روی هر مطلب بزنی کامل باز می‌شه و فهرست مطالبش هم هست. اعلان وبلاگ رو هم از تنظیمات روشن یا خاموش می‌کنی.",
            true, GuideTarget.TAB_BLOG, "blog"
        )
    )
    add(
        GuideStep(
            "دانلودها", Icons.Outlined.Download,
            "همه‌ی دانلودهات اینجا جمع می‌شن.",
            "پیشرفت هر دانلود رو می‌بینی و فایل‌هات رو از همین‌جا مدیریت می‌کنی. اگه توی تنظیمات «فقط وای‌فای» رو بزنی، دانلود با اینترنت سیم‌کارت شروع نمی‌شه.",
            true, GuideTarget.TAB_DOWNLOADS, "downloads"
        )
    )
    add(
        GuideStep(
            "جستجو", Icons.Outlined.Search,
            "اسم هر بازی یا برنامه‌ای رو اینجا بنویس تا سریع پیداش کنی.",
            "نتیجه‌ها همین‌طور که می‌نویسی زیر نوار میان و جستجوهای قبلیت هم ذخیره می‌شه. از تنظیمات می‌تونی تاریخچه‌ش رو پاک کنی.",
            false, GuideTarget.SEARCH, "games"
        )
    )
    add(
        GuideStep(
            "منو", Icons.Outlined.Person,
            "این دکمه منوته. هر وقت خواستی از اینجا حساب بسازی یا وارد شی.",
            "توی منو: دانلودها، علاقه‌مندی‌ها، تنظیمات و درباره اپدونی هست. اگه حساب نداری، «ورود یا ثبت‌نام» هم بالای منوئه. کامنت گذاشتن و امتیاز دادن فقط با حساب فعاله.",
            false, GuideTarget.ACCOUNT
        )
    )
    add(
        GuideStep(
            "تنظیمات", Icons.Outlined.Settings,
            "تنظیمات رو هم از همین منو باز کن.",
            "تم تاریک یا روشن، رنگ و اندازه‌ی فونت، روشن و خاموش کردن من، دانلود فقط با وای‌فای، اعلان‌ها و پاک کردن کش و تاریخچه‌ها همه اونجاست.",
            false, GuideTarget.ACCOUNT
        )
    )
}

/** کل صفحه به‌جز سوراخ (محل هایلایت) */
private class HoleShape(private val hole: Rect, private val corner: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            if (hole.width > 0f && hole.height > 0f) {
                addRoundRect(RoundRect(hole, CornerRadius(corner)))
            }
        }
        return Outline.Generic(path)
    }
}

/**
 * راهنمای اولین ورود: بیرونِ دایره بلور و مات، داخلش شفاف.
 * پاپی با ابر پیام خودش (همون استایل حرف‌های همیشگی‌ش) سمت مخالفِ دایره توضیح می‌ده.
 * جای دایره‌ها از خودِ رابط خونده می‌شه (guideTarget)، پس همیشه دقیقاً روی همون بخشیه که توضیحش داده می‌شه.
 * هر لمسی، هرجای صفحه (بلور، دایره، ابر پیام)، متن بعدی رو می‌خونه.
 * sceneLayer باید کل رابط (نوار بالا، منوی پایین، پاپی و محتوا) رو ضبط کرده باشه.
 */
@Composable
fun AppGuide(
    sceneLayer: GraphicsLayer,
    showPoppyStep: Boolean,
    startRoute: String,
    onNavigate: (String) -> Unit,
    onFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val context = LocalContext.current
    val fixedAi by remember { SettingsManager(context) }.fixedAi.collectAsState(initial = true)
    val steps = remember(showPoppyStep, fixedAi) { buildSteps(showPoppyStep, fixedAi) }

    // تا راهنما بازه، ربات منو و دستیارها حرف تصادفی نمی‌زنن
    DisposableEffect(Unit) {
        GuideState.active = true
        onDispose { GuideState.active = false }
    }

    var step by remember { mutableIntStateOf(0) }
    var innerShown by remember(step) { mutableStateOf(false) }
    val current = steps[step]
    val last = step == steps.lastIndex
    val fullText = if (innerShown) (current.inner ?: current.text) else current.text
    val canExplain = current.inner != null && !innerShown

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450)) }

    // هر مرحله‌ی تب‌ها رو واقعاً باز می‌کنه
    LaunchedEffect(step) { current.route?.let { onNavigate(it) } }

    // تایپ شدن متن پاپی
    var shown by remember(step, innerShown) { mutableIntStateOf(0) }
    LaunchedEffect(step, innerShown) {
        shown = 0
        delay(if (innerShown) 120 else 300)
        while (shown < fullText.length) {
            delay(22)
            shown++
        }
    }
    val typing = shown < fullText.length

    // فقط دکمه‌ها و لمس صفحه می‌تونن راهنما رو جلو ببرن یا ببندن
    BackHandler(enabled = true) { }

    val close = {
        onNavigate(startRoute)
        onFinish()
    }

    // هر لمسی (روی بلور، روی دایره، روی ابر پیام) = خوندن بعدی: اول توضیح داخلی، بعد مرحله‌ی بعد
    val advance: () -> Unit = {
        if (canExplain) {
            innerShown = true
        } else if (last) {
            close()
        } else {
            step++
        }
    }
    val advanceNow by rememberUpdatedState(advance)

    val pulseT = rememberInfiniteTransition(label = "guide_pulse")
    val pulse by pulseT.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
    ) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        fun px(v: Dp): Float = with(density) { v.toPx() }

        val statusTop = WindowInsets.statusBars.getTop(density).toFloat()
        val navInset = WindowInsets.navigationBars.getBottom(density).toFloat()

        // x فیزیکی از روی فاصله‌ی «از ابتدای صفحه» (در RTL از راست)
        fun physX(fromStart: Float, width: Float): Float = if (rtl) w - fromStart - width else fromStart

        val barTop = statusTop + px(10.dp)
        val barH = px(44.dp)

        // جای واقعی هر بخش از خودِ رابط می‌آد؛ اگه هنوز ثبت نشده بود، یه جای تقریبی استفاده می‌شه
        fun reg(key: String): Rect? = GuideState.rects[key]?.takeIf { it.width > 0f && it.height > 0f }

        val navBottom = h - navInset - px(6.dp)
        val navTopFallback = navBottom - px(60.dp)

        fun tabRect(index: Int, route: String): Rect {
            reg("tab_$route")?.let { return it }
            val itemW = (w - px(8.dp)) / 4f
            val x = physX(px(4.dp) + itemW * index, itemW)
            return Rect(x, navTopFallback, x + itemW, navBottom)
        }

        fun rectFor(t: GuideTarget): Rect = when (t) {
            GuideTarget.POPPY -> {
                val size = px(AvatarSize)
                val margin = px(EdgeMargin)
                val topLimit = statusTop + margin / 2f
                val bottomLimit = (h - navInset - px(88.dp) - size - px(6.dp)).coerceAtLeast(topLimit)
                val y = (bottomLimit - px(24.dp)).coerceIn(topLimit, bottomLimit)
                Rect(margin, y, margin + size, y + size)
            }
            GuideTarget.BOTTOM_NAV -> {
                val nav = reg("nav") ?: Rect(px(4.dp), navTopFallback, w - px(4.dp), navBottom)
                val ai = if (fixedAi) reg("ai") else null
                if (ai == null) nav
                else Rect(
                    minOf(nav.left, ai.left), minOf(nav.top, ai.top),
                    maxOf(nav.right, ai.right), maxOf(nav.bottom, ai.bottom)
                )
            }
            GuideTarget.AI_BUTTON -> {
                val b = reg("ai") ?: Rect(Offset(w / 2f, navTopFallback), px(27.dp))
                // اسم «هوش مصنوعی» زیر دکمه‌ست؛ دایره هر دو رو می‌گیره
                Rect(b.left, b.top, b.right, b.bottom + px(20.dp))
            }
            GuideTarget.TAB_GAMES -> tabRect(0, "games")
            GuideTarget.TAB_APPS -> tabRect(1, "apps")
            GuideTarget.TAB_BLOG -> tabRect(2, "blog")
            GuideTarget.TAB_DOWNLOADS -> tabRect(3, "downloads")
            GuideTarget.SEARCH -> reg("search") ?: run {
                val width = w - px(78.dp)
                val x = physX(px(12.dp), width)
                Rect(x, barTop, x + width, barTop + barH)
            }
            GuideTarget.ACCOUNT -> reg("account") ?: run {
                val x = physX(w - px(56.dp), px(44.dp))
                Rect(x, barTop, x + px(44.dp), barTop + barH)
            }
            GuideTarget.CONTENT -> {
                // از زیر نوار بالا تا بالای منوی پایین؛ نیمه‌ی بالایش که ردیف‌های اول محتواست
                val top = (reg("search")?.bottom ?: (barTop + barH)) + px(12.dp)
                val bottom = (reg("nav")?.top ?: navTopFallback) - px(12.dp)
                val height = ((bottom - top) * 0.5f).coerceAtLeast(px(120.dp))
                Rect(px(8.dp), top, w - px(8.dp), top + height)
            }
        }

        // وقتی کاربر روی تب می‌زنه، دایره می‌ره روی محتوای همون بخش
        val activeTarget = if (innerShown && current.innerContent) GuideTarget.CONTENT else current.target
        val raw = rectFor(activeTarget)
        val padPx = px(8.dp)
        val targetHole: Rect
        val targetCorner: Float
        if (activeTarget == GuideTarget.CONTENT) {
            targetHole = raw
            targetCorner = px(28.dp)
        } else if (raw.width / raw.height < 2.2f) {
            // هدف‌های تقریباً مربعی دایره می‌شن
            val r = maxOf(raw.width, raw.height) / 2f + padPx
            targetHole = Rect(raw.center, r)
            targetCorner = r
        } else {
            // هدف‌های پهن (نوار جستجو، کل منوی پایین) کپسول می‌شن
            targetHole = raw.inflate(padPx)
            targetCorner = targetHole.height / 2f
        }

        val hole by animateRectAsState(targetHole, tween(450, easing = FastOutSlowInEasing), label = "hole")
        val corner by animateFloatAsState(targetCorner, tween(450, easing = FastOutSlowInEasing), label = "corner")

        val bg = scheme.background
        val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        val blurPx = px(22.dp)

        // ۱) کپی بلورشده‌ی کل صحنه، همه‌جا به‌جز داخل دایره
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HoleShape(hole, corner))
                .then(
                    if (canBlur) {
                        Modifier.graphicsLayer {
                            renderEffect = BlurEffect(blurPx, blurPx, TileMode.Clamp)
                        }
                    } else Modifier
                )
                .drawBehind { drawLayer(sceneLayer) }
        )

        // ۲) لایه‌ی مات روی بلور (اندروید ۱۱ و پایین‌تر بلور نداره؛ مات‌تر می‌شه)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HoleShape(hole, corner))
                .background(bg.copy(alpha = if (canBlur) 0.55f else 0.92f))
        )

        // ۳) حلقه‌ی دور دایره
        Canvas(modifier = Modifier.fillMaxSize()) {
            val tl = hole.topLeft
            drawRoundRect(
                color = primary.copy(alpha = 0.16f + 0.2f * (1f - pulse)),
                topLeft = tl,
                size = hole.size,
                cornerRadius = CornerRadius(corner),
                style = Stroke(width = (4f + 8f * pulse).dp.toPx())
            )
            drawRoundRect(
                color = primary,
                topLeft = tl,
                size = hole.size,
                cornerRadius = CornerRadius(corner),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // ۴) جلوی لمس اپ زیر راهنما رو می‌گیره. هر لمسی = خوندن بعدی
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { advanceNow() }
                }
        )

        // ۵) پاپی و ابر پیامش، سمت مخالفِ دایره
        val holeInLowerHalf = hole.center.y > h / 2f
        val topPad = with(density) { statusTop.toDp() } + 20.dp
        val bottomPad = with(density) { navInset.toDp() } + 24.dp

        Column(
            modifier = Modifier
                .align(if (holeInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(
                    top = if (holeInLowerHalf) topPad else 0.dp,
                    bottom = if (holeInLowerHalf) 0.dp else bottomPad
                )
                .padding(horizontal = 16.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
        ) {
            // پاپی سمت چپ، درست مثل جای همیشگی‌ش؛ ابر پیام زیرشه
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.fillMaxWidth().padding(start = 6.dp)) {
                    CatAvatar(sizeDp = 64, hasShadow = false, talking = typing)
                }
            }
            Spacer(Modifier.height(6.dp))
            GuideBubble(
                step = current,
                index = step,
                total = steps.size,
                fullText = fullText,
                shownText = fullText.take(shown),
                hint = when {
                    canExplain -> "هر جا بزنی، توضیح بیشتر نشون داده می‌شه"
                    last -> "هر جا بزنی، راهنما تموم می‌شه"
                    else -> "هر جا بزنی، می‌ره مرحله‌ی بعد"
                },
                onTap = advance,
                last = last,
                onNext = { if (last) close() else step++ },
                onSkip = { close() }
            )
        }
    }
}

/** ابر پیام، هم‌استایل حرف‌های همیشگی پاپی (گوشه‌ی کوچک سمت پاپی) */
@Composable
private fun GuideBubble(
    step: GuideStep,
    index: Int,
    total: Int,
    fullText: String,
    shownText: String,
    hint: String,
    onTap: () -> Unit,
    last: Boolean,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val big = 18.dp
    val small = 6.dp
    val shape = AbsoluteRoundedCornerShape(
        topLeft = small, topRight = big, bottomRight = big, bottomLeft = big
    )
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTap
        ),
        color = scheme.surface,
        shape = shape,
        border = BorderStroke(1.dp, primary.copy(alpha = 0.65f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(step.icon, contentDescription = null, tint = primary, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = step.title,
                    color = scheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${index + 1} از $total",
                    color = scheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            // متن کامل نامرئی جا رو نگه می‌داره تا ابر پیام موقع تایپ بالا و پایین نپره
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = fullText,
                    color = Color.Transparent,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = shownText,
                    color = scheme.onSurface,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = hint,
                color = primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!last) {
                    GuideButton(
                        text = "رد کردن",
                        container = scheme.error.copy(alpha = 0.14f),
                        content = scheme.error,
                        borderColor = scheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        onClick = onSkip
                    )
                }
                GuideButton(
                    text = if (last) "شروع کنیم" else "بعدی",
                    container = primary,
                    content = Color.White,
                    borderColor = null,
                    modifier = Modifier.weight(if (last) 1f else 1.4f),
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun GuideButton(
    text: String,
    container: Color,
    content: Color,
    borderColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(container)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = content, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
