package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PlagueDoctorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "PlagueDoctor",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // کلاه، سر و نوک، ردا و دست‌ها: هیچ‌کدوم تو هم نمی‌رن
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.7f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            // لبه‌ی کلاه
            moveTo(5.76f, 5.41f)
            horizontalLineTo(18.24f)

            // کاسه‌ی کلاه
            moveTo(8.49f, 5.41f)
            verticalLineTo(3.46f)
            quadTo(8.49f, 1.9f, 9.28f, 1.9f)
            horizontalLineTo(14.72f)
            quadTo(15.51f, 1.9f, 15.51f, 3.46f)
            verticalLineTo(5.41f)

            // ماسک و نوک
            moveTo(9.27f, 5.41f)
            verticalLineTo(7.75f)
            quadTo(9.27f, 8.53f, 8.49f, 9.16f)
            lineTo(4.98f, 12.83f)
            lineTo(10.05f, 11.88f)
            quadTo(11.61f, 13.21f, 13.17f, 13.06f)
            quadTo(15.51f, 12.43f, 15.51f, 9.7f)
            verticalLineTo(5.41f)

            // ردا
            moveTo(9.6f, 15.2f)
            lineTo(8.2f, 22.4f)
            horizontalLineTo(15.8f)
            lineTo(14.4f, 15.2f)
            close()

            // دست‌ها (از کنار ردا، دور از نوک)
            moveTo(9.25f, 17f)
            lineTo(5.4f, 20.2f)
            moveTo(14.75f, 17f)
            lineTo(18.6f, 20.2f)
        }
        path(fill = SolidColor(Color.Black)) {
            // چشم
            moveTo(11.61f, 8.14f)
            arcToRelative(0.86f, 0.86f, 0f, true, false, 1.72f, 0f)
            arcToRelative(0.86f, 0.86f, 0f, true, false, -1.72f, 0f)
            close()
            // کف دست‌ها
            moveTo(3.85f, 20.55f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, 2.2f, 0f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, -2.2f, 0f)
            close()
            moveTo(17.95f, 20.55f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, 2.2f, 0f)
            arcToRelative(1.1f, 1.1f, 0f, true, false, -2.2f, 0f)
            close()
        }
    }.build()
}

@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // پرش نرم و ژله‌ای موقع حرف زدن
    val jelly = remember { Animatable(0f) }
    LaunchedEffect(talking) {
        if (talking) {
            while (true) {
                jelly.animateTo(1f, spring(0.3f, Spring.StiffnessLow))
                jelly.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
            }
        } else {
            jelly.animateTo(0f, spring(0.4f, Spring.StiffnessLow))
        }
    }

    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape((sizeDp * 0.34f).dp)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                translationY = -jelly.value * (sizeDp * 0.12f).dp.toPx()
                scaleY = 1f + 0.12f * jelly.value
                scaleX = 1f - 0.08f * jelly.value
            }
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .border(1.5.dp, primary, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = PlagueDoctorIcon,
            contentDescription = "دستیار اپدونی",
            tint = primary,
            modifier = Modifier.size((sizeDp * 0.68f).dp)
        )
    }
}
