package com.appdony.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

    // مختصات تقریبی بخش‌هایی که می‌خواهیم هایلایت شوند
    val targetRect = when (currentStep) {
        0 -> Rect(Offset(screenWidthPx * 0.1f, screenHeightPx * 0.02f), Size(screenWidthPx * 0.8f, 60.dp.toPx())) // نوار جستجو
        1 -> Rect(Offset(screenWidthPx * 0.1f, screenHeightPx * 0.85f), Size(screenWidthPx * 0.8f, 80.dp.toPx())) // نوار پایین
        2 -> Rect(Offset(screenWidthPx * 0.75f, screenHeightPx * 0.75f), Size(70.dp.toPx(), 70.dp.toPx())) // دستیار دکتر طاعون
        else -> Rect(Offset(screenWidthPx * 0.1f, screenHeightPx * 0.1f), Size(screenWidthPx * 0.8f, 200.dp.toPx()))
    }

    val stepText = when (currentStep) {
        0 -> "از این نوار برای جستجوی سریع بازی‌ها و برنامه‌ها استفاده کنید."
        1 -> "با این منو می‌توانید بین بخش‌های مختلف مثل بازی‌ها، برنامه‌ها و وبلاگ جابه‌جا شوید."
        2 -> "دستیار هوشمند دکتر طاعون همیشه آماده است تا به شما کمک کند!"
        else -> "برای دسترسی به تنظیمات، دانلودها و پروفایل خود، روی آیکون کاربر کلیک کنید."
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
            .clickable(enabled = false) {} // جلوگیری از کلیک به پشت
    ) {
        // لایه تیره با سوراخ نورافکن
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                // کل صفحه رو پر کن
                addRect(Rect(Offset.Zero, size))
                // بخش هدف رو خالی کن (سوراخ)
                addRoundRect(
                    RoundRect(
                        rect = targetRect,
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                )
                // قانون EvenOdd باعث میشه بخش دوم از بخش اول کم بشه (سوراخ بشه)
                fillType = PathFillType.EvenOdd
            }
            
            // رنگ تیره برای کل صفحه به جز سوراخ
            drawPath(
                path = path,
                color = Color.Black.copy(alpha = 0.85f * spotlightAlpha)
            )

            // کشیدن بوردر دور بخش هدف
            drawRoundRect(
                color = MaterialTheme.colorScheme.primary,
                topLeft = targetRect.topLeft,
                size = targetRect.size,
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        // باکس متن و دکمه (با توجه به موقعیت هدف، جای متن رو تنظیم می‌کنیم)
        val cardOffsetY = if (targetRect.center.y > screenHeightPx / 2) {
            screenHeightPx * 0.3f // اگر هدف پایین بود، متن بالاتر بره
        } else {
            targetRect.bottom + 32.dp.toPx() // اگر هدف بالا بود، متن پایین‌تر بره
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(density) { cardOffsetY.toDp() })
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(20.dp)
        ) {
            Text(
                text = "راهنمای اپدونی (${currentStep + 1}/4)",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stepText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Right
            )
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "رد کردن",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    modifier = Modifier.clickable { onFinish() }
                )
                
                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep++
                        } else {
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = if (currentStep == 3) "متوجه شدم" else "بعدی",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    if (currentStep < 3) {
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
            }
        }
    }
}
