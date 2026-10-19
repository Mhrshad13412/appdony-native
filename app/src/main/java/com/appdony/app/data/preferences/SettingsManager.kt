package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

class SettingsManager(private val context: Context) {

    companion object {
        private val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        private val KEY_NOTIFY_BLOG = booleanPreferencesKey("notify_blog")
        private val KEY_NOTIFY_UPDATES = booleanPreferencesKey("notify_updates")
        private val KEY_START_PAGE = stringPreferencesKey("start_page")
    }

    val wifiOnly: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_WIFI_ONLY] ?: false }
    val notifyBlog: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIFY_BLOG] ?: true }
    val notifyUpdates: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_NOTIFY_UPDATES] ?: true }
    val startPage: Flow<String> = context.settingsDataStore.data.map { it[KEY_START_PAGE] ?: "games" }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_WIFI_ONLY] = enabled }
    }

    suspend fun setNotifyBlog(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFY_BLOG] = enabled }
    }

    suspend fun setNotifyUpdates(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_NOTIFY_UPDATES] = enabled }
    }

    suspend fun setStartPage(page: String) {
        context.settingsDataStore.edit { it[KEY_START_PAGE] = page }
    }
}
