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
import androidx.compose.ui.unit.dp // 👈 اضافه شد
import kotlin.math.sqrt

@Composable
fun CircularRevealAnimation(
    targetRoute: String,
    onAnimationEnd: () -> Unit
) {
    val radius = remember { Animatable(0f) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // 👇 رنگ رو اینجا می‌گیریم
    val primaryColor = MaterialTheme.colorScheme.primary

    val maxRadius = remember(configuration) {
        val widthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
        val heightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
        // 👇 تبدیل به Float
        sqrt((widthPx * widthPx + heightPx * heightPx).toDouble()).toFloat()
    }

    val origin = remember {
        Offset(
            x = with(density) { (configuration.screenWidthDp / 2f).dp.toPx() },
            y = with(density) { configuration.screenHeightDp.dp.toPx() }
        )
    }

    LaunchedEffect(targetRoute) {
        radius.animateTo(
            targetValue = maxRadius,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        onAnimationEnd()
        radius.snapTo(0f)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            color = primaryColor, // 👈 استفاده از رنگ گرفته شده
            radius = radius.value,
            center = origin
        )
    }
}
