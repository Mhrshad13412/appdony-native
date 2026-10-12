package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun BlogScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "📝 وبلاگ", color = AppdonyTextPrimary, fontSize = 20.sp)
    }
}
