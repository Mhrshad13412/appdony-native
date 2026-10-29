package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 8f)
            horizontalLineTo(20f)

            moveTo(7.5f, 8f)
            verticalLineTo(5.5f)
            quadTo(7.5f, 3.5f, 9.5f, 3.5f)
            horizontalLineTo(14.5f)
            quadTo(16.5f, 3.5f, 16.5f, 5.5f)
            verticalLineTo(8f)

            moveTo(8.5f, 8f)
            verticalLineTo(11f)
            quadTo(8.5f, 12f, 7.5f, 12.8f)
            lineTo(3f, 17.5f)
            lineTo(9.5f, 16.3f)
            quadTo(11.5f, 18f, 13.5f, 17.8f)
            quadTo(16.5f, 17f, 16.5f, 13.5f)
            verticalLineTo(8f)

            moveTo(10f, 18.3f)
            lineTo(7.5f, 21f)

            moveTo(15f, 18f)
            lineTo(17.5f, 21f)
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(11.5f, 11.5f)
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
    hasShadow: Boolean = true
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

    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape((sizeDp * 0.34f).dp)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .border(1.5.dp, primary, shape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = PlagueDoctorIcon,
            contentDescription = "دستیار اپدونی",
            tint = primary,
            modifier = Modifier.size((sizeDp * 0.62f).dp)
        )
    }
}
