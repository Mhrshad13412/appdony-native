package com.appdony.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.preferences.SearchHistoryManager
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.util.LatestOnly
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn // 👈 این ایمپورت اضافه شد
import kotlinx.coroutines.launch

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val posts: List<Post>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

class SearchViewModel(private val context: Context) : ViewModel() {

    private val repo = AppRepository(context)
    private val historyManager = SearchHistoryManager(context)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val history: StateFlow<List<String>> = historyManager.history.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private var searchJob: Job? = null
    // نتیجه‌ی جستجوهای قبلی توی حافظه؛ تکراری‌ها فوری نشون داده می‌شن
    private val memo = object : LinkedHashMap<String, List<Post>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Post>>?) = size > 30
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            _state.value = SearchUiState.Idle
            return
        }
        
        val q = newQuery.trim()
        memo[q]?.let { // نتیجه‌ی کش‌شده: بدون تأخیر
            _state.value = SearchUiState.Success(it)
            searchJob = viewModelScope.launch { historyManager.addQuery(q) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(250)
            _state.value = SearchUiState.Loading

            launch { historyManager.addQuery(q) }

            repo.searchPosts(q)
                // از هر اپ/بازی فقط جدیدترین نسخه
                .onSuccess {
                    val list = LatestOnly.latestPerApp(it)
                    memo[q] = list
                    _state.value = SearchUiState.Success(list)
                }
                .onFailure {
                    _state.value = SearchUiState.Error(it.message ?: "خطا")
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }
    
    fun setQueryFromHistory(q: String) {
        onQueryChange(q)
    }
}
