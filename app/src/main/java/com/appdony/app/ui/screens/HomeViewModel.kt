package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.HomeData
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val data: HomeData) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel : ViewModel() {

    private val repo = AppRepository()

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = HomeUiState.Loading
            repo.loadHome()
                .onSuccess { _state.value = HomeUiState.Success(it) }
                .onFailure {
                    _state.value = HomeUiState.Error(it.message ?: "خطا در دریافت اطلاعات")
                }
        }
    }
}
