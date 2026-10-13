package com.appdony.app.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg)
    ) {
        // نوار بالا
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = AppdonyTextPrimary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "تنظیمات",
                    color = AppdonyTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // حالت شب/روز
        item {
            SettingsSection(title = "حالت نمایش", icon = Icons.Default.DarkMode)
            SettingsOption(icon = Icons.Default.DarkMode, title = "تیره", selected = true, onClick = {})
            SettingsOption(icon = Icons.Default.LightMode, title = "روشن", selected = false, onClick = {})
            Spacer(Modifier.height(16.dp))
        }

        // تم رنگی
        item {
            SettingsSection(title = "رنگ تم", icon = Icons.Default.Palette)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val colors = listOf(
                    0xFF0D7377, // سبز اپدونی
                    0xFF1976D2, // آبی
                    0xFF7B1FA2, // بنفش
                    0xFFC2185B, // صورتی
                    0xFFD32F2F, // قرمز
                    0xFF5D4037, // قهوه‌ای
                    0xFF455A64  // خاکستری
                )
                colors.forEachIndexed { index, color ->
                    ColorCircle(
                        color = Color(color),
                        selected = index == 0,
                        onClick = {}
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // سایز فونت
        item {
            SettingsSection(title = "سایز فونت", icon = Icons.Default.FormatSize)
            SettingsOption(title = "کوچک", selected = false, onClick = {})
            SettingsOption(title = "متوسط", selected = true, onClick = {})
            SettingsOption(title = "بزرگ", selected = false, onClick = {})
            Spacer(Modifier.height(16.dp))
        }

        // درباره
        item {
            SettingsSection(title = "درباره", icon = Icons.Default.Check)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppdonyCardBg)
                    .padding(16.dp)
            ) {
                Text(
                    text = "اپدونی",
                    color = AppdonyTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "نسخه 2.5.33",
                    color = AppdonyTextMuted,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "مرجع دانلود اپلیکیشن و بازی اندروید",
                    color = AppdonyTextMuted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "appdony.ir",
                    color = AppdonyGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppdonyGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = AppdonyTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppdonyCardBg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppdonyTextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            color = AppdonyTextPrimary,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppdonyGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ColorCircle(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
