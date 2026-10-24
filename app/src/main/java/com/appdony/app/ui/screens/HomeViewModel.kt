package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.MainRepository

class HomeViewModel(private val repository: MainRepository) : ViewModel() {

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var currentSearchQuery: String? = null

    init {
        loadInitialPosts("برنامه")
    }

    fun loadInitialPosts(searchQuery: String) {
        currentSearchQuery = searchQuery
        currentPage = 1
        isLastPage = false
        _posts.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val newPosts = repository.getPosts(page = currentPage, searchQuery = currentSearchQuery)
                _posts.value = newPosts
                if (newPosts.size < 10) isLastPage = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMorePosts() {
        if (_isLoadingMore.value || isLastPage) return

        _isLoadingMore.value = true
        currentPage++

        viewModelScope.launch {
            try {
                val newPosts = repository.getPosts(page = currentPage, searchQuery = currentSearchQuery)
                if (newPosts.isEmpty()) {
                    isLastPage = true
                } else {
                    _posts.value = _posts.value + newPosts
                }
            } catch (e: Exception) {
                currentPage--
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}
