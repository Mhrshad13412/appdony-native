package com.appdony.app.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
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
    LaunchedEffect(Unit) {
        cacheSize = CacheUtils.getCacheSize(context)
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("تنظیمات", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        item { SettingsHeader("دستیار هوش مصنوعی", Icons.Default.SmartToy, primary) }
        item {
            SettingsSwitchRow("نمایش دکتر طاعون در صفحه", Icons.Default.Visibility, showAiAssistant) { checked ->
                scope.launch { settingsManager.setShowAiAssistant(checked) }
            }
        }

        item { SettingsHeader("ظاهر برنامه", Icons.Default.Palette, primary) }
        item {
            SettingsSwitchRow("حالت تاریک", Icons.Default.Brightness4, themeState.mode == ThemeMode.DARK) { checked ->
                scope.launch { themeManager.setMode(if (checked) ThemeMode.DARK else ThemeMode.LIGHT) }
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, null, tint = primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("رنگ برنامه", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeManager.themeColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(color)) // 👈 اصلاح شد
                                .clickable { scope.launch { themeManager.setColorIndex(index) } }
                        )
                    }
                }
            }
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FontDownload, null, tint = primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("اندازه فونت", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FontSize.values().forEach { size ->
                        val selected = themeState.fontSize == size
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { scope.launch { themeManager.setFontSize(size) } }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(size) {
                                    FontSize.SMALL -> "کوچک"
                                    FontSize.MEDIUM -> "متوسط"
                                    FontSize.LARGE -> "بزرگ"
                                },
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        item { SettingsHeader("دانلود و شبکه", Icons.Default.Download, primary) }
        item {
            SettingsSwitchRow("دانلود فقط با وای‌فای", Icons.Default.Download, wifiOnly) { checked ->
                scope.launch { settingsManager.setWifiOnly(checked) }
            }
        }

        item { SettingsHeader("اعلان‌ها", Icons.Default.Notifications, primary) }
        item {
            SettingsSwitchRow("اعلان پست‌های وبلاگ", Icons.AutoMirrored.Filled.Article, notifyBlog) { checked ->
                scope.launch { settingsManager.setNotifyBlog(checked) }
            }
        }
        item {
            SettingsSwitchRow("اعلان آپدیت برنامه‌ها", Icons.Default.Notifications, notifyUpdates) { checked ->
                scope.launch { settingsManager.setNotifyUpdates(checked) }
            }
        }

        item { SettingsHeader("داده‌ها", Icons.Default.CleaningServices, primary) }
        item {
            SettingsActionRow("پاک کردن حافظه کش", "$cacheSize", Icons.Default.CleaningServices) {
                scope.launch {
                    CacheUtils.clearCache(context)
                    cacheSize = CacheUtils.getCacheSize(context)
                    Toast.makeText(context, "کش پاک شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsActionRow("پاک کردن تاریخچه جستجو", "", Icons.Default.Search) {
                scope.launch {
                    historyManager.clearHistory() // 👈 اصلاح شد
                    Toast.makeText(context, "تاریخچه پاک شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsActionRow("پاک کردن علاقه‌مندی‌ها", "", Icons.Default.Favorite) {
                scope.launch {
                    favoritesManager.clear()
                    Toast.makeText(context, "علاقه‌مندی‌ها پاک شد", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            SettingsActionRow("پاک کردن تاریخچه چت", "", Icons.AutoMirrored.Filled.Chat) {
                scope.launch {
                    chatHistoryManager.clearHistory() // 👈 اصلاح شد
                    Toast.makeText(context, "تاریخچه چت پاک شد", Toast.LENGTH_SHORT).show()
                }
            }
        }

        item { SettingsHeader("حساب کاربری", Icons.AutoMirrored.Filled.Logout, primary) }
        item {
            SettingsActionRow("خروج از حساب", "", Icons.AutoMirrored.Filled.Logout, color = Color.Red) {
                scope.launch {
                    sessionManager.clearSession()
                    Toast.makeText(context, "از حساب خارج شدید", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun SettingsHeader(title: String, icon: ImageVector, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsSwitchRow(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}

@Composable
private fun SettingsActionRow(title: String, subtitle: String, icon: ImageVector, color: Color = MaterialTheme.colorScheme.onBackground, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = color, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (subtitle.isNotBlank()) {
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
}
