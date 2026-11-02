package com.appdony.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.appdony.app.R

/**
 * فونت‌ها: فایل‌ها توی app/src/main/res/font/ هستن.
 *   title_bold.ttf  ← فونت تیتر
 *   body_bold.ttf   ← فونت متن عادی
 * هر فونت فقط یه وزن داره؛ برای همه‌ی وزن‌ها همون فایل ثبت شده تا اندروید الکی بولدش نکنه.
 */
val TitleFont = FontFamily(
    Font(R.font.title_bold, FontWeight.Normal),
    Font(R.font.title_bold, FontWeight.Medium),
    Font(R.font.title_bold, FontWeight.SemiBold),
    Font(R.font.title_bold, FontWeight.Bold)
)

val BodyFont = FontFamily(
    Font(R.font.body_bold, FontWeight.Normal),
    Font(R.font.body_bold, FontWeight.Medium),
    Font(R.font.body_bold, FontWeight.SemiBold),
    Font(R.font.body_bold, FontWeight.Bold)
)

private val Base = Typography()

/** تیترها (display / headline / title) با TitleFont، بقیه‌ی متن‌ها با BodyFont */
val AppTypography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = TitleFont),
    displayMedium = Base.displayMedium.copy(fontFamily = TitleFont),
    displaySmall = Base.displaySmall.copy(fontFamily = TitleFont),
    headlineLarge = Base.headlineLarge.copy(fontFamily = TitleFont),
    headlineMedium = Base.headlineMedium.copy(fontFamily = TitleFont),
    headlineSmall = Base.headlineSmall.copy(fontFamily = TitleFont),
    titleLarge = Base.titleLarge.copy(fontFamily = TitleFont),
    titleMedium = Base.titleMedium.copy(fontFamily = TitleFont),
    titleSmall = Base.titleSmall.copy(fontFamily = TitleFont),
    bodyLarge = Base.bodyLarge.copy(fontFamily = BodyFont),
    bodyMedium = Base.bodyMedium.copy(fontFamily = BodyFont),
    bodySmall = Base.bodySmall.copy(fontFamily = BodyFont),
    labelLarge = Base.labelLarge.copy(fontFamily = BodyFont),
    labelMedium = Base.labelMedium.copy(fontFamily = BodyFont),
    labelSmall = Base.labelSmall.copy(fontFamily = BodyFont)
)
