package com.appdony.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SectionState {
    object NotLoaded : SectionState()
    object Loading : SectionState()
    data class Loaded(val posts: List<Post>) : SectionState()
    object Error : SectionState()
}

class HomeViewModel(
    private val feedType: String,
    private val context: Context
) : ViewModel() {

    private val repo = AppRepository(context)

    private val _initialLoading = MutableStateFlow(true)
    val initialLoading: StateFlow<Boolean> = _initialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _plusPosts = MutableStateFlow<List<Post>>(emptyList())
    val plusPosts: StateFlow<List<Post>> = _plusPosts.asStateFlow()

    private val _featured = MutableStateFlow<List<Post>>(emptyList())
    val featured: StateFlow<List<Post>> = _featured.asStateFlow()

    private val _categories = MutableStateFlow<List<com.appdony.app.data.model.Category>>(emptyList())
    val categories: StateFlow<List<com.appdony.app.data.model.Category>> = _categories.asStateFlow()

    private val _sections = MutableStateFlow<Map<Int, SectionState>>(emptyMap())
    val sections: StateFlow<Map<Int, SectionState>> = _sections.asStateFlow()

    init {
        loadInitial()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _initialLoading.value = true
            _error.value = null
            repo.loadInitial(feedType).onSuccess { data ->
                _featured.value = data.featured
                _categories.value = data.categories
                _initialLoading.value = false
            }.onFailure { e ->
                _error.value = e.message
                _initialLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            repo.loadInitial(feedType).onSuccess { data ->
                _featured.value = data.featured
                _categories.value = data.categories
                _isRefreshing.value = false
            }.onFailure {
                _isRefreshing.value = false
            }
        }
    }

    fun loadSection(categoryId: Int) {
        viewModelScope.launch {
            val current = _sections.value.toMutableMap()
            current[categoryId] = SectionState.Loading
            _sections.value = current

            repo.getPostsByCategory(categoryId, 6).onSuccess { posts ->
                val updated = _sections.value.toMutableMap()
                updated[categoryId] = SectionState.Loaded(posts)
                _sections.value = updated
            }.onFailure {
                val updated = _sections.value.toMutableMap()
                updated[categoryId] = SectionState.Error
                _sections.value = updated
            }
        }
    }
}
