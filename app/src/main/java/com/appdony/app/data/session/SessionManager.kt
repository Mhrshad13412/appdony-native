package com.appdony.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "appdony_session")

class SessionManager(private val context: Context) {

    companion object {
        private val KEY_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val isLoggedIn: Flow<Boolean> =
        context.sessionDataStore.data.map { it[KEY_LOGGED_IN] ?: false }

    val username: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_USERNAME] ?: "" }

    val displayName: Flow<String> =
        context.sessionDataStore.data.map { it[KEY_DISPLAY_NAME] ?: "" }

    suspend fun saveSession(username: String, displayName: String) {
        context.sessionDataStore.edit {
            it[KEY_LOGGED_IN] = true
            it[KEY_USERNAME] = username
            it[KEY_DISPLAY_NAME] = displayName
        }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit {
            it[KEY_LOGGED_IN] = false
            it[KEY_USERNAME] = ""
            it[KEY_DISPLAY_NAME] = ""
        }
    }
}
