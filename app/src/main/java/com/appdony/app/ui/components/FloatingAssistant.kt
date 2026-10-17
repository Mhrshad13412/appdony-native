package com.appdony.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpotlightTutorial(
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(0) }

    val steps = listOf(
        TutorialStep(
            title = "جستجو",
            desc = "از اینجا می‌تونی هر بازی یا برنامه‌ای رو سریع پیدا کنی!",
            highlightY = 150f, // مختصات تقریبی تاپ بار
            highlightHeight = 200f
        ),
        TutorialStep(
            title = "دسته‌بندی‌ها",
            desc = "تب‌های پایین برای جابه‌جایی بین بازی‌ها، برنامه‌ها و وبلاگ هستن.",
            highlightY = 1750f, // مختصات تقریبی منوی پایین
            highlightHeight = 350f
        )
    )

    val current = steps[step]

    val yAnim by animateFloatAsState(
        targetValue = current.highlightY,
        animationSpec = tween(500),
        label = "y_anim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawWithContent {
                drawContent()
                // تیره کردن کل صفحه
                drawRect(color = Color.Black.copy(alpha = 0.85f))
                // ایجاد حفره شفاف (نورافکن)
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(0f, yAnim),
                    size = Size(size.width, current.highlightHeight),
                    cornerRadius = CornerRadius(40f, 40f),
                    blendMode = BlendMode.Clear
                )
            }
            .clickable {
                if (step < steps.size - 1) step++ else onFinish()
            }
    ) {
        // شخصیت و کارت توضیحات در پایین صفحه
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 👇 استفاده از شخصیت دکتر طاعون که خودمون ساختیم
                Box(
                    modifier = Modifier.offset(y = 10.dp)
                ) {
                    PlagueDoctorAvatar(sizeDp = 110)
                }

                Spacer(modifier = Modifier.width(10.dp))

                // کارت توضیحات
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "مرحله ${step + 1} از ${steps.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = current.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = current.desc,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (step < steps.size - 1) step++ else onFinish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            text = if (step == steps.size - 1) "شروع" else "بعدی",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

data class TutorialStep(
    val title: String,
    val desc: String,
    val highlightY: Float,
    val highlightHeight: Float
)
