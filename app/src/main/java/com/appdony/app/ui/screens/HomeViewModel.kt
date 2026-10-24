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

    // نگهداری لیست پست‌ها
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    // وضعیت لودینگ اولیه
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // وضعیت لودینگ برای بارگذاری بی‌نهایت (پایین صفحه)
    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false
    private var currentCategory = "app" // پیش‌فرض: برنامه‌ها

    init {
        loadInitialPosts("app")
    }

    // لود کردن صفحه اول (زمانی که کاربر روی تب بازی‌ها یا برنامه‌ها کلیک می‌کنه)
    fun loadInitialPosts(category: String) {
        currentCategory = category
        currentPage = 1
        isLastPage = false
        _posts.value = emptyList()
        _isLoading.value = true

        viewModelScope.launch {
            try {
                // درخواست به API (مثلاً وردپرس) برای گرفتن صفحه اول
                val newPosts = repository.getPosts(category = category, page = currentPage)
                _posts.value = newPosts
                if (newPosts.isEmpty() || newPosts.size < 10) isLastPage = true
            } catch (e: Exception) {
                // مدیریت خطا
            } finally {
                _isLoading.value = false
            }
        }
    }

    // لود کردن صفحات بعدی (زمانی که کاربر به پایین صفحه اسکرول می‌کنه)
    fun loadMorePosts() {
        if (_isLoadingMore.value || isLastPage) return

        _isLoadingMore.value = true
        currentPage++

        viewModelScope.launch {
            try {
                val newPosts = repository.getPosts(category = currentCategory, page = currentPage)
                if (newPosts.isEmpty()) {
                    isLastPage = true
                } else {
                    _posts.value = _posts.value + newPosts
                }
            } catch (e: Exception) {
                currentPage-- // در صورت خطا، صفحه رو برنگردون عقب
            } finally {
                _isLoadingMore.value = false
            }
        }
    }
}
