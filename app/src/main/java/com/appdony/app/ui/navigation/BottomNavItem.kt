package com.appdony.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Info
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
        selectedIcon = Icons.Filled.PlayArrow, // جایگزین SportsEsports
        unselectedIcon = Icons.Outlined.PlayArrow
    ),
    BottomNavItem(
        route = "apps",
        label = "برنامه‌ها",
        selectedIcon = Icons.Filled.List, // جایگزین Apps
        unselectedIcon = Icons.Outlined.List
    ),
    BottomNavItem(
        route = "blog",
        label = "وبلاگ",
        selectedIcon = Icons.Filled.Info, // جایگزین Article
        unselectedIcon = Icons.Outlined.Info
    ),
    BottomNavItem(
        route = "downloads",
        label = "دانلودها",
        selectedIcon = Icons.Filled.Download, // جایگزین FileDownload
        unselectedIcon = Icons.Outlined.Download
    )
)
