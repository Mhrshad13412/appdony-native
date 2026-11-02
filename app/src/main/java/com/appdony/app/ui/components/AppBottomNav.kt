package com.appdony.app.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.navigation.BottomNavItem
import com.appdony.app.ui.navigation.bottomNavItems
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    onAiClick: () -> Unit = {}
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline
    val glass = LocalGlassMode.current
    val bump = if (fixedAi) AiBump else 0.dp

    if (!glass) {
        ClassicBottomNav(selectedIndex, currentRoute, onTabSelected, fixedAi, onAiClick)
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
                onClick = onAiClick,
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
    onAiClick: () -> Unit
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
            AiNavButton(onClick = onAiClick, modifier = Modifier.align(Alignment.TopCenter))
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
                            text = "هوش مصنوعی",
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

/**
 * دکمه‌ی گرد وسط منو مثل بام: از لبه‌ی بالای منو بیرون می‌زنه، حلقه‌ی دورش مثل بریدگی منو دیده می‌شه
 * و تو دایره‌ی وسطش آدمک اپدونی (سر و شونه) زنده‌ست؛ پلک می‌زنه، نفس می‌کشه و گاهی سلام می‌ده.
 */
@Composable
private fun AiNavButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val primary = scheme.primary
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

    Box(
        modifier = modifier
            .size(AiRingSize)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .drawBehind {
                val t = pulse.value
                drawCircle(
                    color = primary.copy(alpha = 0.30f * (1f - t)),
                    radius = size.minDimension / 2f + 10.dp.toPx() * t
                )
            }
            .clip(CircleShape)
            .background(scheme.background)
            .glassBorder(1.dp, scheme.outline.copy(alpha = 0.35f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = "هوش مصنوعی" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AiButtonSize)
                .clip(CircleShape)
                .background(scheme.surface)
                .background(primary.copy(alpha = 0.16f))
                .border(2.dp, primary, CircleShape)
        ) {
            BuddyCanvas(modifier = Modifier.size(AiButtonSize), bust = true)
        }

        // نشان «هوش مصنوعی» گوشه‌ی دکمه
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(primary)
                .border(1.5.dp, scheme.background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
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
