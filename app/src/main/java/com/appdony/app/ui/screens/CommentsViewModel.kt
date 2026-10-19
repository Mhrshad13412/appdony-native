package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Comment
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CommentsUiState {
    object Loading : CommentsUiState()
    data class Success(val comments: List<Comment>) : CommentsUiState()
    data class Error(val message: String) : CommentsUiState()
}

class CommentsViewModel(private val postId: Int) : ViewModel() {

    private val repo = AppRepository()

    private val _state = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    private val _sendResult = MutableStateFlow<String?>(null)
    val sendResult: StateFlow<String?> = _sendResult.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CommentsUiState.Loading
            repo.getComments(postId)
                .onSuccess { _state.value = CommentsUiState.Success(it) }
                .onFailure {
                    _state.value = CommentsUiState.Error(it.message ?: "خطا در دریافت نظرات")
                }
        }
    }

    fun sendComment(content: String, nonce: String) {
        if (content.isBlank()) {
            _sendResult.value = "متن نظر خالی است"
            return
        }

        viewModelScope.launch {
            _sending.value = true
            
            // 👇 اگه nonce خالی بود، خودمون میریم از API می‌گیریم
            var finalNonce = nonce
            if (finalNonce.isBlank()) {
                val user = repo.getCurrentUser().getOrNull()
                finalNonce = user?.nonce ?: ""
            }

            if (finalNonce.isBlank()) {
                _sendResult.value = "سشن شما منقضی شده. لطفاً از حساب خارج شده و دوباره وارد شوید."
                _sending.value = false
                return@launch
            }

            repo.postComment(postId, content, finalNonce)
                .onSuccess { newComment ->
                    val current = _state.value
                    if (current is CommentsUiState.Success) {
                        _state.value = CommentsUiState.Success(
                            listOf(newComment) + current.comments
                        )
                    }
                    _sendResult.value = "نظر شما ثبت شد و در انتظار تأیید است ✓"
                }
                .onFailure { err ->
                    _sendResult.value = "خطا در ثبت نظر: ${err.message}"
                }
            _sending.value = false
        }
    }

    fun clearSendResult() {
        _sendResult.value = null
    }
}
