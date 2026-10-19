package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.historyDataStore by preferencesDataStore(name = "search_history")

class SearchHistoryManager(private val context: Context) {

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("history_list")
    }

    private val gson = Gson()

    // خواندن لیست تاریخچه
    val history: Flow<List<String>> = context.historyDataStore.data.map { prefs ->
        val json = prefs[KEY_HISTORY] ?: "[]"
        val type = object : TypeToken<List<String>>() {}.type
        gson.fromJson<List<String>>(json, type) ?: emptyList()
    }

    // اضافه کردن به تاریخچه
    suspend fun addQuery(query: String) {
        if (query.isBlank()) return
        
        context.historyDataStore.edit { prefs ->
            val json = prefs[KEY_HISTORY] ?: "[]"
            val type = object : TypeToken<List<String>>() {}.type
            val currentList = gson.fromJson<List<String>>(json, type)?.toMutableList() ?: mutableListOf()
            
            // اگه از قبل بود، حذفش کن و بذار اول لیست
            currentList.remove(query)
            currentList.add(0, query)
            
            // فقط ۱۰ تا آخری رو نگه دار
            val newList = if (currentList.size > 10) currentList.subList(0, 10) else currentList
            
            prefs[KEY_HISTORY] = gson.toJson(newList)
        }
    }

    // پاک کردن تاریخچه
    suspend fun clearHistory() {
        context.historyDataStore.edit { prefs ->
            prefs.remove(KEY_HISTORY)
        }
    }
}
