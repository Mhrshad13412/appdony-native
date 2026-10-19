package com.appdony.app.ui.screens

import android.content.Context
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

class CommentsViewModel(private val postId: Int, context: Context) : ViewModel() {

    private val repo = AppRepository(context)

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
                .onFailure { _state.value = CommentsUiState.Error(it.message ?: "خطا در دریافت نظرات") }
        }
    }

    fun sendComment(content: String, currentNonce: String) {
        if (content.isBlank()) {
            _sendResult.value = "متن نظر خالی است"
            return
        }

        viewModelScope.launch {
            _sending.value = true
            var finalNonce = currentNonce

            // 👇 ۱. اگر nonce نداریم، از API می‌گیریم
            if (finalNonce.isBlank()) {
                val freshUser = repo.getCurrentUser().getOrNull()
                if (freshUser != null) finalNonce = freshUser.nonce
            }

            // 👇 ۲. اگر بازم نداریم (سشن مرده)، به صورت خودکار لاگین می‌کنیم
            if (finalNonce.isBlank()) {
                val autoLoginResult = repo.autoLogin()
                if (autoLoginResult.isSuccess) {
                    val freshUser = repo.getCurrentUser().getOrNull()
                    finalNonce = freshUser?.nonce ?: ""
                }
            }

            if (finalNonce.isBlank()) {
                _sendResult.value = "سشن شما منقضی شده و اطلاعات ورود هم ذخیره نشده است. لطفاً دستی وارد شوید."
                _sending.value = false
                return@launch
            }

            // 👇 ۳. ارسال با nonce جدید
            repo.postComment(postId, content, finalNonce)
                .onSuccess { newComment ->
                    val current = _state.value
                    if (current is CommentsUiState.Success) {
                        _state.value = CommentsUiState.Success(listOf(newComment) + current.comments)
                    }
                    _sendResult.value = "نظر شما ثبت شد و در انتظار تأیید است ✓"
                }
                .onFailure { err ->
                    val errorMsg = err.message ?: ""
                    // اگر خطای 401 داد، دوباره لاگین می‌کنیم و ارسال می‌کنیم
                    if (errorMsg.contains("401") || errorMsg.contains("403") || errorMsg.contains("Forbidden")) {
                        val autoLoginResult = repo.autoLogin()
                        if (autoLoginResult.isSuccess) {
                            val newNonce = repo.getCurrentUser().getOrNull()?.nonce ?: ""
                            if (newNonce.isNotBlank()) {
                                repo.postComment(postId, content, newNonce).onSuccess { newComment ->
                                    val current = _state.value
                                    if (current is CommentsUiState.Success) {
                                        _state.value = CommentsUiState.Success(listOf(newComment) + current.comments)
                                    }
                                    _sendResult.value = "نظر شما ثبت شد ✓"
                                }
                            }
                        } else {
                            _sendResult.value = "سشن شما منقضی شده. لطفاً دوباره وارد شوید."
                        }
                    } else {
                        _sendResult.value = "خطا در ثبت نظر: $errorMsg"
                    }
                }
            _sending.value = false
        }
    }

    fun clearSendResult() { _sendResult.value = null }
}
