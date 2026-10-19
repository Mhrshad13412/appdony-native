package com.appdony.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
import com.appdony.app.data.preferences.SearchHistoryManager
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    // 👇 لیست تاریخچه
    val history: StateFlow<List<String>> = historyManager.history.stateIn(
        viewModelScope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private var searchJob: Job? = null

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            _state.value = SearchUiState.Idle
            return
        }
        
        searchJob = viewModelScope.launch {
            delay(400)
            _state.value = SearchUiState.Loading
            
            // 👇 ذخیره کلمه در تاریخچه
            historyManager.addQuery(newQuery.trim())
            
            repo.searchPosts(newQuery)
                .onSuccess { _state.value = SearchUiState.Success(it) }
                .onFailure {
                    _state.value = SearchUiState.Error(it.message ?: "خطا")
                }
        }
    }

    // پاک کردن تاریخچه
    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
        }
    }
    
    // انتخاب از تاریخچه
    fun setQueryFromHistory(q: String) {
        onQueryChange(q)
    }
}
