package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** گوشه‌ی گرد مشترک همه‌ی کارت‌ها؛ هم‌خانواده با منوی پایین (که ۳۲dp گرده) */
val AppdonyCardShape = RoundedCornerShape(24.dp)

/**
 * استایل مشترک کارت‌ها: همون رنگ سطح و همون خط دور نوار پایینی،
 * تا کارت‌ها و منوی پایین یه ظاهر داشته باشن.
 */
@Composable
fun Modifier.appdonyCard(shape: Shape = AppdonyCardShape): Modifier {
    val scheme = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(scheme.surface)
        .border(1.dp, scheme.outline.copy(alpha = 0.35f), shape)
}

