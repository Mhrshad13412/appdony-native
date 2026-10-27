package com.appdony.app.ui.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

// آیکن‌های خطی مدرن (۲۴×۲۴)؛ رنگ نهایی از بیرون با tint تعیین می‌شود
private fun lineIcon(name: String, vararg paths: String): ImageVector {
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
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return builder.build()
}

private val GamesIcon = lineIcon(
    "games",
    "M7 7.5h10a4.5 4.5 0 0 1 4.4 5.4l-.7 3.4a2.6 2.6 0 0 1-4.3 1.4l-1.7-1.7H9.3l-1.7 1.7a2.6 2.6 0 0 1-4.3-1.4l-.7-3.4A4.5 4.5 0 0 1 7 7.5z",
    "M8 10.5v3M6.5 12h3",
    "M15.5 11.4h.01M17.7 13h.01"
)

private val AppsIcon = lineIcon(
    "apps",
    "M6.5 4h2A2.5 2.5 0 0 1 11 6.5v2A2.5 2.5 0 0 1 8.5 11h-2A2.5 2.5 0 0 1 4 8.5v-2A2.5 2.5 0 0 1 6.5 4z",
    "M15.5 4h2A2.5 2.5 0 0 1 20 6.5v2A2.5 2.5 0 0 1 17.5 11h-2A2.5 2.5 0 0 1 13 8.5v-2A2.5 2.5 0 0 1 15.5 4z",
    "M6.5 13h2A2.5 2.5 0 0 1 11 15.5v2A2.5 2.5 0 0 1 8.5 20h-2A2.5 2.5 0 0 1 4 17.5v-2A2.5 2.5 0 0 1 6.5 13z",
    "M15.5 13h2A2.5 2.5 0 0 1 20 15.5v2A2.5 2.5 0 0 1 17.5 20h-2A2.5 2.5 0 0 1 13 17.5v-2A2.5 2.5 0 0 1 15.5 13z"
)

private val BlogIcon = lineIcon(
    "blog",
    "M5 5.5A1.5 1.5 0 0 1 6.5 4h9A1.5 1.5 0 0 1 17 5.5V19H7.5A2.5 2.5 0 0 1 5 16.5v-11z",
    "M17 9h2a1 1 0 0 1 1 1v7.5a1.5 1.5 0 0 1-3 0",
    "M8.5 8h5M8.5 11.5h5M8.5 15h3"
)

private val DownloadsIcon = lineIcon(
    "downloads",
    "M12 4v11M7.5 11l4.5 4.5 4.5-4.5M5 19.5h14"
)

val bottomNavItems = listOf(
    BottomNavItem(route = "games", label = "بازی‌ها", selectedIcon = GamesIcon, unselectedIcon = GamesIcon),
    BottomNavItem(route = "apps", label = "برنامه‌ها", selectedIcon = AppsIcon, unselectedIcon = AppsIcon),
    BottomNavItem(route = "blog", label = "وبلاگ", selectedIcon = BlogIcon, unselectedIcon = BlogIcon),
    BottomNavItem(route = "downloads", label = "دانلودها", selectedIcon = DownloadsIcon, unselectedIcon = DownloadsIcon)
)
