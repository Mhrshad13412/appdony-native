package com.appdony.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.appdony.app.data.preferences.ChatHistoryManager
import com.appdony.app.data.preferences.FavoritesManager
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.SearchHistoryManager
import com.appdony.app.data.preferences.SettingsManager
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.session.SessionManager
import com.appdony.app.util.CacheUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    val historyManager = remember { SearchHistoryManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    val chatHistoryManager = remember { ChatHistoryManager(context) }
    val scope = rememberCoroutineScope()
    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val primary = MaterialTheme.colorScheme.primary
    val wifiOnly by settingsManager.wifiOnly.collectAsStateWithLifecycle(initialValue = false)
    val notifyBlog by settingsManager.notifyBlog.collectAsStateWithLifecycle(initialValue = true)
    val notifyUpdates by settingsManager.notifyUpdates.collectAsStateWithLifecycle(initialValue = true)
    val showAiAssistant by settingsManager.showAiAssistant.collectAsStateWithLifecycle(initialValue = true)
    var cacheSize by remember { mutableStateOf("در حال محاسبه...") }

    LaunchedEffect(Unit) { cacheSize = CacheUtils.getCacheSize(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("تنظیمات", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            SettingsGroup("ظاهر برنامه") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeTile(
                        title = "تاریک",
                        icon = Icons.Outlined.DarkMode,
                        selected = themeState.mode == ThemeMode.DARK,
                        modifier = Modifier.weight(1f)
                    ) { scope.launch { themeManager.setMode(ThemeMode.DARK) } }
                    ModeTile(
                        title = "روشن",
                        icon = Icons.Outlined.LightMode,
                        selected = themeState.mode == ThemeMode.LIGHT,
                        modifier = Modifier.weight(1f)
                    ) { scope.launch { themeManager.setMode(ThemeMode.LIGHT) } }
                }
                RowDivider()

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Palette, null, tint = primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("رنگ برنامه", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(
                            ThemeManager.colorNames.getOrElse(themeState.colorIndex) { "" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeManager.themeColors.forEachIndexed { index, colorValue ->
                            ColorDot(
                                color = Color(colorValue),
                                selected = themeState.colorIndex == index
                            ) { scope.launch { themeManager.setColorIndex(index) } }
                        }
                    }
                }
                RowDivider()

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FormatSize, null, tint = primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("اندازه فونت", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FontSize.values().forEach { size ->
                            val selected = themeState.fontSize == size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (selected) primary else MaterialTheme.colorScheme.background)
                                    .border(1.dp, if (selected) primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    .clickable { scope.launch { themeManager.setFontSize(size) } }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (size) {
                                        FontSize.SMALL -> "کوچک"
                                        FontSize.MEDIUM -> "متوسط"
                                        FontSize.LARGE -> "بزرگ"
                                    },
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsGroup("دستیار هوش مصنوعی") {
                SettingsSwitchRow("نمایش دکتر طاعون", Icons.Outlined.Visibility, showAiAssistant) {
                    scope.launch { settingsManager.setShowAiAssistant(it) }
                }
            }
        }

        item {
            SettingsGroup("دانلود و اعلان‌ها") {
                SettingsSwitchRow("دانلود فقط با وای‌فای", Icons.Outlined.Download, wifiOnly) {
                    scope.launch { settingsManager.setWifiOnly(it) }
                }
                RowDivider()
                SettingsSwitchRow("اعلان وبلاگ", Icons.Outlined.Notifications, notifyBlog) {
                    scope.launch { settingsManager.setNotifyBlog(it) }
                }
                RowDivider()
                SettingsSwitchRow("اعلان آپدیت", Icons.Outlined.Update, notifyUpdates) {
                    scope.launch { settingsManager.setNotifyUpdates(it) }
                }
            }
        }

        item {
            SettingsGroup("داده‌ها") {
                SettingsActionRow("پاک کردن حافظه کش", cacheSize, Icons.Outlined.DeleteOutline) {
                    scope.launch {
                        CacheUtils.clearCache(context)
                        cacheSize = CacheUtils.getCacheSize(context)
                        Toast.makeText(context, "کش پاک شد", Toast.LENGTH_SHORT).show()
                    }
                }
                RowDivider()
                SettingsActionRow("پاک کردن تاریخچه جستجو", "", Icons.Outlined.Search) {
                    scope.launch {
                        historyManager.clearHistory()
                        Toast.makeText(context, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show()
                    }
                }
                RowDivider()
                SettingsActionRow("پاک کردن علاقه‌مندی‌ها", "", Icons.Outlined.FavoriteBorder) {
                    scope.launch {
                        favoritesManager.clear()
                        Toast.makeText(context, "علاقه‌مندی‌ها پاک شد", Toast.LENGTH_SHORT).show()
                    }
                }
                RowDivider()
                SettingsActionRow("پاک کردن تاریخچه چت", "", Icons.Outlined.ChatBubbleOutline) {
                    scope.launch {
                        chatHistoryManager.clearHistory()
                        Toast.makeText(context, "تاریخچه چت پاک شد", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        item {
            SettingsGroup("حساب کاربری") {
                SettingsActionRow("خروج از حساب", "", Icons.AutoMirrored.Filled.ExitToApp, color = MaterialTheme.colorScheme.error) {
                    scope.launch {
                        sessionManager.clearSession()
                        Toast.makeText(context, "از حساب خارج شدید", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), RoundedCornerShape(20.dp)),
            content = content
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    )
}

@Composable
private fun ModeTile(title: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.background)
            .border(1.5.dp, if (selected) primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SettingsSwitchRow(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.background,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (color == MaterialTheme.colorScheme.error) color else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = color, fontSize = 14.sp)
            if (subtitle.isNotBlank()) Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}
