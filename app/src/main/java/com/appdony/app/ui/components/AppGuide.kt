package com.appdony.app.ui.components

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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay

private enum class GuideTarget { POPPY, BOTTOM_NAV, TAB_GAMES, TAB_APPS, TAB_BLOG, TAB_DOWNLOADS, SEARCH, ACCOUNT }

private class GuideStep(
    val title: String,
    val text: String,
    val target: GuideTarget,
    val route: String? = null
)

private fun buildSteps(showPoppy: Boolean): List<GuideStep> = buildList {
    if (showPoppy) {
        add(
            GuideStep(
                "سلام، من پاپی‌ام",
                "من دستیار اپدونی‌ام. روی من بزن تا چت کنیم، سراغ دانلودها بریم یا چیزی رو جستجو کنیم. می‌تونی منو بکشی و هر جای صفحه که خواستی بذاری.",
                GuideTarget.POPPY
            )
        )
    }
    add(
        GuideStep(
            "منوی پایین",
            "از این منو بین بخش‌های اصلی اپ جابه‌جا می‌شی: بازی‌ها، برنامه‌ها، وبلاگ و دانلودها. بیا یکی‌یکی ببینیمشون.",
            GuideTarget.BOTTOM_NAV
        )
    )
    add(
        GuideStep(
            "بازی‌ها",
            "اینجا جدیدترین و محبوب‌ترین بازی‌های اندروید رو پیدا می‌کنی، با لینک مستقیم و دانلود راحت.",
            GuideTarget.TAB_GAMES, "games"
        )
    )
    add(
        GuideStep(
            "برنامه‌ها",
            "برنامه‌های کاربردی اندروید اینجاست؛ از ابزارها تا سرگرمی، همه دسته‌بندی شده.",
            GuideTarget.TAB_APPS, "apps"
        )
    )
    add(
        GuideStep(
            "وبلاگ",
            "مقاله‌ها، خبرها و معرفی‌های تازه‌ی اپدونی اینجان. اعلان وبلاگ رو هم می‌تونی از تنظیمات روشن یا خاموش کنی.",
            GuideTarget.TAB_BLOG, "blog"
        )
    )
    add(
        GuideStep(
            "دانلودها",
            "هر چی دانلود کنی اینجا جمع می‌شه؛ پیشرفت دانلود رو ببین و فایل‌هات رو مدیریت کن.",
            GuideTarget.TAB_DOWNLOADS, "downloads"
        )
    )
    add(
        GuideStep(
            "جستجو",
            "اسم هر بازی یا برنامه‌ای رو توی این نوار بنویس تا سریع پیداش کنی.",
            GuideTarget.SEARCH, "games"
        )
    )
    add(
        GuideStep(
            "منوی شما",
            "این دکمه منوته: دانلودها، علاقه‌مندی‌ها، تنظیمات و درباره اپدونی. اگه حساب داری یا می‌خوای بسازی، ورود هم از همینجاست.",
            GuideTarget.ACCOUNT
        )
    )
    add(
        GuideStep(
            "تنظیمات",
            "از همین منو وارد تنظیمات شو تا تم، رنگ و اندازه‌ی فونت رو عوض کنی، پاپی رو روشن یا خاموش کنی، دانلود فقط با وای‌فای رو فعال کنی و اعلان‌ها رو مدیریت کنی. همه‌چیز آماده‌ست، خوش اومدی!",
            GuideTarget.ACCOUNT
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
 * راهنمای اولین ورود: بیرونِ دایره بلور و مات، داخلش شفاف؛ پاپی توی بخش بلوری توضیح می‌ده.
 * hazeState باید منبعِ کل رابط (نوار بالا، منوی پایین، پاپی و محتوا) باشه.
 */
@Composable
fun AppGuide(
    hazeState: HazeState,
    showPoppyStep: Boolean,
    startRoute: String,
    onNavigate: (String) -> Unit,
    onFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val steps = remember(showPoppyStep) { buildSteps(showPoppyStep) }
    var step by remember { mutableIntStateOf(0) }
    val current = steps[step]
    val last = step == steps.lastIndex

    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(450)) }

    // هر مرحله‌ی تب‌ها رو واقعاً باز می‌کنه
    LaunchedEffect(step) { current.route?.let { onNavigate(it) } }

    // تایپ شدن متن پاپی
    var shown by remember(step) { mutableIntStateOf(0) }
    LaunchedEffect(step) {
        shown = 0
        delay(300)
        while (shown < current.text.length) {
            delay(24)
            shown++
        }
    }
    val typing = shown < current.text.length

    // فقط دکمه‌ها می‌تونن راهنما رو ببندن
    BackHandler(enabled = true) { }

    val close = {
        onNavigate(startRoute)
        onFinish()
    }

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
        val glassBottom = h - navInset - px(10.dp)
        val glassTop = glassBottom - px(68.dp)

        fun tabRect(index: Int): Rect {
            val innerW = w - px(32.dp) - px(12.dp)
            val itemW = innerW / 4f
            val x = physX(px(22.dp) + itemW * index, itemW)
            return Rect(x, glassTop + px(6.dp), x + itemW, glassTop + px(62.dp))
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
            GuideTarget.BOTTOM_NAV -> Rect(px(16.dp), glassTop, w - px(16.dp), glassBottom)
            GuideTarget.TAB_GAMES -> tabRect(0)
            GuideTarget.TAB_APPS -> tabRect(1)
            GuideTarget.TAB_BLOG -> tabRect(2)
            GuideTarget.TAB_DOWNLOADS -> tabRect(3)
            GuideTarget.SEARCH -> {
                val width = w - px(78.dp)
                val x = physX(px(12.dp), width)
                Rect(x, barTop, x + width, barTop + barH)
            }
            GuideTarget.ACCOUNT -> {
                val x = physX(w - px(56.dp), px(44.dp))
                Rect(x, barTop, x + px(44.dp), barTop + barH)
            }
        }

        // هدف‌های تقریباً مربعی دایره می‌شن، هدف‌های پهن کپسول
        val raw = rectFor(current.target)
        val padPx = px(8.dp)
        val targetHole: Rect
        val targetCorner: Float
        if (raw.width / raw.height < 2.2f) {
            val r = maxOf(raw.width, raw.height) / 2f + padPx
            targetHole = Rect(raw.center, r)
            targetCorner = r
        } else {
            targetHole = raw.inflate(padPx)
            targetCorner = targetHole.height / 2f
        }

        val hole by animateRectAsState(targetHole, tween(450, easing = FastOutSlowInEasing), label = "hole")
        val corner by animateFloatAsState(targetCorner, tween(450, easing = FastOutSlowInEasing), label = "corner")

        val bg = scheme.background

        // لایه‌ی بلور و مات: همه‌جا به‌جز داخل دایره
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(HoleShape(hole, corner))
                .hazeEffect(state = hazeState) {
                    blurRadius = 22.dp
                    backgroundColor = bg
                    tints = listOf(HazeTint(bg.copy(alpha = 0.6f)))
                    // اندروید ۱۱ و پایین‌تر بلور نداره؛ فقط لایه‌ی مات میاد
                    fallbackTint = HazeTint(bg.copy(alpha = 0.92f))
                }
        )

        // حلقه‌ی دور دایره
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

        // جلوی لمس شدن اپ زیر راهنما رو می‌گیره (داخل دایره هم)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures { } }
        )

        // پاپی و پیامش، سمت مخالفِ دایره
        val holeInLowerHalf = hole.center.y > h / 2f
        val topPad = with(density) { statusTop.toDp() } + 20.dp
        val bottomPad = with(density) { navInset.toDp() } + 24.dp

        Column(
            modifier = Modifier
                .align(if (holeInLowerHalf) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(top = if (holeInLowerHalf) topPad else 0.dp, bottom = if (holeInLowerHalf) 0.dp else bottomPad)
                .padding(horizontal = 20.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CatAvatar(sizeDp = 72, hasShadow = false, talking = typing)
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, primary.copy(alpha = 0.5f)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        steps.indices.forEach { i ->
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(if (i == step) 18.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i == step) primary else scheme.outline.copy(alpha = 0.5f))
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = current.title,
                        color = scheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    // متن کامل نامرئی جا رو نگه می‌داره تا کارت موقع تایپ بالا و پایین نپره
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = current.text,
                            color = Color.Transparent,
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = current.text.take(shown),
                            color = scheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(18.dp))
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
                                modifier = Modifier.weight(1f)
                            ) { close() }
                        }
                        GuideButton(
                            text = if (last) "شروع کنیم" else "بعدی",
                            container = primary,
                            content = Color.White,
                            borderColor = null,
                            modifier = Modifier.weight(if (last) 1f else 1.4f)
                        ) {
                            if (last) close() else step++
                        }
                    }
                }
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
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(container)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = content, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
