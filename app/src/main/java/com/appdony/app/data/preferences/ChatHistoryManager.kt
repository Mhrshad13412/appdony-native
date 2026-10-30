package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** پیام ذخیره‌شده‌ی چت هوش مصنوعی روی گوشی */
data class StoredChatMessage(val text: String, val isUser: Boolean, val excludeFromContext: Boolean = false)

private val Context.chatDataStore by preferencesDataStore(name = "chat_history")

/** تاریخچه‌ی چت هوش مصنوعی فقط روی خود گوشی ذخیره می‌شه (با org.json تا R8 خرابش نکنه) */
class ChatHistoryManager(private val context: Context) {

    companion object {
        private val KEY_CHAT_HISTORY = stringPreferencesKey("chat_messages")
        private const val MAX_SAVED = 200
    }

    fun getChatHistory(): Flow<List<StoredChatMessage>> = context.chatDataStore.data.map { prefs ->
        parse(prefs[KEY_CHAT_HISTORY])
    }

    suspend fun load(): List<StoredChatMessage> = getChatHistory().first()

    suspend fun saveChatHistory(messages: List<StoredChatMessage>) {
        val arr = JSONArray()
        messages.takeLast(MAX_SAVED).forEach {
            arr.put(JSONObject().put("t", it.text).put("u", it.isUser).put("x", it.excludeFromContext))
        }
        context.chatDataStore.edit { it[KEY_CHAT_HISTORY] = arr.toString() }
    }

    suspend fun clearHistory() {
        context.chatDataStore.edit { it.remove(KEY_CHAT_HISTORY) }
    }

    private fun parse(json: String?): List<StoredChatMessage> = try {
        val arr = JSONArray(json ?: "[]")
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            StoredChatMessage(o.getString("t"), o.getBoolean("u"), o.optBoolean("x", false))
        }
    } catch (e: Exception) {
        emptyList()
    }
}
