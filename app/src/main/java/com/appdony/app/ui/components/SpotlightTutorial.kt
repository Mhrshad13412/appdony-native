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
            highlightY = 100f,
            highlightHeight = 150f
        ),
        TutorialStep(
            title = "اپدونی پلاس",
            desc = "بازی‌های خاص و نسخه‌های ویژه (مثل مود یا اورجینال) همیشه بالای صفحه هستن.",
            highlightY = 300f,
            highlightHeight = 350f
        ),
        TutorialStep(
            title = "دسته‌بندی‌ها",
            desc = "اینجا بازی‌ها و برنامه‌ها به دسته‌های مختلفی مثل اکشن، پازل و... تقسیم شدن.",
            highlightY = 800f,
            highlightHeight = 450f
        ),
        TutorialStep(
            title = "دستیار شما (دکتر طاعون)",
            desc = "هر وقت سوالی داشتی یا خواستی سریع به دانلودها یا جستجو بری، روی من بزن!",
            highlightY = 1400f,
            highlightHeight = 300f
        ),
        TutorialStep(
            title = "منوی پایین",
            desc = "از این تب‌ها برای جابه‌جایی بین بازی‌ها، برنامه‌ها، وبلاگ و دانلودها استفاده کن.",
            highlightY = 1750f,
            highlightHeight = 300f
        )
    )

    val current = steps[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { if (step < steps.size - 1) step++ else onFinish() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val holeTopLeft = Offset(0f, current.highlightY)
            val holeSize = Size(size.width, current.highlightHeight)
            
            drawRoundRect(
                color = Color.Transparent,
                topLeft = holeTopLeft,
                size = holeSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            
            drawRoundRect(
                color = Color.White,
                topLeft = holeTopLeft,
                size = holeSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 50.dp, start = 24.dp, end = 24.dp)
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
