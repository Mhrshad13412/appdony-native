package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.onboardingDataStore by preferencesDataStore(name = "appdony_onboarding")

class OnboardingManager(private val context: Context) {

    companion object {
        private val KEY_HAS_SEEN = booleanPreferencesKey("has_seen_onboarding")
    }

    val hasSeenOnboarding: Flow<Boolean> = context.onboardingDataStore.data.map {
        it[KEY_HAS_SEEN] ?: false
    }

    suspend fun setSeen() {
        context.onboardingDataStore.edit {
            it[KEY_HAS_SEEN] = true
        }
    }
}
