package com.appdony.app.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** وقتی اپ از پس‌زمینه برمی‌گرده خبر می‌ده (با مدت پس‌زمینه بودن به میلی‌ثانیه) */
object AppForeground {

    private val _resumed = MutableSharedFlow<Long>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val resumed: SharedFlow<Long> = _resumed

    /** true یعنی یکی از صفحه‌های اپ جلوی کاربره (پاپیِ بیرونی باید قایم باشه) */
    private val _inForeground = MutableStateFlow(false)
    val inForeground: StateFlow<Boolean> = _inForeground

    private var startedCount = 0
    private var stoppedAt = 0L

    fun install(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (startedCount == 0 && stoppedAt > 0L) {
                    _resumed.tryEmit(System.currentTimeMillis() - stoppedAt)
                }
                startedCount++
                _inForeground.value = true
                // اگه پاپیِ بیرونی روشنه، سرویسش همین‌جا (وقتی اپ جلوئه) بالا میاد
                CatOverlayService.startIfEnabled(activity)
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount = (startedCount - 1).coerceAtLeast(0)
                if (startedCount == 0) {
                    stoppedAt = System.currentTimeMillis()
                    _inForeground.value = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
