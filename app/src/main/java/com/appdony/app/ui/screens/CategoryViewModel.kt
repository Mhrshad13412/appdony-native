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
    private val categoryName: String
) : ViewModel() {

    private val repo = AppRepository()

    private val _state = MutableStateFlow<CategoryUiState>(CategoryUiState.Loading)
    val state: StateFlow<CategoryUiState> = _state.asStateFlow()

    private val _title = MutableStateFlow(categoryName)
    val title: StateFlow<String> = _title.asStateFlow()

    private var currentPage = 1
    private var isLoadingMore = false

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CategoryUiState.Loading
            currentPage = 1
            repo.getPostsByCategory(categoryId, perPage = 24)
                .onSuccess { posts ->
                    _state.value = CategoryUiState.Success(posts)
                }
                .onFailure {
                    _state.value = CategoryUiState.Error(it.message ?: "خطا")
                }
        }
    }

    fun loadMore() {
        if (isLoadingMore) return
        val current = _state.value
        if (current !is CategoryUiState.Success) return
        isLoadingMore = true
        viewModelScope.launch {
            currentPage++
            repo.getPostsByCategoryPaged(categoryId, currentPage, 24)
                .onSuccess { newPosts ->
                    if (newPosts.isNotEmpty()) {
                        _state.value = CategoryUiState.Success(current.posts + newPosts)
                    }
                }
            isLoadingMore = false
        }
    }
}
