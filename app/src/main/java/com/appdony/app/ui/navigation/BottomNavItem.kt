package com.appdony.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.VideogameAsset
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
        label = "بازی",
        selectedIcon = Icons.Filled.VideogameAsset,
        unselectedIcon = Icons.Outlined.VideogameAsset
    ),
    BottomNavItem(
        route = "apps",
        label = "برنامه",
        selectedIcon = Icons.Filled.Android,
        unselectedIcon = Icons.Outlined.Android
    ),
    BottomNavItem(
        route = "blog",
        label = "وبلاگ",
        selectedIcon = Icons.Filled.Article,
        unselectedIcon = Icons.Outlined.Article
    ),
    BottomNavItem(
        route = "updates",
        label = "دانلودها",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )
)
