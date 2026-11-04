package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_NOTIFY_BLOG = booleanPreferencesKey("notify_blog")
        private val KEY_NOTIFY_UPDATES = booleanPreferencesKey("notify_updates")
        private val KEY_SHOW_AI = booleanPreferencesKey("show_ai_assistant") // 👈 جدید
        private val KEY_UI_STYLE = stringPreferencesKey("ui_style") // "glass" | "classic"
        private val KEY_BETA = booleanPreferencesKey("beta_enabled")
        private val KEY_FIXED_AI = booleanPreferencesKey("fixed_ai") // دکمه‌ی ثابت هوش مصنوعی وسط منو
        private val KEY_AI_CORNER = booleanPreferencesKey("ai_corner") // دکمه‌ی ثابت هوش مصنوعی گوشه‌ی صفحه

        const val STYLE_GLASS = "glass"
        const val STYLE_CLASSIC = "classic"
    }

    val wifiOnly: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_WIFI_ONLY] ?: false }
    val notifyBlog: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIFY_BLOG] ?: true }
    val notifyUpdates: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIFY_UPDATES] ?: true }
    val showAiAssistant: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_SHOW_AI] ?: true } // 👈 جدید

    // پیش‌فرض کلاسیکه؛ شیشه‌ای فقط وقتی کاربر خودش از تنظیمات انتخاب کنه
    val uiStyle: Flow<String> = context.settingsDataStore.data.map { it[KEY_UI_STYLE] ?: STYLE_CLASSIC }

    // بتا: پیش‌فرض خاموش؛ وقتی روشن باشه بلوک‌های اپدونی پلاس نشون داده می‌شن
    val beta: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_BETA] ?: false }

    // هوش مصنوعی ثابت: پیش‌فرض روشن؛ دکمه‌ی گرد هوش مصنوعی وسط منوی پایین (شیشه‌ای و کلاسیک)
    val fixedAi: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_FIXED_AI] ?: true }

    // هوش مصنوعی ثابت گوشه (مثل دکمه‌ی پشتیبانی تلگرام ادز): پیش‌فرض خاموش
    val aiCorner: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_AI_CORNER] ?: false }

    suspend fun setAiCorner(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_AI_CORNER] = enabled }
    }

    suspend fun setFixedAi(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_FIXED_AI] = enabled }
    }

    suspend fun setBeta(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_BETA] = enabled }
    }

    suspend fun setUiStyle(style: String) {
        context.settingsDataStore.edit { it[KEY_UI_STYLE] = style }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setNotifyBlog(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFY_BLOG] = enabled }
    }

    suspend fun setNotifyUpdates(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFY_UPDATES] = enabled }
    }

    suspend fun setShowAiAssistant(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_SHOW_AI] = enabled }
    }
}
