package com.appdony.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** مقصدی که پنجره‌ی شناور یا پاپیِ بیرونی می‌خواد داخل اپ باز بشه */
object PendingRoute {
    private val _route = MutableStateFlow<String?>(null)
    val route: StateFlow<String?> = _route

    fun set(route: String?) {
        _route.value = route
    }
}
