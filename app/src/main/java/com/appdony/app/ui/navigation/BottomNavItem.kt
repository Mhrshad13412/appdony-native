package com.appdony.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = "games",
        label = "بازی‌ها",
        selectedIcon = Icons.Filled.SportsEsports,
        unselectedIcon = Icons.Outlined.SportsEsports
    ),
    BottomNavItem(
        route = "apps",
        label = "برنامه‌ها",
        selectedIcon = Icons.Filled.Apps,
        unselectedIcon = Icons.Outlined.Apps
    ),
    BottomNavItem(
        route = "blog",
        label = "وبلاگ",
        selectedIcon = Icons.AutoMirrored.Filled.Article,
        unselectedIcon = Icons.AutoMirrored.Outlined.Article
    ),
    BottomNavItem(
        route = "downloads",
        label = "دانلودها",
        selectedIcon = Icons.Filled.FileDownload,
        unselectedIcon = Icons.Outlined.FileDownload
    )
)
