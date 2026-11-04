package com.appdony.app.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.appdony.app.ui.components.AiMode
import com.appdony.app.ui.components.AiModeSelector
import com.appdony.app.ui.components.PillOption
import com.appdony.app.ui.components.PillSelector
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
import com.appdony.app.util.CatOverlayService
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
    val fixedAi by settingsManager.fixedAi.collectAsStateWithLifecycle(initialValue = true)
    val aiCorner by settingsManager.aiCorner.collectAsStateWithLifecycle(initialValue = false)
    val beta by settingsManager.beta.collectAsStateWithLifecycle(initialValue = false)
    val uiStyle by settingsManager.uiStyle.collectAsStateWithLifecycle(initialValue = SettingsManager.STYLE_CLASSIC)
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    var cacheSize by remember { mutableStateOf("در حال محاسبه...") }
    var overlayOn by remember {
        mutableStateOf(CatOverlayService.isEnabled(context) && CatOverlayService.canDraw(context))
    }

    LaunchedEffect(Unit) { cacheSize = CacheUtils.getCacheSize(context) }

    // حالت فعلی دستیار از روی تنظیم‌ها درمیاد (مثل دنده‌ی اتومات: همیشه فقط یکی فعاله)
    val aiMode = when {
        aiCorner -> AiMode.CORNER
        fixedAi -> AiMode.MENU
        showAiAssistant && overlayOn -> AiMode.OUTSIDE
        showAiAssistant -> AiMode.FLOATING
        else -> AiMode.OFF
    }
    val applyAiMode: (AiMode) -> Unit = { m ->
        scope.launch {
            settingsManager.setShowAiAssistant(m == AiMode.MENU || m == AiMode.FLOATING || m == AiMode.OUTSIDE)
            settingsManager.setFixedAi(m == AiMode.MENU)
            settingsManager.setAiCorner(m == AiMode.CORNER)
        }
        val wantOverlay = m == AiMode.OUTSIDE
        if (wantOverlay != overlayOn) {
            overlayOn = wantOverlay
            if (wantOverlay && !CatOverlayService.canDraw(context)) {
                // اول اجازه‌ی «نمایش روی سایر برنامه‌ها»؛ بعد از برگشتن، سرویس خودش بالا میاد
                context.startActivity(
                    CatOverlayService.permissionIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            CatOverlayService.setEnabled(context, wantOverlay)
        }
    }

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
                PillSelector(
                    options = listOf(
                        PillOption("تاریک", "پس‌زمینه‌ی تیره؛ برای چشم راحت‌تر، مخصوصاً شب‌ها.", Icons.Outlined.DarkMode),
                        PillOption("روشن", "پس‌زمینه‌ی روشن؛ مناسب روز و نور زیاد.", Icons.Outlined.LightMode)
                    ),
                    selectedIndex = if (themeState.mode == ThemeMode.DARK) 0 else 1,
                    onSelect = { i -> scope.launch { themeManager.setMode(if (i == 0) ThemeMode.DARK else ThemeMode.LIGHT) } }
                )
                RowDivider()

                val fontSizes = FontSize.values()
                PillSelector(
                    options = listOf(
                        PillOption("فونت کوچک", "نوشته‌ها کوچیک‌تر می‌شن و جای بیشتری روی صفحه جا می‌شه.", Icons.Outlined.FormatSize, 18.dp),
                        PillOption("فونت متوسط", "اندازه‌ی پیش‌فرض و متعادل برنامه.", Icons.Outlined.FormatSize, 24.dp),
                        PillOption("فونت بزرگ", "نوشته‌ها درشت‌تر می‌شن تا راحت‌تر خونده بشن.", Icons.Outlined.FormatSize, 30.dp)
                    ),
                    selectedIndex = fontSizes.indexOf(themeState.fontSize).coerceAtLeast(0),
                    onSelect = { i -> scope.launch { themeManager.setFontSize(fontSizes[i]) } }
                )
            }
        }

        item {
            SettingsGroup("حالت") {
                PillSelector(
                    options = listOf(
                        PillOption("شیشه‌ای", "منوی پایین نیمه‌شفاف و محو، مثل شیشه.", Icons.Outlined.Palette),
                        PillOption("کلاسیک", "منوی پایین ساده و مات، مثل گوگل پلی.", Icons.Outlined.Visibility)
                    ),
                    selectedIndex = if (uiStyle == SettingsManager.STYLE_GLASS) 0 else 1,
                    onSelect = { i ->
                        scope.launch {
                            settingsManager.setUiStyle(if (i == 0) SettingsManager.STYLE_GLASS else SettingsManager.STYLE_CLASSIC)
                        }
                    }
                )
            }
        }

        item {
            SettingsGroup("آزمایشی") {
                SettingsSwitchRow("بتا (نمایش اپدونی پلاس)", Icons.Outlined.Visibility, beta) {
                    scope.launch { settingsManager.setBeta(it) }
                }
            }
        }

        item {
            SettingsGroup("دستیار هوش مصنوعی") {
                AiModeSelector(selected = aiMode, onSelect = applyAiMode)
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

        // «خروج از حساب» فقط برای کاربرِ واردشده؛ مهمان این بخش رو نمی‌بینه
        if (isLoggedIn) {
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
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val glass = com.appdony.app.ui.components.LocalGlassMode.current
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
                .then(
                    if (glass) Modifier.border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), RoundedCornerShape(20.dp))
                    else Modifier
                ),
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
