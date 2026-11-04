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

        // رنگ ثابت اپدونی (انتخاب رنگ حذف شده؛ همه‌جا همین رنگ سایته)
        val themeColors = listOf(
            0xFF4BA98F // سبز اپدونی
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
