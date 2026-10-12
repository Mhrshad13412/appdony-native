package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🏠", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "صفحه خانه", color = AppdonyTextPrimary, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "محتوای ویژه اینجا نمایش داده می‌شود",
                color = AppdonyTextMuted,
                fontSize = 14.sp
            )
        }
    }
}
