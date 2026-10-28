package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.api.AiApi
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.cleanTitle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    // پیام‌های خوش‌آمدگویی و خطا نباید به مدل فرستاده بشن
    val excludeFromContext: Boolean = false
)

class AiChatViewModel : ViewModel() {

    private val welcome = ChatMessage("سلام! من دکتر طاعونم. چطور میتونم کمکت کنم؟", false, excludeFromContext = true)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        _messages.value = listOf(welcome)
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank() || _isLoading.value) return

        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage(userMessage, true))
        _messages.value = currentMessages
        _isLoading.value = true

        viewModelScope.launch {
            // فقط پیام‌های واقعی؛ آخرین ۸ تا؛ و تاریخچه با پیام کاربر شروع بشه
            val history = currentMessages
                .dropLast(1)
                .filter { !it.excludeFromContext }
                .takeLast(8)
                .dropWhile { !it.isUser }
                .map { (if (it.isUser) "user" else "assistant") to it.text }

            val context = loadSiteContext(userMessage)
            val result = AiApi.sendMessage(userMessage, history, context)

            result.onSuccess { response ->
                _messages.value = _messages.value + ChatMessage(response, false)
            }.onFailure { error ->
                _messages.value = _messages.value + ChatMessage(
                    text = "⚠️ ${error.message ?: "مشکلی پیش اومد."}",
                    isUser = false,
                    excludeFromContext = true
                )
            }
            _isLoading.value = false
        }
    }

    // چند پست مرتبط از خود سایت اپدونی تا مدل اسم بازی‌ها رو از خودش نسازه
    private suspend fun loadSiteContext(query: String): String {
        if (query.trim().length < 4) return ""
        return try {
            ApiClient.api.searchPosts(query = query.trim().take(60), perPage = 4)
                .joinToString("\n") { "- " + it.cleanTitle() }
        } catch (e: Exception) {
            ""
        }
    }

    fun clearChat() {
        _messages.value = listOf(welcome)
    }
}
