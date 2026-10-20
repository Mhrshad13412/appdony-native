package com.appdony.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpotlightTutorial(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // 👈 استخراج رنگ قبل از Canvas برای جلوگیری از خطای Composable
    val primaryColor = MaterialTheme.colorScheme.primary

    val targetRect = when (currentStep) {
        0 -> Rect(Offset(screenWidthPx * 0.05f, screenHeightPx * 0.02f), Size(screenWidthPx * 0.9f, with(density) { 60.dp.toPx() }))
        1 -> Rect(Offset(screenWidthPx * 0.1f, screenHeightPx * 0.2f), Size(screenWidthPx * 0.8f, with(density) { 200.dp.toPx() }))
        2 -> Rect(Offset(screenWidthPx * 0.05f, screenHeightPx * 0.88f), Size(screenWidthPx * 0.9f, with(density) { 60.dp.toPx() }))
        3 -> Rect(Offset(screenWidthPx * 0.75f, screenHeightPx * 0.75f), Size(with(density) { 64.dp.toPx() }, with(density) { 64.dp.toPx() }))
        else -> Rect(Offset(screenWidthPx * 0.7f, screenHeightPx * 0.02f), Size(with(density) { 60.dp.toPx() }, with(density) { 60.dp.toPx() }))
    }

    val stepTitle = when (currentStep) {
        0 -> "جستجو"
        1 -> "ویژه و اپدونی پلاس"
        2 -> "دسته‌بندی‌ها"
        3 -> "دستیار دکتر طاعون"
        else -> "حساب کاربری"
    }

    val stepText = when (currentStep) {
        0 -> "از این نوار می‌تونید هر بازی یا برنامه‌ای رو سریع پیدا کنید!"
        1 -> "جدیدترین بازی‌ها و برنامه‌های ویژه رو از اینجا ببینید."
        2 -> "با این منو بین بخش‌های مختلف مثل بازی‌ها، برنامه‌ها و وبلاگ جابه‌جا بشید."
        3 -> "دستیار هوشمند ما همیشه آماده است تا در پیدا کردن برنامه‌ها کمکتون کنه."
        else -> "با کلیک روی این آیکون وارد حساب کاربری خود بشید."
    }

    val spotlightAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "spotlight"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(enabled = false) {} 
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addRoundRect(
                    RoundRect(
                        rect = targetRect,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
                fillType = PathFillType.EvenOdd
            }
            
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.2f * spotlightAlpha)
            )

            drawRoundRect(
                color = primaryColor, // 👈 استفاده از رنگ استخراج شده
                topLeft = targetRect.topLeft,
                size = targetRect.size,
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(
                    width = 2.dp.toPx(), 
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), 0f)
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .widthIn(max = 320.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحله ${currentStep + 1} از 5",
                color = primaryColor,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stepTitle,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stepText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (currentStep < 4) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(
                    text = if (currentStep == 4) "پایان" else "بعدی",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (currentStep < 4) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "رد کردن آموزش",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.clickable { onFinish() }
            )
        }
    }
}
