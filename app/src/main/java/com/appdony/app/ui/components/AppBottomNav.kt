package com.appdony.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Color
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
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.15f else 1f,
                animationSpec = tween(250),
                label = "icon_scale"
            )
            val labelColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                animationSpec = tween(250),
                label = "label_color"
            )
            val interactionSource = remember { MutableInteractionSource() }

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        onTabSelected(item.route)
                    }
                },
                interactionSource = interactionSource,
                icon = {
                    Icon(
                        if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp).scale(iconScale)
                    )
                },
                label = {
                    Text(
                        item.label,
                        fontSize = 11.sp,
                        color = labelColor,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent // 👈 حذف دایره آبی
                )
            )
        }
    }
}
