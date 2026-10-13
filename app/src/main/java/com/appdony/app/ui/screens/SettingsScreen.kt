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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val scope = rememberCoroutineScope()
    val state by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val primary = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                        Icons.AutoMirrored.Filled.ArrowBack,
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

        // حالت نمایش
        item {
            SettingsSection(title = "حالت نمایش", icon = Icons.Default.DarkMode, tint = primary)
            SettingsOption(
                icon = Icons.Default.DarkMode,
                title = "تیره",
                selected = state.mode == ThemeMode.DARK,
                onClick = { scope.launch { themeManager.setMode(ThemeMode.DARK) } },
                selectedColor = primary
            )
            SettingsOption(
                icon = Icons.Default.LightMode,
                title = "روشن",
                selected = state.mode == ThemeMode.LIGHT,
                onClick = { scope.launch { themeManager.setMode(ThemeMode.LIGHT) } },
                selectedColor = primary
            )
            Spacer(Modifier.height(16.dp))
        }

        // تم رنگی
        item {
            SettingsSection(title = "رنگ تم", icon = Icons.Default.Palette, tint = primary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ThemeManager.themeColors.forEachIndexed { index, colorLong ->
                    ColorCircle(
                        color = Color(colorLong),
                        selected = state.colorIndex == index,
                        onClick = {
                            scope.launch { themeManager.setColorIndex(index) }
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // سایز فونت
        item {
            SettingsSection(title = "سایز فونت", icon = Icons.Default.FormatSize, tint = primary)
            SettingsOption(
                title = "کوچک",
                selected = state.fontSize == FontSize.SMALL,
                onClick = { scope.launch { themeManager.setFontSize(FontSize.SMALL) } },
                selectedColor = primary
            )
            SettingsOption(
                title = "متوسط",
                selected = state.fontSize == FontSize.MEDIUM,
                onClick = { scope.launch { themeManager.setFontSize(FontSize.MEDIUM) } },
                selectedColor = primary
            )
            SettingsOption(
                title = "بزرگ",
                selected = state.fontSize == FontSize.LARGE,
                onClick = { scope.launch { themeManager.setFontSize(FontSize.LARGE) } },
                selectedColor = primary
            )
            Spacer(Modifier.height(16.dp))
        }

        // درباره
        item {
            SettingsSection(title = "درباره", icon = Icons.Default.Info, tint = primary)
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
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
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
    selectedColor: Color,
    icon: ImageVector? = null
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
                tint = selectedColor,
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
