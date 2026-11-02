package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.util.LatestOnly
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption(val title: String, val apiValue: String, val order: String) {
    NEWEST("جدیدترین", "date", "desc"),
    POPULAR("محبوب‌ترین", "comment_count", "desc"),
    ALPHABETICAL("الفبا", "title", "asc")
}

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

    // 👇 وضعیت مرتب‌سازی فعلی
    private val _currentSort = MutableStateFlow(SortOption.NEWEST)
    val currentSort: StateFlow<SortOption> = _currentSort.asStateFlow()

    // «مشاهده همه»ی ردیف «به‌روزترین‌ها» با همین اسم باز می‌شه؛ پس تب پیش‌فرض بر اساس آخرین ویرایش مرتب می‌شه
    private val updatedMode = categoryName == "به‌روزترین‌ها"

    private fun apiSort(sort: SortOption): String =
        if (updatedMode && sort == SortOption.NEWEST) "modified" else sort.apiValue

    private var currentPage = 1
    private var isEndReached = false
    private var isLoadingData = false

    init { loadInitial() }

    fun setSort(option: SortOption) {
        if (_currentSort.value == option) return
        _currentSort.value = option
        loadInitial() // دوباره از اول لود کن
    }

    fun loadInitial() {
        if (isLoadingData) return
        isLoadingData = true
        currentPage = 1
        isEndReached = false

        viewModelScope.launch {
            _state.value = CategoryUiState.Loading
            val sort = _currentSort.value
            
            repo.getPostsByCategoryPaged(
                categoryId = categoryId, 
                page = 1, 
                perPage = 24,
                orderBy = apiSort(sort),
                order = sort.order
            ).onSuccess { posts ->
                // از هر اپ/بازی فقط جدیدترین نسخه
                _state.value = CategoryUiState.Success(LatestOnly.latestPerApp(posts))
                if (posts.size < 24) isEndReached = true
            }.onFailure {
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
            val sort = _currentSort.value

            repo.getPostsByCategoryPaged(
                categoryId = categoryId, 
                page = currentPage, 
                perPage = 24,
                orderBy = apiSort(sort),
                order = sort.order
            ).onSuccess { newPosts ->
                if (newPosts.isEmpty()) {
                    isEndReached = true
                } else {
                    val current = _state.value
                    if (current is CategoryUiState.Success) {
                        // صفحه‌های بعدی هم با قبلی‌ها یکی می‌شن تا نسخه‌ی قدیمی دوباره نیاد
                        _state.value = CategoryUiState.Success(
                            LatestOnly.latestPerApp(current.posts + newPosts)
                        )
                    }
                    if (newPosts.size < 24) isEndReached = true
                }
            }.onFailure {
                currentPage--
            }
            _isLoadingMore.value = false
            isLoadingData = false
        }
    }
}
