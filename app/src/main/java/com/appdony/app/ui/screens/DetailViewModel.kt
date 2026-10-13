package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Post
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class SectionState {
    object NotLoaded : SectionState()
    object Loading : SectionState()
    data class Loaded(val posts: List<Post>) : SectionState()
    data class Error(val message: String) : SectionState()
}

class HomeViewModel(private val feedType: String = "games") : ViewModel() {

    private val repo = AppRepository()

    private val _initialLoading = MutableStateFlow(true)
    val initialLoading: StateFlow<Boolean> = _initialLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _plusPosts = MutableStateFlow<List<Post>>(emptyList())
    val plusPosts: StateFlow<List<Post>> = _plusPosts.asStateFlow()

    private val _featured = MutableStateFlow<List<Post>>(emptyList())
    val featured: StateFlow<List<Post>> = _featured.asStateFlow()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _sections = MutableStateFlow<Map<Int, SectionState>>(emptyMap())
    val sections: StateFlow<Map<Int, SectionState>> = _sections.asStateFlow()

    init {
        loadInitial()
        loadPlus()
    }

    fun loadInitial() {
        viewModelScope.launch {
            _initialLoading.value = true
            _error.value = null
            repo.loadInitial(feedType)
                .onSuccess { data ->
                    _featured.value = data.featured
                    _categories.value = data.categories
                    _sections.value = data.categories.associate { it.id to SectionState.NotLoaded }
                    _initialLoading.value = false
                }
                .onFailure { err ->
                    _error.value = err.message ?: "خطا در دریافت اطلاعات"
                    _initialLoading.value = false
                }
        }
    }

    fun loadPlus() {
        viewModelScope.launch {
            repo.loadPlusPosts()
                .onSuccess { _plusPosts.value = it }
        }
    }

    fun loadSection(categoryId: Int) {
        val current = _sections.value[categoryId]
        if (current is SectionState.Loading || current is SectionState.Loaded) return

        viewModelScope.launch {
            _sections.update { it + (categoryId to SectionState.Loading) }
            repo.getPostsByCategory(categoryId, perPage = 15)
                .onSuccess { posts ->
                    _sections.update { it + (categoryId to SectionState.Loaded(posts)) }
                }
                .onFailure { err ->
                    _sections.update { currentMap ->
                        currentMap + (categoryId to SectionState.Error(err.message ?: "خطا"))
                    }
                }
        }
    }
}
