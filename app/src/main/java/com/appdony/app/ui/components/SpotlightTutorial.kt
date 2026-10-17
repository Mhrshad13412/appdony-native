package com.appdony.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
        TutorialStep("جستجو", "از این نوار می‌تونی هر بازی یا برنامه‌ای رو سریع پیدا کنی!", 100f, 120f),
        TutorialStep("اپدونی پلاس", "بازی‌های خاص و نسخه‌های ویژه (مثل مود یا اورجینال) همیشه بالای صفحه هستن.", 300f, 250f),
        TutorialStep("دسته‌بندی‌ها", "اینجا بازی‌ها و برنامه‌ها به دسته‌های مختلفی مثل اکشن، پازل و... تقسیم شدن.", 800f, 350f),
        TutorialStep("دستیار شما", "هر وقت سوالی داشتی یا خواستی سریع به دانلودها یا جستجو بری، روی من بزن!", 1500f, 250f),
        TutorialStep("منوی پایین", "از این تب‌ها برای جابه‌جایی بین بازی‌ها، برنامه‌ها، وبلاگ و دانلودها استفاده کن.", 1750f, 250f)
    )

    val current = steps[step]
    val yAnim by animateFloatAsState(targetValue = current.highlightY, animationSpec = tween(400), label = "y_anim")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { if (step < steps.size - 1) step++ else onFinish() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(0f, yAnim),
                size = Size(size.width, current.highlightHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(0f, yAnim),
                size = Size(size.width, current.highlightHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
                style = Stroke(width = 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f)))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
                .offset(y = (current.highlightY + current.highlightHeight + 20f).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Text("مرحله ${step + 1} از ${steps.size}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
            Text(current.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(current.desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (step < steps.size - 1) step++ else onFinish() },
                modifier = Modifier.align(Alignment.End),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (step == steps.size - 1) "شروع" else "بعدی", color = Color.White)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

data class TutorialStep(val title: String, val desc: String, val highlightY: Float, val highlightHeight: Float)
