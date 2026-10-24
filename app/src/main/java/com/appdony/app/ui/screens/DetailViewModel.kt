package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.util.AppInfo
import com.appdony.app.util.HtmlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val post: Post, val info: AppInfo, val similar: List<Post>) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val postId: Int,
    private val isBlog: Boolean // 👈 پارامتر جدید
) : ViewModel() {
    private val repo = AppRepository()
    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = DetailUiState.Loading
            
            // 👈 انتخاب متد بر اساس نوع پست
            val postResult = if (isBlog) {
                repo.getBlogPost(postId)
            } else {
                repo.getPost(postId)
            }

            postResult.onSuccess { post ->
                val info = HtmlParser.parsePostContent(post.content.rendered)
                val catId = post.primaryCategoryId()
                val similar = if (catId != null) {
                    repo.getPostsByCategory(catId, perPage = 8)
                        .getOrNull()
                        ?.filter { it.id != post.id }
                        ?.take(6)
                        ?: emptyList()
                } else emptyList()
                _state.value = DetailUiState.Success(post, info, similar)
            }.onFailure {
                _state.value = DetailUiState.Error(it.message ?: "خطا")
            }
        }
    }
}

private fun Post.primaryCategoryId(): Int? {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.id
}
