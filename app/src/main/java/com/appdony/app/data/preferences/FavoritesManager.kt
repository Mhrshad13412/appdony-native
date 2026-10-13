package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "appdony_favorites")

class FavoritesManager(private val context: Context) {

    companion object {
        private val KEY_FAVORITES = stringSetPreferencesKey("favorite_ids")
    }

    val favoriteIds: Flow<Set<Int>> = context.favoritesDataStore.data.map { prefs ->
        prefs[KEY_FAVORITES]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    suspend fun toggle(postId: Int) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES]?.toMutableSet() ?: mutableSetOf()
            val idStr = postId.toString()
            if (current.contains(idStr)) current.remove(idStr) else current.add(idStr)
            prefs[KEY_FAVORITES] = current
        }
    }

    suspend fun clear() {
        context.favoritesDataStore.edit { it.remove(KEY_FAVORITES) }
    }
}
