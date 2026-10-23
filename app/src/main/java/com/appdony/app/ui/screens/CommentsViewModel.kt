package com.appdony.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Comment
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.data.session.CredentialsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

sealed class CommentsUiState {
    object Loading : CommentsUiState()
    data class Success(val comments: List<Comment>) : CommentsUiState()
    data class Error(val message: String) : CommentsUiState()
}

class CommentsViewModel(
    private val postId: Int,
    private val context: Context
) : ViewModel() {

    private val repo = AppRepository(context)
    private val credentialsManager = CredentialsManager(context)

    private val _state = MutableStateFlow<CommentsUiState>(CommentsUiState.Loading)
    val state: StateFlow<CommentsUiState> = _state.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    init {
        loadComments()
    }

    fun loadComments() {
        viewModelScope.launch {
            _state.value = CommentsUiState.Loading
            repo.getComments(postId).onSuccess { list ->
                _state.value = CommentsUiState.Success(list)
            }.onFailure { err ->
                _state.value = CommentsUiState.Error(err.message ?: "خطا در دریافت نظرات")
            }
        }
    }

    fun sendComment(content: String, nonce: String) {
        viewModelScope.launch {
            _sending.value = true
            
            // تلاش اول
            var result = repo.postComment(postId, content, nonce)
            
            // اگر خطا 401 یا 403 بود، یعنی سشن منقضی شده. میریم لاگین خودکار
            if (result.isFailure && (result.exceptionOrNull()?.message?.contains("401") == true || result.exceptionOrNull()?.message?.contains("403") == true)) {
                val username = credentialsManager.getUsername()
                val password = credentialsManager.getPassword()
                
                if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                    // لاگین خودکار
                    val loginResult = repo.autoLogin()
                    if (loginResult.isSuccess) {
                        // توکن جدید رو بگیر (چون لاگین کردیم)
                        val newNonce = repo.getCurrentUser().getOrNull()?.nonce ?: nonce
                        
                        // تلاش دوم (بعد از لاگین موفق)
                        result = repo.postComment(postId, content, newNonce)
                    }
                }
            }

            result.onSuccess { newComment ->
                val currentList = (_state.value as? CommentsUiState.Success)?.comments ?: emptyList()
                _state.value = CommentsUiState.Success(listOf(newComment) + currentList)
            }.onFailure { err ->
                // شاید بخوای یه پیام خاص برای خطای ارسال بدی
            }
            
            _sending.value = false
        }
    }
}
