package com.appdony.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.appdony.app.data.preferences.SettingsManager
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
private val NavItemHeight = 60.dp
private val NavIconSize = 24.dp

// ── کلاسیک (مثل گوگل پلی): قرص پهن پشت آیکن، اسم زیر قرص ──
private val NavPillWidth = 60.dp
private val NavPillHeight = 32.dp
private val NavPillTop = 4.dp

// ── شیشه‌ای (مثل تلگرام ادز): کاشی مستطیل گرد دور آیکن و اسم، آیکن و اسم از بالا ۱۰dp شروع می‌شن ──
private val GlassContentTop = 10.dp
private val GlassTileInset = 2.dp
private val GlassTileShape = RoundedCornerShape(20.dp)

// دکمه‌ی هوش مصنوعی مثل بام: دایره‌ی توپر که تو یه «برش گرد» از خودِ نوار می‌شینه
private val AiButtonSize = 54.dp

/** خونه‌ی وسط (دکمه‌ی هوش مصنوعی) از بقیه‌ی خونه‌ها پهن‌تره تا «هوش مصنوعی» کامل جا بشه */
private const val AiSlotWeight = 1.5f

/** چقدر دکمه‌ی وسط از لبه‌ی بالای منو بیرون می‌زنه */
private val AiBump = 24.dp

/** فاصله‌ی برش تا لبه‌ی دکمه */
private val AiNotchGap = 8.dp
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
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    // جمله‌های «اپدونی پلاس» فقط وقتی بتا (تنظیمات ← آزمایشی) روشنه گفته می‌شن
    LaunchedEffect(Unit) {
        settings.beta.collect { AssistantPhrases.plusEnabled = it }
    }
    // حالت «ثابت گوشه»: دکمه‌ی هوش مصنوعی بالای منو، گوشه‌ی صفحه می‌شینه (جای خالی دورش لمس رو نمی‌گیره)
    val cornerAi by settings.aiCorner.collectAsState(initial = false)
    val glassMode = LocalGlassMode.current

    Column(modifier = Modifier.fillMaxWidth()) {
        if (cornerAi && !fixedAi) CornerAiButton(onClick = onAiClick, hazeState = hazeState, glass = glassMode)
        NavBar(currentRoute, hazeState, onTabSelected, fixedAi, onAiClick, onSupportClick)
    }
}

@Composable
private fun NavBar(
    currentRoute: String?,
    hazeState: HazeState,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean,
    onAiClick: () -> Unit,
    onSupportClick: () -> Unit
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

    // نوار مستطیل گرد مثل تلگرام ادز (نه کپسول کامل؛ کناره‌ها یه تکه‌ی صاف دارن)
    val barShape = remember(notch) { NotchedBarShape(corner = 28.dp, notchRadius = notch, notchCenterY = AiNotchCenterY) }
    val dark = background.luminance() < 0.5f
    // لبه‌ی «شیشه‌ی مایع»: از گوشه‌ی بالا-چپ برق می‌زنه، وسط تقریباً نامرئیه و پایین-راست دوباره یه برق کم‌رنگ داره
    val rimBrush = Brush.linearGradient(
        if (dark) listOf(Color.White.copy(alpha = 0.42f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.22f))
        else listOf(Color.White.copy(alpha = 0.95f), outline.copy(alpha = 0.10f), outline.copy(alpha = 0.04f), Color.White.copy(alpha = 0.70f))
    )
    // برقِ نرمِ بالای شیشه
    val sheenBrush = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.30f), Color.Transparent)
    )

    Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 8.dp + bump, bottom = 14.dp)
        ) {
            // شیشه‌ی مات: محتوای پشتش (که زیرش اسکرول می‌شه) محو می‌شه؛ برش دکمه هم جزو همین شکله
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(barShape)
                    .hazeEffect(state = hazeState) {
                        // تارتر از قبل (۳۰ ← ۴۴) تا شیشه‌ی مایع باشه ولی متن‌ها خوانا بمونن
                        blurRadius = 44.dp
                        backgroundColor = background
                        // شفاف‌تر از قبل: رنگ‌های پشتش مثل نور از زیر شیشه می‌درخشن
                        tints = listOf(
                            HazeTint(surface.copy(alpha = if (dark) 0.26f else 0.38f)),
                            HazeTint(Color.White.copy(alpha = if (dark) 0.07f else 0.22f))
                        )
                        // دونه‌ی خیلی ریزِ شیشه‌ی مات‌شده
                        noiseFactor = 0.03f
                        // اندروید ۱۱ و پایین‌تر بلور نداره؛ فقط یه لایه‌ی نیمه‌مات میاد
                        fallbackTint = HazeTint(surface.copy(alpha = 0.88f))
                    }
                    .background(sheenBrush)
                    // خط برقِ باریکِ زیر لبه‌ی بالا، مثل بازتاب نور روی شیشه
                    .drawBehind {
                        val h = 1.2.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = if (dark) 0.35f else 0.80f), Color.Transparent)
                            ),
                            topLeft = Offset(size.width * 0.14f, h),
                            size = Size(size.width * 0.72f, h)
                        )
                    }
                    .border(1.dp, rimBrush, barShape)
            ) {
                Box(modifier = Modifier.padding(NavInnerPadding)) {
                    NavTabsArea(
                        selectedIndex = selectedIndex,
                        currentRoute = currentRoute,
                        onTabSelected = onTabSelected,
                        fixedAi = fixedAi,
                        glass = true
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

/** حالت کلاسیک (مثل گوگل پلی/بام): نوار مات چسبیده به لبه‌های صفحه، خط نازک بالا (دور برش هم می‌چرخه)، بدون بلور */
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
                fixedAi = fixedAi,
                glass = false
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
 * ردیف تب‌ها. وقتی [fixedAi] روشنه، خونه‌ی وسط مال دکمه‌ی هوش مصنوعیه
 * (کمی پهن‌تر از بقیه) و دو تب یه طرفش، دو تب طرف دیگه‌ش.
 * هایلایت از تبی به تب دیگه با یه فنر نرم سُر می‌خوره:
 *  - شیشه‌ای: کاشی گرد شیشه‌ای با ته‌رنگ تم دور آیکن و اسم (مثل تلگرام)
 *  - کلاسیک: قرص پهن پشت آیکن (مثل گوگل پلی)
 */
@Composable
private fun NavTabsArea(
    selectedIndex: Int,
    currentRoute: String?,
    onTabSelected: (String) -> Unit,
    fixedAi: Boolean,
    glass: Boolean
) {
    val primary = MaterialTheme.colorScheme.primary
    val half = bottomNavItems.size / 2
    val aiW = if (fixedAi) AiSlotWeight else 0f
    val supportOn = SupportPresence.phase.let { it == SupportPhase.ENTER || it == SupportPhase.ON }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val unit = maxWidth / (bottomNavItems.size + aiW)
        val before = selectedIndex + if (fixedAi && selectedIndex >= half) aiW else 0f
        val slotStart = unit * before

        // آفست توی RTL از راست حساب می‌شه، یعنی هم‌جهت با ترتیب آیتم‌ها
        val indicatorX by animateDpAsState(
            targetValue = if (glass) slotStart + GlassTileInset else slotStart + (unit - NavPillWidth) / 2,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "nav_indicator_x"
        )

        if (glass) {
            // کاشی انتخاب‌شده: حباب شیشه‌ای مایع؛ قطری روشن، وسطش یه ته‌رنگ از تم و لبه‌ی برق‌دار
            Box(
                modifier = Modifier
                    .offset(x = indicatorX)
                    .size(width = unit - GlassTileInset * 2, height = NavItemHeight)
                    .clip(GlassTileShape)
                    .background(
                        Brush.linearGradient(
                            if (dark) listOf(Color.White.copy(alpha = 0.24f), primary.copy(alpha = 0.12f), Color.White.copy(alpha = 0.07f))
                            else listOf(Color.White.copy(alpha = 0.90f), primary.copy(alpha = 0.12f), Color.White.copy(alpha = 0.50f))
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            if (dark) listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.24f))
                            else listOf(Color.White, primary.copy(alpha = 0.18f), Color.White.copy(alpha = 0.85f))
                        ),
                        GlassTileShape
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(x = indicatorX, y = NavPillTop)
                    .size(width = NavPillWidth, height = NavPillHeight)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.24f))
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            bottomNavItems.forEachIndexed { index, item ->
                if (fixedAi && index == half) {
                    AiSlotLabel(supportOn = supportOn, glass = glass, modifier = Modifier.weight(aiW))
                }
                NavTab(
                    item = item,
                    selected = index == selectedIndex,
                    glass = glass,
                    // همیشه خبر می‌ده؛ اینکه روی همون صفحه‌ی تب نیازی به رفتن نیست رو MainActivity تصمیم می‌گیره (توی جزئیات، تبِ روشن باید برگردونه به لیست)
                    onClick = { onTabSelected(item.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** خونه‌ی وسط: خود دکمه از بالا میاد؛ اینجا فقط اسمش (کامل، بدون بریدگی) هم‌تراز اسم بقیه‌ی تب‌ها می‌شینه */
@Composable
private fun AiSlotLabel(supportOn: Boolean, glass: Boolean, modifier: Modifier = Modifier) {
    val labelTop = if (glass) GlassContentTop + NavIconSize + 3.dp else NavPillTop + NavPillHeight + 3.dp
    Column(
        modifier = modifier.height(NavItemHeight),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(labelTop))
        Text(
            text = if (supportOn) "پشتیبانی" else "هوش مصنوعی",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        )
    }
}

// ───────────────────────── دکمه‌ی ثابت گوشه ─────────────────────────

/** فقط یک بار در هر اجرای برنامه اول با نوشته باز می‌شه، بعد به دایره جمع می‌شه */
private var cornerIntroShown = false

/**
 * هوش مصنوعیِ ثابت گوشه (مثل دکمه‌ی پشتیبانی تلگرام ادز): دایره‌ی شیشه‌ای گوشه‌ی راست، بالای منو.
 * اول به شکل قرص با نوشته‌ی «هوش مصنوعی» میاد و بعد از چند ثانیه به دایره جمع می‌شه.
 * تو حالت شیشه‌ای خودش شیشه‌ی رنگیه (بلور + ته‌رنگ تم)؛ تو کلاسیک سطح توپره.
 * توی هر دو حالت، ربات سفیده روی یه دایره‌ی توپرِ رنگ تم (مثل دکمه‌ی وسط منو).
 */
@Composable
private fun CornerAiButton(onClick: () -> Unit, hazeState: HazeState, glass: Boolean) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(!cornerIntroShown) }
    LaunchedEffect(Unit) {
        if (expanded) {
            cornerIntroShown = true
            delay(4_000)
            expanded = false
        }
    }
    val shape = RoundedCornerShape(32.dp)
    // کلاسیک: سطح توپرِ کمی رنگی؛ شیشه‌ای: شیشه‌ی بلورشده با ته‌رنگ تم (پشتش خوانا می‌مونه)
    val fill = scheme.primary.copy(alpha = 0.14f).compositeOver(scheme.surface)

    // چپ‌به‌راست ثابت می‌شه تا دکمه همیشه گوشه‌ی راست بمونه و قرص به سمت چپ باز بشه
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 18.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .clip(shape)
                    .then(
                        if (glass) {
                            // شیشه‌ی رنگی مثل دکمه‌ی «تماس با پشتیبانی» تلگرام ادز
                            Modifier.hazeEffect(state = hazeState) {
                                blurRadius = 30.dp
                                backgroundColor = scheme.background
                                tints = listOf(
                                    HazeTint(scheme.surface.copy(alpha = 0.55f)),
                                    HazeTint(scheme.primary.copy(alpha = 0.18f))
                                )
                                noiseFactor = 0.035f
                                fallbackTint = HazeTint(fill)
                            }
                        } else Modifier.background(fill)
                    )
                    .border(
                        if (glass) 1.dp else 1.5.dp,
                        scheme.primary.copy(alpha = if (glass) 0.40f else 0.55f),
                        shape
                    )
                    .clickable(onClick = onClick)
                    .semantics { contentDescription = "هوش مصنوعی" }
                    // فاصله‌ی یکسان دور دایره‌ی ربات تا پیلِ دور تا دور هم‌اندازه بمونه
                    .padding(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Text(
                        text = "هوش مصنوعی",
                        color = scheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 18.dp, end = 12.dp)
                    )
                }
                // مثل دکمه‌ی وسط منو: دایره‌ی توپرِ رنگ تم با ربات سفید (نه یه دیسک تیره توی قرص)
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape).background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    BuddyCanvas(modifier = Modifier.size(50.dp), plate = scheme.primary, ink = Color.White)
                }
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

/** آیکن تب: انتخاب‌شده تو‌پر، انتخاب‌نشده خطی؛ با کراس‌فید نرم */
@Composable
private fun TabIcon(item: BottomNavItem, selected: Boolean, tint: Color) {
    Crossfade(targetState = selected, animationSpec = tween(180), label = "nav_icon_fill") { sel ->
        Icon(
            imageVector = if (sel) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(NavIconSize)
        )
    }
}

@Composable
private fun NavTab(
    item: BottomNavItem,
    selected: Boolean,
    glass: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // آیکن و اسم تب انتخاب‌شده به رنگ تمه (کلاسیک و شیشه‌ای، مثل تلگرام ادز)
    val tint by animateColorAsState(if (selected) primary else muted, tween(250), label = "nav_tint")

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
        val iconTop = if (glass) GlassContentTop else NavPillTop + (NavPillHeight - NavIconSize) / 2
        val labelTop = if (glass) GlassContentTop + NavIconSize + 3.dp else NavPillTop + NavPillHeight + 3.dp
        Spacer(Modifier.height(iconTop))
        // آیکن انتخاب‌شده با یه فنر نرم کمی بزرگ می‌شه
        val iconScale by animateFloatAsState(
            targetValue = if (selected) 1.12f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
            label = "nav_icon_scale"
        )
        Box(modifier = Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
            TabIcon(item = item, selected = selected, tint = tint)
        }
        Spacer(Modifier.height(labelTop - iconTop - NavIconSize))
        Text(
            text = item.label,
            fontSize = 11.sp,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
