package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 👇 کلاس ChatMessage رو از UI آوردیم اینجا تا دیتابیس بتونه بشناسش
data class ChatMessage(val text: String, val isUser: Boolean)

private val Context.chatDataStore by preferencesDataStore(name = "chat_history")

class ChatHistoryManager(private val context: Context) {

    companion object {
        private val KEY_CHAT_HISTORY = stringPreferencesKey("chat_messages")
    }

    private val gson = Gson()

    // خواندن تاریخچه
    fun getChatHistory(): Flow<List<ChatMessage>> = context.chatDataStore.data.map { prefs ->
        val json = prefs[KEY_CHAT_HISTORY] ?: "[]"
        val type = object : TypeToken<List<ChatMessage>>() {}.type
        gson.fromJson<List<ChatMessage>>(json, type) ?: emptyList()
    }

    // ذخیره تاریخچه
    suspend fun saveChatHistory(messages: List<ChatMessage>) {
        context.chatDataStore.edit { prefs ->
            prefs[KEY_CHAT_HISTORY] = gson.toJson(messages)
        }
    }

    // پاک کردن تاریخچه
    suspend fun clearHistory() {
        context.chatDataStore.edit { prefs ->
            prefs.remove(KEY_CHAT_HISTORY)
        }
    }
}
