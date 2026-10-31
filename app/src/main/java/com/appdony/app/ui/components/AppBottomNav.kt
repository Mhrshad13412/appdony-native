package com.appdony.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.scale
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

/**
 * فاصله‌ای که لیست‌های صفحه‌های اصلی باید پایین نگه دارن تا آخرِ لیست زیر منوی پایین نره.
 * منوی پایین روی محتوا می‌شینه، پس جای خودش رو از صفحه رزرو نمی‌کنه.
 */
val LocalBottomBarClearance = compositionLocalOf<Dp> { 0.dp }

@Composable
fun AppBottomNav(
    currentRoute: String?,
    hazeState: HazeState,
    onTabSelected: (String) -> Unit
) {
    val selectedIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val background = MaterialTheme.colorScheme.background
    val outline = MaterialTheme.colorScheme.outline
    val barShape = RoundedCornerShape(32.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
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
            BoxWithConstraints(modifier = Modifier.padding(NavInnerPadding)) {
                val itemWidth = maxWidth / bottomNavItems.size

                // اندیکاتور لغزنده: با فنر از یک تب به تب بعدی «سُر می‌خورد»
                // (آفست در RTL از راست حساب می‌شود، یعنی هم‌جهت با ترتیب آیتم‌ها)
                val indicatorX by animateDpAsState(
                    targetValue = itemWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                    label = "nav_indicator_x"
                )
                Box(
                    modifier = Modifier
                        .offset(x = indicatorX)
                        .width(itemWidth)
                        .height(NavItemHeight)
                        .clip(RoundedCornerShape(26.dp))
                        .background(primary.copy(alpha = 0.14f))
                        .border(1.5.dp, primary, RoundedCornerShape(26.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bottomNavItems.forEachIndexed { index, item ->
                        NavTab(
                            item = item,
                            selected = index == selectedIndex,
                            onClick = { if (currentRoute != item.route) onTabSelected(item.route) },
                            modifier = Modifier.width(itemWidth)
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
