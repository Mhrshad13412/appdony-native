package com.appdony.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appdony.app.data.preferences.*
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
    val chatHistoryManager = remember { ChatHistoryManager(context) } // 👈 جدید
    
    val scope = rememberCoroutineScope()
    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val primary = MaterialTheme.colorScheme.primary

    // تنظیمات
    val wifiOnly by settingsManager.wifiOnly.collectAsStateWithLifecycle(initialValue = false)
    val notifyBlog by settingsManager.notifyBlog.collectAsStateWithLifecycle(initialValue = true)
    val notifyUpdates by settingsManager.notifyUpdates.collectAsStateWithLifecycle(initialValue = true)
    val showAiAssistant by settingsManager.showAiAssistant.collectAsStateWithLifecycle(initialValue = true) // 👈 جدید

    var cacheSize by remember { mutableStateOf("در حال محاسبه...") }
    LaunchedEffect(Unit) { cacheSize = CacheUtils.getCacheSize(context) }

    LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onBack() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.width(8.dp))
                Text("تنظیمات", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 🤖 بخش هوش مصنوعی (جدید)
        item { SettingsHeader("دستیار هوش مصنوعی", Icons.Default.SmartToy, primary) }
        item {
            SettingsSwitchRow("نمایش دکتر طاعون در صفحه", Icons.Default.Visibility, showAiAssistant) { checked ->
                scope.launch { settingsManager.setShowAiAssistant(checked) }
            }
        }
        item {
            SettingsActionRow("پاک کردن تاریخچه چت", "حذف تمام مکالمات با دکتر طاعون", Icons.Default.DeleteSweep) {
                scope.launch {
                    chatHistoryManager.clearHistory()
                    Toast.makeText(context, "تاریخچه چت پاک شد", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // 🎨 تم و ظاهر
        item { SettingsHeader("ظاهر اپلیکیشن", Icons.Default.Palette, primary) }
        item {
            SettingsSwitchRow("حالت تاریک", Icons.Default.DarkMode, themeState.mode == ThemeMode.DARK) { checked ->
                scope.launch { themeManager.setMode(if (checked) ThemeMode.DARK else ThemeMode.LIGHT) }
            }
        }
        item { ThemeColorsRow(themeState.colorIndex, primary) { index -> scope.launch { themeManager.setColorIndex(index) } } }
        item {
            SettingsDropdownRow("سایز فونت", Icons.Default.FormatSize, themeState.fontSize.name) { expanded ->
                Column {
                    FontSize.values().forEach { size ->
                        DropdownMenuItem(text = { Text(size.name) }, onClick = { scope.launch { themeManager.setFontSize(size) }; expanded(false) })
                    }
                }
            }
        }

        // 📥 دانلود
        item { SettingsHeader("دانلود", Icons.Default.Download, primary) }
        item {
            SettingsSwitchRow("دانلود فقط با وای‌فای", Icons.Default.Wifi, wifiOnly) { checked ->
                scope.launch { settingsManager.setWifiOnly(checked) }
            }
        }

        // 🧹 حافظه
        item { SettingsHeader("حافظه و داده‌ها", Icons.Default.Storage, primary) }
        item {
            SettingsActionRow("پاک کردن حافظه کش", "حجم فعلی: $cacheSize", Icons.Default.Delete) {
                scope.launch {
                    CacheUtils.clearCache(context)
                    cacheSize = CacheUtils.getCacheSize(context)
                }
            }
        }
        item {
            SettingsActionRow("پاک کردن تاریخچه جستجو", "پاک کردن کلمات جستجو شده", Icons.Default.History) {
                scope.launch { historyManager.clearHistory() }
            }
        }
        item {
            SettingsActionRow("پاک کردن علاقه‌مندی‌ها", "حذف تمام برنامه‌های ذخیره شده", Icons.Default.FavoriteBorder) {
                scope.launch { favoritesManager.clear() }
            }
        }

        // 🔔 اعلان‌ها
        item { SettingsHeader("اعلان‌ها", Icons.Default.Notifications, primary) }
        item {
            SettingsSwitchRow("اعلان وبلاگ", Icons.Default.Article, notifyBlog) { checked ->
                scope.launch { settingsManager.setNotifyBlog(checked) }
            }
        }
        item {
            SettingsSwitchRow("اعلان آپدیت برنامه", Icons.Default.Update, notifyUpdates) { checked ->
                scope.launch { settingsManager.setNotifyUpdates(checked) }
            }
        }

        // 👤 حساب کاربری
        item { SettingsHeader("حساب کاربری", Icons.Default.Person, primary) }
        item {
            SettingsActionRow("خروج از حساب", "پاک کردن اطلاعات ورود", Icons.Default.Logout) {
                scope.launch { sessionManager.clearSession() }
            }
        }
        
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun SettingsHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SettingsSwitchRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
fun SettingsActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ThemeColorsRow(selectedIndex: Int, primary: Color, onColorSelected: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        ThemeManager.themeColors.forEachIndexed { index, color ->
            Box(Modifier.size(32.dp).clip(CircleShape).background(Color(color)).clickable { onColorSelected(index) }, contentAlignment = Alignment.Center) {
                if (selectedIndex == index) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun SettingsDropdownRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, currentValue: String, items: @Composable (expanded: (Boolean) -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 14.sp)
            Text(currentValue, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items { expanded = it }
        }
    }
}
