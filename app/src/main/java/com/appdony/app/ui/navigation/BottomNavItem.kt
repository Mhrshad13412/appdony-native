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

/** ضخامت خط آیکن‌های انتخاب‌نشده (۲۴×۲۴) */
private const val StrokeW = 1.9f

private fun builder(name: String) = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
)

/** آیکن تو‌پر (حالت انتخاب‌شده)؛ سوراخ‌ها با جهت مخالف مسیر ساخته شدن. رنگ نهایی با tint میاد */
private fun filledIcon(name: String, vararg paths: String): ImageVector {
    val b = builder(name)
    paths.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
    return b.build()
}

/** آیکن خطی با گوشه‌های گرد (حالت انتخاب‌نشده)؛ dots دایره‌های کوچیک تو‌پرن */
private fun lineIcon(name: String, lines: List<String>, dots: List<String> = emptyList()): ImageVector {
    val b = builder(name)
    lines.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = StrokeW,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    dots.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
    return b.build()
}

// ───────── بازی‌ها: دسته‌ی بازی (sports_esports) ─────────
private const val GamepadBody =
    "M21.58 16.09l-1.09-7.66C20.21 6.46 18.52 5 16.53 5H7.47C5.48 5 3.79 6.46 3.51 8.43l-1.09 7.66C2.2 17.63 3.39 19 4.94 19c.68 0 1.32-.27 1.8-.75L9 16h6l2.25 2.25c.48.48 1.13.75 1.8.75 1.56 0 2.75-1.37 2.53-2.91z"

private val GamesOutlined = lineIcon(
    name = "games_outlined",
    lines = listOf(
        GamepadBody,
        "M6.5 10.5h4",
        "M8.5 8.5v4"
    ),
    dots = listOf(
        "M15 7.9a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2z",
        "M17 10.9a1.1 1.1 0 1 0 0 2.2 1.1 1.1 0 0 0 0-2.2z"
    )
)

private val GamesFilled = filledIcon(
    "games_filled",
    "$GamepadBody" +
        "M11 11H9v2H8v-2H6v-1h2V8h1v2h2v1z" +
        "m4-1c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z" +
        "m2 3c-.55 0-1-.45-1-1s.45-1 1-1 1 .45 1 1-.45 1-1 1z"
)

// ───────── برنامه‌ها: چهار مربع گرد ─────────
private val AppsOutlined = lineIcon(
    name = "apps_outlined",
    lines = listOf(
        "M6 4h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M16 4h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M6 14h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2z",
        "M16 14h2a2 2 0 0 1 2 2v2a2 2 0 0 1-2 2h-2a2 2 0 0 1-2-2v-2a2 2 0 0 1 2-2z"
    )
)

private val AppsFilled = filledIcon(
    "apps_filled",
    "M5 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
    "M15 3h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
    "M5 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z",
    "M15 13h4a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-4a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z"
)

// ───────── وبلاگ: مقاله (article) ─────────
private val BlogOutlined = lineIcon(
    name = "blog_outlined",
    lines = listOf(
        "M6.5 4h11A2.5 2.5 0 0 1 20 6.5v11a2.5 2.5 0 0 1-2.5 2.5h-11A2.5 2.5 0 0 1 4 17.5v-11A2.5 2.5 0 0 1 6.5 4z",
        "M8 8h8",
        "M8 12h8",
        "M8 16h5"
    )
)

private val BlogFilled = filledIcon(
    "blog_filled",
    "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-5 14H7v-2h7v2zm3-4H7v-2h10v2zm0-4H7V7h10v2z"
)

// ───────── دانلودها: فلش رو به پایین با سینی ─────────
private val DownloadsOutlined = lineIcon(
    name = "downloads_outlined",
    lines = listOf(
        "M12 4.5v10",
        "M7.5 10.5l4.5 4.5 4.5-4.5",
        "M5 19.5h14"
    )
)

private val DownloadsFilled = filledIcon(
    "downloads_filled",
    "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"
)

val bottomNavItems = listOf(
    BottomNavItem(route = "games", label = "بازی‌ها", selectedIcon = GamesFilled, unselectedIcon = GamesOutlined),
    BottomNavItem(route = "apps", label = "برنامه‌ها", selectedIcon = AppsFilled, unselectedIcon = AppsOutlined),
    BottomNavItem(route = "blog", label = "وبلاگ", selectedIcon = BlogFilled, unselectedIcon = BlogOutlined),
    BottomNavItem(route = "downloads", label = "دانلودها", selectedIcon = DownloadsFilled, unselectedIcon = DownloadsOutlined)
)
