package com.appdony.app.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// آیکن‌های متریال گوگل (۲۴×۲۴): نسخه‌ی خطی برای حالت عادی و نسخه‌ی تو‌پر برای تب انتخاب‌شده
// رنگ نهایی از بیرون با tint تعیین می‌شود
private fun materialIcon(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    paths.forEach { d ->
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
    return builder.build()
}

// بازی‌ها (videogame_asset)
private val GamesOutlined = materialIcon(
    "games_outlined",
    "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h18v8z",
    "M6 15h2v-2h2v-2H8V9H6v2H4v2h2z",
    "M14.5 12a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z",
    "M18.5 9a1.5 1.5 0 1 0 0 3 1.5 1.5 0 0 0 0-3z"
)

private val GamesFilled = materialIcon(
    "games_filled",
    "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm-10 7H8v3H6v-3H3v-2h3V8h2v3h3v2zm4.5 2c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zm4-3c-.83 0-1.5-.67-1.5-1.5S18.67 9 19.5 9s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z"
)

// برنامه‌ها (grid_view)
private val AppsOutlined = materialIcon(
    "apps_outlined",
    "M3 3v8h8V3H3zm6 6H5V5h4v4zm-6 4v8h8v-8H3zm6 6H5v-4h4v4zm4-16v8h8V3h-8zm6 6h-4V5h4v4zm-6 4v8h8v-8h-8zm6 6h-4v-4h4v4z"
)

private val AppsFilled = materialIcon(
    "apps_filled",
    "M3 3h8v8H3zM13 3h8v8h-8zM3 13h8v8H3zM13 13h8v8h-8z"
)

// وبلاگ (article)
private val BlogOutlined = materialIcon(
    "blog_outlined",
    "M19 5v14H5V5h14m0-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"
)

private val BlogFilled = materialIcon(
    "blog_filled",
    "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"
)

// دانلودها (file_download)
private val DownloadsOutlined = materialIcon(
    "downloads_outlined",
    "M19 9h-4V3H9v6H5l7 7 7-7zM11 11V5h2v6h1.17L12 13.17 9.83 11H11zm-6 7h14v2H5z"
)

private val DownloadsFilled = materialIcon(
    "downloads_filled",
    "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"
)

val bottomNavItems = listOf(
    BottomNavItem(route = "games", label = "بازی‌ها", selectedIcon = GamesFilled, unselectedIcon = GamesOutlined),
    BottomNavItem(route = "apps", label = "برنامه‌ها", selectedIcon = AppsFilled, unselectedIcon = AppsOutlined),
    BottomNavItem(route = "blog", label = "وبلاگ", selectedIcon = BlogFilled, unselectedIcon = BlogOutlined),
    BottomNavItem(route = "downloads", label = "دانلودها", selectedIcon = DownloadsFilled, unselectedIcon = DownloadsOutlined)
)
