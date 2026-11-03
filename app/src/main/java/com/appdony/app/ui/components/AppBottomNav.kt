package com.appdony.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.appdony.app.ui.navigation.BottomNavItem
import com.appdony.app.ui.navigation.bottomNavItems
import com.appdony.app.util.AssistantBus
import com.appdony.app.util.AssistantSay
import com.appdony.app.util.SayKind
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val NavInnerPadding = 6.dp
private val NavItemHeight = 58.dp

// مثل پلی‌استور: آیکن ۲۴، یه قرص گرد پشتش و اسم زیر قرص
private val NavIconSize = 24.dp
private val NavPillWidth = 56.dp
private val NavPillHeight = 32.dp
private val NavPillTop = 4.dp

// دکمه‌ی هوش مصنوعی مثل بام: دایره‌ی توپر که تو یه «برش گرد» از خودِ نوار می‌شینه
private val AiButtonSize = 54.dp

/** چقدر دکمه‌ی وسط از لبه‌ی بالای منو بیرون می‌زنه */
private val AiBump = 22.dp

/** فاصله‌ی برش تا لبه‌ی دکمه */
private val AiNotchGap = 5.dp
private val AiNotchRadius = AiButtonSize / 2 + AiNotchGap

/** مرکز برش نسبت به لبه‌ی بالای نوار (همون مرکز دکمه) */
private val AiNotchCenterY = AiButtonSize / 2 - AiBump

/** ارتفاع کل منو وقتی دکمه‌ی هوش مصنوعی وسطشه (برای کسانی که جا رزرو می‌کنن) */
val AiNavTotalHeight = 88.dp + AiBump

/**
 * فاصله‌ای که لیست‌های صفحه‌های اصلی باید پایین نگه دارن تا آخرِ لیست زیر منوی پایین نره.
 * منوی پایین روی محتوا می‌شینه، پس جای خودش رو از صفحه رزرو نمی‌کنه.
 */
val LocalBottomBarClearance = compositionLocalOf<Dp> { 0.dp }

/**
 * بدنه‌ی نوار با یه برش گرد وسطِ لبه‌ی بالا (مثل بام).
 * برش از خودِ نوار بریده می‌شه، پس رنگ و حلقه‌ی جدا نداره؛
 * هر چی پشت نواره (محتوای صفحه) از همون برش دیده می‌شه.
 * [notchRadius] صفر باشه برش نداره (وقتی هوش مصنوعی ثابت خاموشه).
 */
private class NotchedBarShape(
    private val corner: Dp,
    private val notchRadius: Dp,
    private val notchCenterY: Dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val c = with(density) { corner.toPx() }
        val r = with(density) { notchRadius.toPx() }
        val cy = with(density) { notchCenterY.toPx() }
        val bar = Path().apply {
            addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(c, c)))
        }
        if (r <= 0f) return Outline.Generic(bar)
        val hole = Path().apply {
            addOval(Rect(center = Offset(size.width / 2f, cy), radius = r))
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, bar, hole))
    }
}

@Composable
fun AppBottomNav(
    currentRoute: String?,
    hazeState: HazeState,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean = false,
    onAiClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline
    val glass = LocalGlassMode.current
    val bump = if (fixedAi) AiBump else 0.dp
    val notch = if (fixedAi) AiNotchRadius else 0.dp

    if (!glass) {
        ClassicBottomNav(selectedIndex, currentRoute, onTabSelected, fixedAi, onAiClick, onSupportClick)
        return
    }

    val barShape = remember(notch) { NotchedBarShape(corner = 32.dp, notchRadius = notch, notchCenterY = AiNotchCenterY) }

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp + bump, bottom = 10.dp)
        ) {
            // شیشه‌ی بلوری: محتوای پشتش (که زیرش اسکرول می‌شه) محو می‌شه؛ برش دکمه هم جزو همین شکله
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(barShape)
                    .hazeEffect(state = hazeState) {
                        blurRadius = 24.dp
                        backgroundColor = background
                        tints = listOf(HazeTint(surface.copy(alpha = 0.55f)))
                        // اندروید ۱۱ و پایین‌تر بلور نداره؛ فقط یه لایه‌ی نیمه‌مات میاد
                        fallbackTint = HazeTint(surface.copy(alpha = 0.92f))
                    }
                    .border(1.dp, outline.copy(alpha = 0.35f), barShape)
            ) {
                Box(modifier = Modifier.padding(NavInnerPadding)) {
                    NavTabsArea(
                        selectedIndex = selectedIndex,
                        currentRoute = currentRoute,
                        onTabSelected = onTabSelected,
                        fixedAi = fixedAi
                    )
                }
            }
        }

        if (fixedAi) {
            AiNavButton(
                route = currentRoute,
                onClick = onAiClick,
                onSupportClick = onSupportClick,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
            )
        }
    }
}

/** حالت کلاسیک (مثل بام): نوار مات چسبیده به لبه‌های صفحه، خط نازک بالا (دور برش هم می‌چرخه)، بدون بلور */
@Composable
private fun ClassicBottomNav(
    selectedIndex: Int,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean,
    onAiClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    val bump = if (fixedAi) AiBump else 0.dp
    val notch = if (fixedAi) AiNotchRadius else 0.dp
    val surface = MaterialTheme.colorScheme.surface
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val barShape = remember(notch) { NotchedBarShape(corner = 0.dp, notchRadius = notch, notchCenterY = AiNotchCenterY) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = bump)
                .clip(barShape)
                .background(surface)
                .drawBehind {
                    // فقط خط لبه‌ی بالا و دور برش؛ کناره‌ها و پایین بی‌خط می‌مونن
                    val edge = barShape.createOutline(size, layoutDirection, this)
                    if (edge is Outline.Generic) {
                        clipRect(
                            left = 2.dp.toPx(),
                            top = 0f,
                            right = size.width - 2.dp.toPx(),
                            bottom = 48.dp.toPx()
                        ) {
                            drawPath(
                                path = edge.path,
                                color = lineColor,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
                .navigationBarsPadding()
                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
            NavTabsArea(
                selectedIndex = selectedIndex,
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
                fixedAi = fixedAi
            )
        }

        if (fixedAi) {
            AiNavButton(
                route = currentRoute,
                onClick = onAiClick,
                onSupportClick = onSupportClick,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * ردیف تب‌ها مثل بام: همه‌ی خونه‌ها مساوین. وقتی [fixedAi] روشنه، خونه‌ی وسط (پنجم)
 * مال دکمه‌ی هوش مصنوعیه و دو تب یه طرفش، دو تب طرف دیگه‌ش.
 * قرص هایلایت پشت آیکنِ تب انتخاب‌شده با یه فنر نرم از تبی به تب دیگه سُر می‌خوره.
 */
@Composable
private fun NavTabsArea(
    selectedIndex: Int,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val half = bottomNavItems.size / 2
    val slots = bottomNavItems.size + if (fixedAi) 1 else 0
    val supportOn = SupportPresence.phase.let { it == SupportPhase.ENTER || it == SupportPhase.ON }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val slotWidth = maxWidth / slots
        val slotIndex = selectedIndex + if (fixedAi && selectedIndex >= half) 1 else 0

        // قرص سُر‌خور (آفست توی RTL از راست حساب می‌شه، یعنی هم‌جهت با ترتیب آیتم‌ها)
        val pillX by animateDpAsState(
            targetValue = slotWidth * slotIndex + (slotWidth - NavPillWidth) / 2,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "nav_pill_x"
        )
        Box(
            modifier = Modifier
                .offset(x = pillX, y = NavPillTop)
                .size(width = NavPillWidth, height = NavPillHeight)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.22f))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            bottomNavItems.forEachIndexed { index, item ->
                if (fixedAi && index == half) {
                    AiSlotLabel(supportOn = supportOn, modifier = Modifier.weight(1f))
                }
                NavTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { if (currentRoute != item.route) onTabSelected(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** خونه‌ی وسط: خود دکمه از بالا میاد؛ اینجا فقط اسمش هم‌تراز اسم بقیه‌ی تب‌ها می‌شینه */
@Composable
private fun AiSlotLabel(supportOn: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.height(NavItemHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(NavPillTop + NavPillHeight + 3.dp))
        Text(
            text = if (supportOn) "پشتیبانی" else "هوش مصنوعی",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ───────────────────────── حرف زدن ربات وسط منو ─────────────────────────

private val BuddyLines = listOf(
    "سلام! من هوش مصنوعی اپدونی‌ام؛ هر سؤالی داری بزن روم.",
    "دنبال بازی یا برنامه‌ای می‌گردی؟ بزن روم تا پیداش کنیم.",
    "من همین پایین منتظرم؛ هر وقت خواستی چت کنیم.",
    "بازی خوب پیدا نمی‌کنی؟ من برات انتخاب می‌کنم.",
    "گوشی‌ها رو گذاشتم روی گوشم تا حرفات رو خوب بشنوم.",
    "چشم‌هام رو دیدی؟ همیشه بیدارم!",
    "یه نفس عمیق بکش، بعد بریم سراغ دانلود.",
    "اگه نصب بازی گیر کرد، از من بپرس."
).map { AssistantSay(it) }

// جمله‌هایی که مخصوص گربه‌ی شناورن و به ربات ثابت نمی‌خورن
private val BuddyBanned = listOf("منو بگیر", "توپ", "مخفی", "کاموا")

private fun nextBuddySay(route: String?): AssistantSay {
    if (Random.nextBoolean()) return BuddyLines.random()
    repeat(5) {
        val s = AssistantPhrases.next(route)
        if (BuddyBanned.none { it in s.text }) return s
    }
    return BuddyLines.random()
}

/** حباب همیشه درست بالای دکمه می‌شینه و از لبه‌های صفحه بیرون نمی‌زنه */
private object BubbleAbove : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val maxX = (windowSize.width - popupContentSize.width - 12).coerceAtLeast(12)
        val x = (anchorBounds.center.x - popupContentSize.width / 2).coerceIn(12, maxX)
        val y = (anchorBounds.top - popupContentSize.height - 12).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * دکمه‌ی گرد وسط منو مثل بام: فقط یه دایره‌ی توپر به رنگ تم که کمی از لبه‌ی بالای نوار بیرون زده.
 * هیچ حلقه یا رنگ اضافه‌ای دورش نیست؛ جای خالی دورش همون برشیه که از خودِ نوار بریده شده.
 * تو دایره ربات هوش مصنوعیه که زندگی خودش رو داره: هر چند ثانیه حرف می‌زنه (حباب میاد)،
 * می‌پره یا می‌ترسه (خوردن تصادفی به دیوار عمداً حذف شده).
 * وقتی پشتیبان میاد، همین‌جا جای ربات رو می‌گیره و بوردرش تایمرشه؛ با زدنش چت پشتیبانی باز می‌شه.
 */
@Composable
private fun AiNavButton(
    route: String?,
    onClick: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ai_press"
    )

    // پشتیبان توی حالت ثابت جای آیکن هوش مصنوعی می‌شینه؛ ENTER و ON هر دو یعنی فعال
    val supportOn = SupportPresence.phase.let { it == SupportPhase.ENTER || it == SupportPhase.ON }
    val nowMs = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(supportOn) {
        if (!supportOn) return@LaunchedEffect
        while (true) {
            nowMs.longValue = SystemClock.elapsedRealtime()
            if (nowMs.longValue >= SupportPresence.endAt) {
                SupportPresence.reset()
                break
            }
            delay(200)
        }
    }

    // ── جان ربات ──
    val buddy = remember { BuddyState() }
    var say by remember { mutableStateOf<AssistantSay?>(null) }
    var talking by remember { mutableStateOf(false) }
    var wasSupport by remember { mutableStateOf(supportOn) }
    val routeNow by rememberUpdatedState(route)

    suspend fun speak(s: AssistantSay, extraMs: Long = 1_800L) {
        say = s
        talking = true
        delay((s.text.length * 55L).coerceIn(1_500L, 5_000L))
        talking = false
        delay(extraMs)
        if (say === s) say = null
    }

    // کارهای تصادفی ربات؛ وقتی پشتیبان اومده ساکت می‌شه
    LaunchedEffect(supportOn) {
        if (supportOn) {
            say = null
            talking = false
            wasSupport = true
            return@LaunchedEffect
        }
        if (wasSupport) {
            // پشتیبان رفته و ربات برگشته
            wasSupport = false
            delay(500)
            launch { buddy.jump() }
            launch { buddy.cheer() }
            speak(AssistantSay("برگشتم! پشتیبان رفت سر کارش."))
        } else {
            delay(3_500)
        }
        while (true) {
            if (say == null) {
                // خوردن تصادفی به دیوار عمداً حذف شده؛ ربات تو حالت ثابت خودش رو نمی‌کوبه (فقط موقع کشیدن توسط کاربر)
                when ((0..99).random()) {
                    in 0..44 -> speak(nextBuddySay(routeNow))
                    in 45..79 -> {
                        launch { buddy.jump() }
                        buddy.cheer()
                    }
                    else -> {
                        launch { buddy.startle() }
                        speak(AssistantSay("هی! ترسیدم!"), extraMs = 900L)
                    }
                }
            }
            delay((9_000L..16_000L).random())
        }
    }

    // خبرهای فوری اپ (مثلاً تموم شدن دانلود) رو ربات خودش اعلام می‌کنه
    LaunchedEffect(supportOn) {
        if (supportOn) return@LaunchedEffect
        AssistantBus.events.collect { s ->
            if (s.kind == SayKind.DONE) launch { buddy.jump() }
            speak(s, extraMs = 6_000L)
        }
    }

    Box(
        modifier = modifier
            .size(AiButtonSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = -buddy.hop.value * 10.dp.toPx()
            }
            .clip(CircleShape)
            .background(scheme.primary)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    say = null
                    if (supportOn) onSupportClick() else onClick()
                }
            )
            .semantics { contentDescription = if (supportOn) "پشتیبانی" else "هوش مصنوعی" },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = supportOn, animationSpec = tween(350), label = "ai_support_swap") { support ->
            if (support) {
                val ring = ((SupportPresence.endAt - nowMs.longValue).toFloat() / SupportPresence.HOLD_MS)
                    .coerceIn(0f, 1f)
                SupportAvatar(sizeDp = AiButtonSize.value.toInt(), ring = ring)
            } else {
                // ربات سفید روی دایره‌ی توپرِ رنگ تم
                BuddyCanvas(
                    modifier = Modifier.size(AiButtonSize),
                    talking = talking,
                    plate = scheme.primary,
                    ink = Color.White,
                    state = buddy
                )
            }
        }

        // حباب حرف‌های ربات؛ پنجره‌ی جدا که از لبه‌ی دکمه بیرون بزنه و بریده نشه
        val shown = say
        if (shown != null && !supportOn) {
            key(shown) {
                Popup(
                    popupPositionProvider = BubbleAbove,
                    properties = PopupProperties(focusable = false, clippingEnabled = false)
                ) {
                    val pop = remember { Animatable(0f) }
                    LaunchedEffect(Unit) { pop.animateTo(1f, spring(0.55f, Spring.StiffnessMedium)) }
                    Box(
                        modifier = Modifier.graphicsLayer {
                            val s = 0.8f + 0.2f * pop.value
                            scaleX = s
                            scaleY = s
                            alpha = pop.value.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                    ) {
                        SpeechBubble(
                            say = shown,
                            onRight = false,
                            lowerHalf = true,
                            onClick = { say = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavTab(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val tint by animateColorAsState(if (selected) primary else muted, tween(250), label = "nav_tint")

    // آیکن همیشه ۴dp از بالا شروع می‌شه تا دقیقاً وسط قرصِ سُرخور بشینه
    Column(
        modifier = modifier
            .height(NavItemHeight)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = NavPillTop)
                .size(width = NavPillWidth, height = NavPillHeight),
            contentAlignment = Alignment.Center
        ) {
            // انتخاب‌شده: آیکن تو‌پر؛ انتخاب‌نشده: آیکن خطی
            Crossfade(targetState = selected, animationSpec = tween(180), label = "nav_icon_fill") { sel ->
                Icon(
                    imageVector = if (sel) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(NavIconSize)
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
