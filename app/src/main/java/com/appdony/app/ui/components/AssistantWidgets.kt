package com.appdony.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.util.AssistantSay

@Composable
internal fun Rtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

@Composable
internal fun SpeechBubble(
    say: AssistantSay,
    onRight: Boolean,
    lowerHalf: Boolean,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val accent = kindColor(say.kind, primary)
    val big = 18.dp
    val small = 6.dp
    val shape = AbsoluteRoundedCornerShape(
        topLeft = if (!lowerHalf && !onRight) small else big,
        topRight = if (!lowerHalf && onRight) small else big,
        bottomRight = if (lowerHalf && onRight) small else big,
        bottomLeft = if (lowerHalf && !onRight) small else big
    )
    Rtl {
        Surface(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(shape)
                .clickable { onClick() },
            color = MaterialTheme.colorScheme.surface,
            shape = shape,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.65f)),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = kindIcon(say.kind),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = say.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/** یه گلوله‌ی کاموا که آیکن و اسم هر گزینه روشه */
@Composable
internal fun YarnButton(
    icon: ImageVector,
    label: String,
    tint: Color,
    progress: () -> Float,
    spin: () -> Float,
    bob: () -> Float,
    flip: Float,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .graphicsLayer {
                val p = progress()
                alpha = (p * 3f).coerceIn(0f, 1f)
                val s = 0.35f + 0.65f * p
                scaleX = s
                scaleY = s
                val wave = (bob() - 0.5f) * 2f
                translationY = wave * 3.dp.toPx() * flip
                rotationZ = wave * 4f * flip
            }
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .drawBehind {
                    val r = size.minDimension / 2f - 2.dp.toPx()
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawYarnBall(c, r, tint, spin() + (bob() - 0.5f) * 24f)
                    // سایه‌ی نرم پشت آیکن تا همیشه خوانا باشه
                    drawCircle(Color.Black.copy(alpha = 0.28f), r * 0.46f, c)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Rtl {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.45f)),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
