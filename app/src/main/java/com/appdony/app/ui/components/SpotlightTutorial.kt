package com.appdony.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            desc = "از این نوار می‌تونی هر بازی یا برنامه‌ای رو سریع پیدا کنی!",
            highlightY = 100f, // ارتفاع نوار جستجو
            highlightHeight = 150f
        ),
        TutorialStep(
            title = "دسته‌بندی‌ها",
            desc = "تب‌های پایین برای جابه‌جایی بین بازی‌ها، برنامه‌ها و وبلاگ هستن.",
            highlightY = 1700f, // ارتفاع منوی پایین
            highlightHeight = 300f
        ),
        TutorialStep(
            title = "دستیار اپدونی",
            desc = "دستیار ما (دکتر طاعون) همیشه پایین صفحه کنارت هست تا سریع بهت کمک کنه!",
            highlightY = 1500f,
            highlightHeight = 200f
        )
    )

    val current = steps[step]

    // لایه اصلی که کل صفحه رو می‌پوشونه
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)) // ماسک تیره
            .clickable { if (step < steps.size - 1) step++ else onFinish() },
        contentAlignment = Alignment.Center
    ) {
        // رسم مربع شفاف (نورافکن) روی بخش مورد نظر
        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeTopLeft = Offset(0f, current.highlightY)
            val holeSize = Size(size.width, current.highlightHeight)
            
            // رسم مستطیل شفاف با گوشه گرد
            drawRoundRect(
                color = Color.Transparent,
                topLeft = holeTopLeft,
                size = holeSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            
            // کشیدن حاشیه دور نورافکن (اختیاری برای زیبایی)
            drawRoundRect(
                color = Color.White,
                topLeft = holeTopLeft,
                size = holeSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
        }

        // کارت توضیحات که همیشه پایین صفحه ثابت می‌مونه
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحله ${step + 1} از ${steps.size}",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = current.title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = current.desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { if (step < steps.size - 1) step++ else onFinish() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (step == steps.size - 1) "متوجه شدم، بریم!" else "بعدی",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
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
