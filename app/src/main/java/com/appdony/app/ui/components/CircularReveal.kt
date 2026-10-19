package com.appdony.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sqrt

@Composable
fun CircularRevealAnimation(
    targetRoute: String,
    onAnimationEnd: () -> Unit
) {
    val radius = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    // محاسبه بزرگترین شعاع ممکن برای پوشش کل صفحه
    val maxRadius = remember(configuration) {
        val widthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        sqrt(widthPx * widthPx + heightPx * heightPx)
    }

    // نقطه شروع انیمیشن (پایین وسط صفحه، جایی که منوی باتم هست)
    val origin = remember {
        Offset(
            x = with(density) { (configuration.screenWidthDp / 2f).dp.toPx() },
            y = with(density) { configuration.screenHeightDp.dp.toPx() }
        )
    }

    LaunchedEffect(targetRoute) {
        // انیمیشن بزرگ شدن دایره
        radius.animateTo(
            targetValue = maxRadius,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        // وقتی دایره کل صفحه رو پوشاند، مسیر رو عوض کن
        onAnimationEnd()
        // بعد از عوض شدن صفحه، دایره رو محو کن (برمی‌گرده به صفر)
        radius.snapTo(0f)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = MaterialTheme.colorScheme.primary,
            radius = radius.value,
            center = origin
        )
    }
}
