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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** حالت ظاهر برنامه: true = شیشه‌ای (بلور و بوردر)، false = کلاسیک (مات و بدون بوردر) */
val LocalGlassMode = compositionLocalOf { false }

/** گوشه‌ی گرد مشترک همه‌ی کارت‌ها؛ هم‌خانواده با منوی پایین (که ۳۲dp گرده) */
val AppdonyCardShape = RoundedCornerShape(24.dp)

/** گوشه‌ی کارت‌ها در حالت کلاسیک: کمتر گرد */
val ClassicCardShape = RoundedCornerShape(16.dp)

/** بوردر فقط در حالت شیشه‌ای کشیده می‌شه؛ در کلاسیک هیچ بوردری نیست */
@Composable
fun Modifier.glassBorder(width: Dp, color: Color, shape: Shape): Modifier =
    if (LocalGlassMode.current) this.border(width, color, shape) else this

/** نسخه‌ی BorderStroke برای Surface و Card؛ در کلاسیک null */
@Composable
fun glassStroke(width: Dp, color: Color): BorderStroke? =
    if (LocalGlassMode.current) BorderStroke(width, color) else null

/**
 * استایل مشترک کارت‌ها: همون رنگ سطح و همون خط دور نوار پایینی،
 * تا کارت‌ها و منوی پایین یه ظاهر داشته باشن.
 */
@Composable
fun Modifier.appdonyCard(shape: Shape? = null): Modifier {
    val scheme = MaterialTheme.colorScheme
    val glass = LocalGlassMode.current
    val s = shape ?: if (glass) AppdonyCardShape else ClassicCardShape
    val base = this
        .clip(s)
        .background(scheme.surface)
    return if (glass) base.border(1.dp, scheme.outline.copy(alpha = 0.35f), s) else base
}
