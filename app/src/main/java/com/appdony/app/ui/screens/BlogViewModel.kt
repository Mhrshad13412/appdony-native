package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlogViewModel : ViewModel() {

    private val repo = AppRepository()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private var isLoadingData = false

    init {
        loadInitial()
    }

    fun loadInitial() {
        if (isLoadingData) return
        isLoadingData = true
        currentPage = 1
        isEndReached = false
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMsg.value = null
            
            repo.getBlogPosts(page = currentPage, perPage = 10)
                .onSuccess { newPosts ->
                    _posts.value = newPosts
                    if (newPosts.size < 10) isEndReached = true
                }
                .onFailure { err ->
                    _errorMsg.value = err.message ?: "خطا در دریافت اطلاعات"
                }
            
            _isLoading.value = false
            isLoadingData = false
        }
    }

    fun loadMore() {
        if (isLoadingData || isEndReached) return
        isLoadingData = true
        
        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++
            
            repo.getBlogPosts(page = currentPage, perPage = 10)
                .onSuccess { newPosts ->
                    if (newPosts.isEmpty()) {
                        isEndReached = true
                    } else {
                        _posts.value = _posts.value + newPosts
                        if (newPosts.size < 10) isEndReached = true
                    }
                }
                .onFailure {
                    currentPage-- // برگرداندن شماره صفحه در صورت خطا
                }
            
            _isLoadingMore.value = false
            isLoadingData = false
        }
    }
}
