package com.appdony.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appdony.app.data.api.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "appdony_session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USER_ID = intPreferencesKey("user_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        private val KEY_EMAIL = stringPreferencesKey("email")
        private val KEY_AVATAR = stringPreferencesKey("avatar_url")
        private val KEY_NONCE = stringPreferencesKey("nonce")
    }

    val isLoggedIn: Flow<Boolean> =
        context.sessionDataStore.data.map { it[KEY_LOGGED_IN] ?: false }

    val username: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_USERNAME] ?: "" }

    val displayName: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_DISPLAY_NAME] ?: "" }

    val userEmail: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_EMAIL] ?: "" }

    val avatarUrl: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_AVATAR] ?: "" }

    val nonce: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_NONCE] ?: "" }

    suspend fun saveSession(
        userId: Int,
        username: String,
        displayName: String,
        email: String,
        avatarUrl: String,
        nonce: String
    ) {
        context.sessionDataStore.edit {
            it[KEY_LOGGED_IN] = true
            it[KEY_USER_ID] = userId
            it[KEY_USERNAME] = username
            it[KEY_DISPLAY_NAME] = displayName
            it[KEY_EMAIL] = email
            it[KEY_AVATAR] = avatarUrl
            it[KEY_NONCE] = nonce
        }
    }

    suspend fun updateNonce(newNonce: String) {
        context.sessionDataStore.edit {
            it[KEY_NONCE] = newNonce
        }
    }

    suspend fun clearSession() {
        // کوکی‌ها رو هم پاک کن
        ApiClient.clearCookies()

        context.sessionDataStore.edit {
            it[KEY_LOGGED_IN] = false
            it[KEY_USER_ID] = 0
            it[KEY_USERNAME] = ""
            it[KEY_DISPLAY_NAME] = ""
            it[KEY_EMAIL] = ""
            it[KEY_AVATAR] = ""
            it[KEY_NONCE] = ""
        }
    }
}
