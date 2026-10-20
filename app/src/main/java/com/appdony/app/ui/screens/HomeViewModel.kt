package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val repo = AppRepository()

    private val _initialData = MutableStateFlow<InitialData?>(null)
    val initialData: StateFlow<InitialData?> = _initialData.asStateFlow()

    private val _plusPosts = MutableStateFlow<List<Post>>(emptyList())
    val plusPosts: StateFlow<List<Post>> = _plusPosts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var isEndReached = false
    private var isLoadingData = false

    init {
        loadInitial()
    }

    fun loadInitial() {
        if (isLoadingData) return
        isLoadingData = true
        viewModelScope.launch {
            _isLoading.value = true
            repo.loadInitial("home").onSuccess { data ->
                _initialData.value = data
            }.onFailure {
                // Handle error
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
            repo.loadPlusPosts(page = currentPage, perPage = 10).onSuccess { newPosts ->
                if (newPosts.isEmpty()) {
                    isEndReached = true
                } else {
                    _plusPosts.value = _plusPosts.value + newPosts
                    if (newPosts.size < 10) isEndReached = true
                }
            }.onFailure {
                currentPage--
            }
            _isLoadingMore.value = false
            isLoadingData = false
        }
    }
}
