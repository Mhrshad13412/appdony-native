package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.api.AiApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

class AiChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        _messages.value = listOf(ChatMessage("سلام! من دکتر طاعونم. چطور میتونم کمکت کنم؟", false))
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return
        
        val currentMessages = _messages.value.toMutableList()
        currentMessages.add(ChatMessage(userMessage, true))
        _messages.value = currentMessages
        _isLoading.value = true

        viewModelScope.launch {
            val history = currentMessages.dropLast(1).map { (if (it.isUser) "user" else "assistant") to it.text }
            val result = AiApi.sendMessage(userMessage, history)
            
            result.onSuccess { response ->
                val updated = _messages.value.toMutableList()
                updated.add(ChatMessage(response, false))
                _messages.value = updated
            }.onFailure { error ->
                val updated = _messages.value.toMutableList()
                updated.add(ChatMessage("خطا: ${error.message}", false))
                _messages.value = updated
            }
            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = listOf(ChatMessage("سلام! من دکتر طاعونم. چطور میتونم کمکت کنم؟", false))
    }
}
