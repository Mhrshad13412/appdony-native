package com.appdony.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
private val NavItemHeight = 56.dp

// دکمه‌ی هوش مصنوعی وسط منو (مثل بام): حلقه‌ی بیرونی، دایره‌ی داخلی و جای خالی بین تب‌ها
private val AiSlotWidth = 78.dp
private val AiRingSize = 72.dp
private val AiButtonSize = 62.dp

/** چقدر دکمه‌ی وسط از لبه‌ی بالای منو بیرون می‌زنه */
private val AiBump = 24.dp

/** ارتفاع کل منو وقتی دکمه‌ی هوش مصنوعی وسطشه (برای کسانی که جا رزرو می‌کنن) */
val AiNavTotalHeight = 88.dp + AiBump

/**
 * فاصله‌ای که لیست‌های صفحه‌های اصلی باید پایین نگه دارن تا آخرِ لیست زیر منوی پایین نره.
 * منوی پایین روی محتوا می‌شینه، پس جای خودش رو از صفحه رزرو نمی‌کنه.
 */
val LocalBottomBarClearance = compositionLocalOf<Dp> { 0.dp }

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

    if (!glass) {
        ClassicBottomNav(selectedIndex, currentRoute, onTabSelected, fixedAi, onAiClick, onSupportClick)
        return
    }

    val barShape = RoundedCornerShape(32.dp)

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp + bump, bottom = 10.dp)
        ) {
            // شیشه‌ی بلوری: محتوای پشتش (که زیرش اسکرول می‌شه) محو می‌شه
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
                        fixedAi = fixedAi,
                        indicatorShape = RoundedCornerShape(26.dp),
                        indicatorBorder = true,
                        indicatorLabel = "nav_indicator_x"
                    )
                }
            }
        }

        if (fixedAi) {
            AiNavButton(
                route = currentRoute,
                onClick = onAiClick,
                onSupportClick = onSupportClick,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
            )
        }
    }
}

/** حالت کلاسیک: نوار مات، چسبیده به لبه‌های صفحه، بدون بلور و بدون بوردر */
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

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = bump)
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            NavTabsArea(
                selectedIndex = selectedIndex,
                currentRoute = currentRoute,
                onTabSelected = onTabSelected,
                fixedAi = fixedAi,
                indicatorShape = RoundedCornerShape(22.dp),
                indicatorBorder = false,
                indicatorLabel = "classic_nav_indicator_x"
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
 * ردیف تب‌ها و اندیکاتور لغزنده. وقتی [fixedAi] روشنه، وسط ردیف یه جای خالی برای دکمه‌ی
 * هوش مصنوعی می‌ماند: دو تب یه طرف، دو تب طرف دیگه.
 */
@Composable
private fun NavTabsArea(
    selectedIndex: Int,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean,
    indicatorShape: Shape,
    indicatorBorder: Boolean,
    indicatorLabel: String
) {
    val primary = MaterialTheme.colorScheme.primary
    val half = bottomNavItems.size / 2
    val supportOn = SupportPresence.phase.let { it == SupportPhase.ENTER || it == SupportPhase.ON }

    BoxWithConstraints {
        val aiWidth = if (fixedAi) AiSlotWidth else 0.dp
        val tabWidth = (maxWidth - aiWidth) / bottomNavItems.size

        // اندیکاتور لغزنده: با فنر از یک تب به تب بعدی «سُر می‌خورد»
        // (آفست در RTL از راست حساب می‌شود، یعنی هم‌جهت با ترتیب آیتم‌ها)
        val indicatorX by animateDpAsState(
            targetValue = tabWidth * selectedIndex + (if (fixedAi && selectedIndex >= half) aiWidth else 0.dp),
            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
            label = indicatorLabel
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorX)
                .width(tabWidth)
                .height(NavItemHeight)
                .clip(indicatorShape)
                .background(primary.copy(alpha = 0.14f))
                .then(if (indicatorBorder) Modifier.border(1.5.dp, primary, indicatorShape) else Modifier)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                if (fixedAi && index == half) {
                    // جای دکمه‌ی وسط؛ فقط اسمش پایین می‌شینه
                    Box(
                        modifier = Modifier.width(AiSlotWidth).height(NavItemHeight),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = if (supportOn) "پشتیبانی" else "هوش مصنوعی",
                            fontSize = 10.sp,
                            color = primary,
                            maxLines = 1,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                NavTab(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { if (currentRoute != item.route) onTabSelected(item.route) },
                    modifier = Modifier.width(tabWidth)
                )
            }
        }
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
 * دکمه‌ی گرد وسط منو مثل بام: از لبه‌ی بالای منو بیرون می‌زنه و حلقه‌ی دورش مثل بریدگی منو دیده می‌شه.
 * تو دایره‌ی وسطش ربات هوش مصنوعیه که به رنگ تم برنامه‌ست و زندگی خودش رو داره:
 * هر چند ثانیه حرف می‌زنه (حباب میاد)، می‌پره، می‌خوره به لبه‌ی دایره و گیج می‌شه یا می‌ترسه.
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
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "ai_press"
    )
    // موج نرمی که از دکمه پخش می‌شه و محو می‌شه
    val pulse = rememberInfiniteTransition(label = "ai_pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearOutSlowInEasing), RepeatMode.Restart),
        label = "ai_pulse_value"
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
                when ((0..99).random()) {
                    in 0..44 -> speak(nextBuddySay(routeNow))
                    in 45..64 -> {
                        launch { buddy.jump() }
                        buddy.cheer()
                    }
                    in 65..84 -> {
                        launch { buddy.bonk(if (Random.nextBoolean()) 1f else -1f) }
                        delay(450)
                        speak(AssistantSay("آخ! خوردم به دیوار... سرم گیج می‌ره"), extraMs = 1_200L)
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

    // رنگ‌های دکمه از تم برنامه می‌آن
    val fill = lerp(scheme.background, scheme.primary, 0.16f)
    val pulseColor = scheme.primary

    Box(
        modifier = modifier
            .size(AiRingSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                translationY = -buddy.hop.value * 10.dp.toPx()
            }
            .drawBehind {
                val t = pulse.value
                drawCircle(
                    color = pulseColor.copy(alpha = 0.22f * (1f - t)),
                    radius = size.minDimension / 2f + 10.dp.toPx() * t
                )
            }
            .clip(CircleShape)
            .background(scheme.background)
            .glassBorder(1.dp, scheme.outline.copy(alpha = 0.35f), CircleShape)
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
                Box(modifier = Modifier.size(AiButtonSize).clip(CircleShape)) {
                    SupportAvatar(sizeDp = AiButtonSize.value.toInt(), ring = ring)
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(AiButtonSize)
                        .clip(CircleShape)
                        .background(fill)
                        .border(1.5.dp, scheme.primary.copy(alpha = 0.55f), CircleShape)
                ) {
                    BuddyCanvas(
                        modifier = Modifier.size(AiButtonSize),
                        talking = talking,
                        plate = fill,
                        ink = scheme.primary,
                        state = buddy
                    )
                }
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
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_icon_scale"
    )
    val iconLift by animateDpAsState(
        targetValue = if (selected) (-1).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "nav_icon_lift"
    )

    Column(
        modifier = modifier
            .height(NavItemHeight)
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = item.selectedIcon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .offset(y = iconLift)
                .scale(iconScale)
        )
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
