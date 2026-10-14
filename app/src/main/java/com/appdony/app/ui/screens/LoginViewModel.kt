package com.appdony.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.UserData
import com.appdony.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: UserData) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val repo = AppRepository()

    private val _state = MutableStateFlow<LoginState>(LoginState.Idle)
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = LoginState.Error("نام کاربری و رمز عبور الزامی است")
            return
        }

        viewModelScope.launch {
            _state.value = LoginState.Loading
            repo.login(username.trim(), password)
                .onSuccess { user ->
                    _state.value = LoginState.Success(user)
                }
                .onFailure { err ->
                    _state.value = LoginState.Error(
                        err.message ?: "خطا در لاگین — دوباره تلاش کن"
                    )
                }
        }
    }

    fun resetState() {
        _state.value = LoginState.Idle
    }
}
