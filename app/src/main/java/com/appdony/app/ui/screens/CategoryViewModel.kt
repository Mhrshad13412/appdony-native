package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CategoryUiState {
    object Loading : CategoryUiState()
    data class Success(val posts: List<Post>) : CategoryUiState()
    data class Error(val message: String) : CategoryUiState()
}

class CategoryViewModel(
    private val categoryId: Int,
    private val categoryName: String,
    private val context: android.content.Context
) : ViewModel() {

    private val repo = AppRepository(context)

    private val _state = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    private val _title = MutableStateFlow(categoryName)
    val title: StateFlow<String> = _title.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private var isLoadingData = false

    init { loadInitial() }

    fun loadInitial() {
        if (isLoadingData) return
        isLoadingData = true
        currentPage = 1
        isEndReached = false

        viewModelScope.launch {
            _state.value = CategoryUiState.Loading
            repo.getPostsByCategoryPaged(categoryId, page = 1, perPage = 24)
                .onSuccess { posts ->
                    _state.value = CategoryUiState.Success(posts)
                    if (posts.size < 24) isEndReached = true
                }
                .onFailure {
                    _state.value = CategoryUiState.Error(it.message ?: "خطا")
                }
            isLoadingData = false
        }
    }

    fun loadMore() {
        if (isLoadingData || isEndReached) return
        isLoadingData = true

        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++

            repo.getPostsByCategoryPaged(categoryId, page = currentPage, perPage = 24)
                .onSuccess { newPosts ->
                    if (newPosts.isEmpty()) {
                        isEndReached = true
                    } else {
                        val current = _state.value
                        if (current is CategoryUiState.Success) {
                            _state.value = CategoryUiState.Success(current.posts + newPosts)
                        }
                        if (newPosts.size < 24) isEndReached = true
                    }
                }
                .onFailure {
                    currentPage-- // اگه خطا داد، صفحه رو برمی‌گردونیم
                }
            _isLoadingMore.value = false
            isLoadingData = false
        }
    }
}
