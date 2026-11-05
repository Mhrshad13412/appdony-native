package com.appdony.app.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * وضعیت زنده‌ی اینترنت. موتور دانلود با این می‌فهمه کِی باید صبر کنه (قطعی یا «فقط وای‌فای»)
 * و کِی خودش از همون‌جا که مونده ادامه بده.
 */
object NetworkMonitor {

    data class State(val connected: Boolean = false, val unmetered: Boolean = false)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var started = false

    @Synchronized
    fun init(context: Context) {
        if (started) return
        started = true
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        _state.value = read(cm)
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _state.value = read(cm)
                }

                override fun onLost(network: Network) {
                    _state.value = read(cm)
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    _state.value = from(caps)
                }
            })
        } catch (e: Exception) {
            // اگه ثبت نشد، به جای قفل شدن دانلودها فرض می‌کنیم اینترنت هست
            e.printStackTrace()
            _state.value = State(connected = true, unmetered = true)
        }
    }

    private fun read(cm: ConnectivityManager): State {
        val net = cm.activeNetwork ?: return State()
        val caps = cm.getNetworkCapabilities(net) ?: return State()
        return from(caps)
    }

    // VALIDATED عمداً چک نمی‌شه؛ روی بعضی شبکه‌ها و با فیلترشکن همیشه false برمی‌گرده
    private fun from(caps: NetworkCapabilities): State = State(
        connected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
        unmetered = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    )

    fun usable(wifiOnly: Boolean): Boolean {
        val s = _state.value
        return s.connected && (!wifiOnly || s.unmetered)
    }

    suspend fun awaitUsable(wifiOnly: Boolean) {
        _state.first { it.connected && (!wifiOnly || it.unmetered) }
    }
}
