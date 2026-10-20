package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.navigation.bottomNavItems

@Composable
fun AppBottomNav(
    currentRoute: String?,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { item ->
            val selected = currentRoute == item.route
            
            // ۱. مدیریت وضعیت لمس شدن دکمه (برای افکت فشرده شدن)
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()

            // ۲. انیمیشن فنری برای فشرده شدن (وقتی کاربر کلیک می‌کنه)
            val pressScale by animateFloatAsState(
                targetValue = if (isPressed) 0.85f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "press_scale"
            )

            // ۳. انیمیشن انتخاب شدن (بزرگ شدن ملایم)
            val selectedScale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                label = "selected_scale"
            )

            // ۴. انیمیشن تغییر رنگ متن
            val labelColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(300),
                label = "label_color"
            )

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        onTabSelected(item.route)
                    }
                },
                interactionSource = interactionSource,
                icon = {
                    // ۵. استفاده از AnimatedContent برای تعویض نرم و جادویی آیکون‌ها (Filled و Outlined)
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.7f, animationSpec = tween(200))) togetherWith
                            (fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.7f, animationSpec = tween(200)))
                        },
                        label = "icon_swap"
                    ) { isSelected ->
                        val icon = if (isSelected) item.selectedIcon else item.unselectedIcon
                        Icon(
                            imageVector = icon,
                            contentDescription = item.label,
                            modifier = Modifier
                                .size(24.dp)
                                // اعمال همزمان انیمیشن فشردن و انیمیشن انتخاب
                                .scale(pressScale * selectedScale) 
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        color = labelColor,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.scale(pressScale) // فشرده شدن متن همراه با آیکون
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    // هایلایت کپسولی ملایم برای تب فعال
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}
