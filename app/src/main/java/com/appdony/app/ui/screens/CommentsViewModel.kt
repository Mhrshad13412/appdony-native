package com.appdony.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Comment
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.data.repository.CommentException
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CommentsUiState {
    object Loading : CommentsUiState()
    data class Success(val comments: List<Comment>) : CommentsUiState()
    data class Error(val message: String) : CommentsUiState()
}

class CommentsViewModel(
    private val postId: Int,
    context: Context
) : ViewModel() {

    // applicationContext تا ViewModel به Activity نچسبه
    private val appContext = context.applicationContext
    private val repo = AppRepository(appContext)
    private val session = SessionManager(appContext)

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

    private fun needsSessionRefresh(result: Result<Comment>): Boolean {
        val code = (result.exceptionOrNull() as? CommentException)?.httpCode
        return result.isFailure && (code == 401 || code == 403)
    }

    private fun errorTextOf(err: Throwable?): String {
        val e = err as? CommentException ?: return "ارسال نظر انجام نشد. اینترنت را بررسی کن و دوباره امتحان کن."
        return when {
            e.wpCode == "comment_duplicate" -> "این نظر قبلاً ثبت شده."
            e.wpCode == "comment_flood" -> "کمی صبر کن و دوباره امتحان کن."
            e.wpCode == "rest_comment_closed" || e.wpCode == "comment_closed" -> "نظردهی این پست بسته است."
            e.httpCode == 401 || e.httpCode == 403 -> "نشست شما منقضی شده؛ یک بار از حساب خارج و دوباره وارد شو."
            !e.serverMessage.isNullOrBlank() -> e.serverMessage
            else -> "ارسال نظر انجام نشد (خطای ${e.httpCode})."
        }
    }

    /**
     * ارسال نظر. اگه نانس/نشست منقضی بود: اول نانس تازه، بعد ورود خودکار، و هر بار تلاش دوباره.
     * [onDone] با true وقتی نظر سمت سرور ثبت شد (حتی اگه منتظر تأیید باشه).
     */
    fun sendComment(content: String, nonce: String, onDone: (Boolean) -> Unit = {}) {
        if (_sending.value) return
        val text = content.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            _sending.value = true

            var result = repo.postComment(postId, text, nonce)

            // مرحله‌ی ۱: نانس تازه با همون کوکی‌ها
            if (needsSessionRefresh(result)) {
                val fresh = repo.fetchFreshNonce()
                if (!fresh.isNullOrBlank()) {
                    session.saveNonce(fresh)
                    result = repo.postComment(postId, text, fresh)
                }
            }

            // مرحله‌ی ۲: ورود خودکار با اطلاعات ذخیره‌شده
            if (needsSessionRefresh(result)) {
                val user = repo.autoLogin().getOrNull()
                if (user != null) {
                    val newNonce = user.nonce.ifBlank { repo.fetchFreshNonce().orEmpty() }
                    if (newNonce.isNotBlank()) {
                        session.saveNonce(newNonce)
                        result = repo.postComment(postId, text, newNonce)
                    }
                }
            }

            val comment = result.getOrNull()
            if (comment != null) {
                if (comment.status == "approved" || comment.status.isBlank()) {
                    val currentList = (_state.value as? CommentsUiState.Success)?.comments ?: emptyList()
                    _state.value = CommentsUiState.Success(listOf(comment) + currentList)
                    AppBanner.show(BannerKind.SUCCESS, "نظر شما ثبت شد")
                } else {
                    AppBanner.show(
                        BannerKind.WARNING,
                        "نظر شما ثبت شد و بعد از تأیید مدیر نمایش داده می‌شود",
                        4500L
                    )
                }
                _sending.value = false
                onDone(true)
            } else {
                AppBanner.show(BannerKind.ERROR, errorTextOf(result.exceptionOrNull()), 4500L)
                _sending.value = false
                onDone(false)
            }
        }
    }
}
