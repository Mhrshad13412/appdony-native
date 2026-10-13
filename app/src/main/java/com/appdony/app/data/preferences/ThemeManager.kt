package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "appdony_theme")

enum class ThemeMode { DARK, LIGHT }

enum class FontSize(val scale: Float) {
    SMALL(0.85f),
    MEDIUM(1.0f),
    LARGE(1.2f)
}

data class ThemeState(
    val mode: ThemeMode = ThemeMode.DARK,
    val colorIndex: Int = 0,
    val fontSize: FontSize = FontSize.MEDIUM
)

class ThemeManager(private val context: Context) {

    companion object {
        private val KEY_MODE = stringPreferencesKey("theme_mode")
        private val KEY_COLOR_INDEX = intPreferencesKey("theme_color_index")
        private val KEY_FONT_SIZE = stringPreferencesKey("font_size")

        // پالت رنگ‌ها
        val themeColors = listOf(
            0xFF0D7377, // سبز اپدونی (پیش‌فرض)
            0xFF1976D2, // آبی
            0xFF7B1FA2, // بنفش
            0xFFC2185B, // صورتی
            0xFFD32F2F, // قرمز
            0xFF5D4037, // قهوه‌ای
            0xFF455A64  // خاکستری
        )

        val colorNames = listOf(
            "سبز اپدونی",
            "آبی",
            "بنفش",
            "صورتی",
            "قرمز",
            "قهوه‌ای",
            "خاکستری"
        )
    }

    val state: Flow<ThemeState> = context.themeDataStore.data.map { prefs ->
        ThemeState(
            mode = when (prefs[KEY_MODE]) {
                "light" -> ThemeMode.LIGHT
                else -> ThemeMode.DARK
            },
            colorIndex = prefs[KEY_COLOR_INDEX] ?: 0,
            fontSize = when (prefs[KEY_FONT_SIZE]) {
                "small" -> FontSize.SMALL
                "large" -> FontSize.LARGE
                else -> FontSize.MEDIUM
            }
        )
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit {
            it[KEY_MODE] = if (mode == ThemeMode.LIGHT) "light" else "dark"
        }
    }

    suspend fun setColorIndex(index: Int) {
        context.themeDataStore.edit { it[KEY_COLOR_INDEX] = index }
    }

    suspend fun setFontSize(size: FontSize) {
        context.themeDataStore.edit {
            it[KEY_FONT_SIZE] = when (size) {
                FontSize.SMALL -> "small"
                FontSize.LARGE -> "large"
                else -> "medium"
            }
        }
    }
}
