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

    // 👇 وضعیت نیاز به لاگین مجدد
    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CommentsUiState.Loading
            repo.getComments(postId)
                .onSuccess { _state.value = CommentsUiState.Success(it) }
                .onFailure { _state.value = CommentsUiState.Error(it.message ?: "خطا در دریافت نظرات") }
        }
    }

    fun sendComment(content: String, oldNonce: String) {
        if (content.isBlank()) {
            _sendResult.value = "متن نظر خالی است"
            return
        }

        viewModelScope.launch {
            _sending.value = true
            _sessionExpired.value = false
            
            var finalNonce = oldNonce
            try {
                val freshUser = repo.getCurrentUser().getOrNull()
                if (freshUser != null && freshUser.nonce.isNotBlank()) {
                    finalNonce = freshUser.nonce
                }
            } catch (e: Exception) {}

            if (finalNonce.isBlank()) {
                _sendResult.value = "سشن شما منقضی شده است."
                _sessionExpired.value = true
                _sending.value = false
                return@launch
            }

            repo.postComment(postId, content, finalNonce)
                .onSuccess { newComment ->
                    val current = _state.value
                    if (current is CommentsUiState.Success) {
                        _state.value = CommentsUiState.Success(listOf(newComment) + current.comments)
                    }
                    _sendResult.value = "نظر شما ثبت شد و در انتظار تأیید است ✓"
                }
                .onFailure { err ->
                    // 👇 تشخیص ارور 401 یا 403
                    val errorMsg = err.message ?: ""
                    if (errorMsg.contains("401") || errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
                        _sessionExpired.value = true
                        _sendResult.value = "سشن شما منقضی شده است. لطفاً دوباره وارد شوید."
                    } else {
                        _sendResult.value = "خطا در ثبت نظر: $errorMsg"
                    }
                }
            _sending.value = false
        }
    }

    fun clearSendResult() { _sendResult.value = null }
}
