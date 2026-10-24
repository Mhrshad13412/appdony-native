package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.util.AppInfo
import com.appdony.app.util.ContentBlock
import com.appdony.app.util.HtmlParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(
        val post: Post, 
        val info: AppInfo, 
        val similar: List<Post>,
        val contentBlocks: List<ContentBlock>
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}

class DetailViewModel(
    private val postId: Int,
    private val isBlog: Boolean
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
            
            val postResult = if (isBlog) {
                repo.getBlogPost(postId)
            } else {
                repo.getPost(postId)
            }

            postResult.onSuccess { post ->
                val rawHtml = post.content?.rendered ?: ""
                
                // پارس کردن HTML به بلاک‌های تمیز
                val blocks = HtmlParser.parseContent(rawHtml)
                
                // استخراج اطلاعات (مثل لینک دانلود)
                val info = HtmlParser.parsePostContent(rawHtml)
                
                val catId = post.primaryCategoryId()
                val similar = if (!isBlog && catId != null) {
                    repo.getPostsByCategory(catId, perPage = 8)
                        .getOrNull()
                        ?.filter { it.id != post.id }
                        ?.take(6)
                        ?: emptyList()
                } else emptyList()
                
                _state.value = DetailUiState.Success(post, info, similar, blocks)
            }.onFailure {
                _state.value = DetailUiState.Error(it.message ?: "خطا در دریافت اطلاعات")
            }
        }
    }
}

private fun Post.primaryCategoryId(): Int? {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.id
}
