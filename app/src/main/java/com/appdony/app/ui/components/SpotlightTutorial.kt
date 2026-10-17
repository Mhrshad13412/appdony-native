package com.appdony.app.ui.components

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
        TutorialStep("جستجو", "از این نوار می‌تونی هر بازی یا برنامه‌ای رو سریع پیدا کنی!"),
        TutorialStep("دسته‌بندی‌ها", "تب‌های پایین برای جابه‌جایی بین بازی‌ها، برنامه‌ها و وبلاگ هستن."),
        TutorialStep("دستیار اپدونی", "دستیار ما (دکتر طاعون) همیشه پایین صفحه کنارت هست تا سریع بهت کمک کنه!")
    )

    val current = steps[step]

    // پس‌زمینه تیره که کل صفحه رو می‌پوشونه
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { if (step < steps.size - 1) step++ else onFinish() },
        contentAlignment = Alignment.Center
    ) {
        // کارت توضیحات
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // شخصیت دکتر طاعون
            PlagueDoctorAvatar(sizeDp = 120)
            
            Spacer(modifier = Modifier.height(16.dp))

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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
    val desc: String
)
