package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Post
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

class SearchViewModel : ViewModel() {

    private val repo = AppRepository()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

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
            repo.searchPosts(newQuery)
                .onSuccess { _state.value = SearchUiState.Success(it) }
                .onFailure {
                    _state.value = SearchUiState.Error(it.message ?: "خطا")
                }
        }
    }
}
