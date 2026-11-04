package com.appdony.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** حالت ظاهر برنامه: true = شیشه‌ای (بلور و بوردر)، false = کلاسیک (مات و بدون بوردر) */
val LocalGlassMode = compositionLocalOf { false }

/** گوشه‌ی گرد مشترک همه‌ی کارت‌ها؛ هم‌خانواده با منوی پایین (که ۳۲dp گرده) */
val AppdonyCardShape = RoundedCornerShape(24.dp)

/** گوشه‌ی کارت‌ها در حالت کلاسیک: کمتر گرد */
val ClassicCardShape = RoundedCornerShape(16.dp)

// ───────────────────────── شیشه‌ی هم‌تم با منوی پایین ─────────────────────────
// همه‌ی عددها (ته‌رنگ، لبه‌ی باریک، بلور) دقیقاً از خودِ منوی پایین برداشته شدن
// تا کارت‌ها، منوی اکانت و دکمه‌های هوش مصنوعی یه ظاهر با منو داشته باشن.

/** تیره بودن تم؛ همون شرطی که منوی پایین استفاده می‌کنه */
@Composable
fun isGlassDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** ته‌رنگ شیشه‌ی منو: مشکیِ تیره‌ی نیمه‌شفاف (تم تیره) یا سفیدِ نیمه‌شفاف (تم روشن) */
fun glassTintOf(dark: Boolean, alpha: Float = 0.55f): Color =
    if (dark) Color(0xFF08090B).copy(alpha = alpha) else Color.White.copy(alpha = alpha)

/** لبه‌ی باریک شیشه‌ای منو: بالا کمی پررنگ‌تر و پایین تقریباً بی‌خط، بدون رنگ تم */
@Composable
fun glassRimBrush(): Brush {
    val outline = MaterialTheme.colorScheme.outline
    return Brush.verticalGradient(
        if (isGlassDark()) listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.03f))
        else listOf(Color.White.copy(alpha = 0.85f), outline.copy(alpha = 0.12f), outline.copy(alpha = 0.06f))
    )
}

/** رنگ «صفحه»‌ی ربات توی دکمه‌های شیشه‌ای: تیره/سفید با یه ذره از رنگ تم (مثل کاشی انتخاب‌شده‌ی منو) */
@Composable
fun glassRobotScreen(): Color {
    val scheme = MaterialTheme.colorScheme
    return lerp(if (isGlassDark()) Color(0xFF08090B) else Color.White, scheme.primary, 0.10f)
}

/**
 * شیشه‌ی واقعی (بلور + ته‌رنگ منو + لبه‌ی منو) برای چیزهایی که بیرون از منبع بلور می‌شینن؛
 * مثل دکمه‌های هوش مصنوعی. [extraTint] یه ته‌رنگ اضافه‌ست (مثلاً رنگ تم، خیلی کم‌رنگ).
 */
@Composable
fun Modifier.glassPanel(
    shape: Shape,
    hazeState: HazeState,
    blur: Dp = 40.dp,
    extraTint: Color? = null
): Modifier {
    val scheme = MaterialTheme.colorScheme
    val dark = isGlassDark()
    val tint = glassTintOf(dark)
    val rim = glassRimBrush()
    val bg = scheme.background
    val surface = scheme.surface
    return this
        .clip(shape)
        .hazeEffect(state = hazeState) {
            blurRadius = blur
            backgroundColor = bg
            tints = listOfNotNull(HazeTint(tint), extraTint?.let { HazeTint(it) })
            noiseFactor = 0.02f
            // اندروید ۱۱ و پایین‌تر بلور نداره؛ فقط یه لایه‌ی نیمه‌مات میاد
            fallbackTint = HazeTint(surface.copy(alpha = 0.88f))
        }
        .border(1.dp, rim, shape)
}

/** بوردر فقط در حالت شیشه‌ای کشیده می‌شه؛ در کلاسیک هیچ بوردری نیست */
@Composable
fun Modifier.glassBorder(width: Dp, color: Color, shape: Shape): Modifier =
    if (LocalGlassMode.current) this.border(width, color, shape) else this

/**
 * نسخه‌ی BorderStroke برای Surface و Card؛ در کلاسیک null.
 * توی حالت شیشه‌ای همیشه لبه‌ی باریک منو کشیده می‌شه ([color] فقط برای سازگاری با کدهای قدیمی مونده).
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun glassStroke(width: Dp, color: Color): BorderStroke? =
    if (LocalGlassMode.current) BorderStroke(width, glassRimBrush()) else null

/**
 * استایل مشترک کارت‌ها.
 * - کلاسیک: سطح مات و بدون بوردر.
 * - شیشه‌ای: همون ته‌رنگ شیشه‌ی منوی پایین + برقِ ملایم از بالا + همون لبه‌ی باریک منو.
 *   (کارت‌ها داخل منبع بلورن و پشتشون فقط رنگ صفحه‌ست، پس بلور اضافه‌ای لازم نیست؛
 *   ته‌رنگ روی رنگ صفحه دقیقاً همون چیزیه که منو نشون می‌ده.)
 */
@Composable
fun Modifier.appdonyCard(shape: Shape? = null): Modifier {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalGlassMode.current
    val s = shape ?: if (glass) AppdonyCardShape else ClassicCardShape
    if (!glass) {
        return this
            .clip(s)
            .background(scheme.surface)
    }
    val dark = isGlassDark()
    val sheen = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = if (dark) 0.05f else 0.30f), Color.Transparent)
    )
    return this
        .clip(s)
        .background(scheme.background)
        .background(glassTintOf(dark))
        .background(sheen)
        .border(1.dp, glassRimBrush(), s)
}

/**
 * مسیر (ردیف) انتخاب‌گرها و کلیدها.
 * کلاسیک: همون سطح مات قبلی. شیشه‌ای: ته‌رنگ و لبه‌ی منوی پایین.
 */
@Composable
fun Modifier.glassTrack(shape: Shape): Modifier {
    val scheme = MaterialTheme.colorScheme
    if (!LocalGlassMode.current) return this.clip(shape).background(scheme.background)
    return this
        .clip(shape)
        .background(scheme.background)
        .background(glassTintOf(isGlassDark()))
        .border(1.dp, glassRimBrush(), shape)
}

/**
 * دسته‌ی لغزنده‌ی انتخاب‌گرها.
 * کلاسیک: توپر با رنگ تم. شیشه‌ای: کاشی شیشه‌ای با ته‌رنگ تم و لبه‌ی منو (مثل کاشی انتخاب‌شده‌ی منوی پایین).
 */
@Composable
fun Modifier.glassKnob(shape: Shape): Modifier {
    val scheme = MaterialTheme.colorScheme
    if (!LocalGlassMode.current) return this.clip(shape).background(scheme.primary)
    val dark = isGlassDark()
    return this
        .clip(shape)
        .background(if (dark) Color.White.copy(alpha = 0.03f) else Color.White.copy(alpha = 0.50f))
        .background(scheme.primary.copy(alpha = if (dark) 0.22f else 0.16f))
        .border(1.dp, glassRimBrush(), shape)
}

/** رنگ آیکن گزینه‌ی انتخاب‌شده: کلاسیک سفید روی دسته‌ی توپر؛ شیشه‌ای رنگ تم */
@Composable
fun glassSelectedIconTint(): Color =
    if (LocalGlassMode.current) MaterialTheme.colorScheme.primary else Color.White
