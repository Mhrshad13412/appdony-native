package com.appdony.app

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.PopularityRemote
import com.appdony.app.data.repository.HomeRepository
import com.appdony.app.util.AppForeground
import com.appdony.app.util.AutoUpdateWorker
import com.appdony.app.util.CrashReporter
import com.appdony.app.util.DownloadEngine
import com.appdony.app.util.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AppdonyApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        ApiClient.init(this)
        PopularityRemote.init(this)
        AppForeground.install(this)
        NetworkMonitor.init(this)
        resumeDownloads()
        scheduleAutoUpdate()
        prefetchHome()
    }

    // دانلودهایی که وسط کار بودن (پروسه کشته شده بود) همین اول کار از همون‌جا که مونده ادامه پیدا می‌کنن
    private fun resumeDownloads() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                DownloadEngine.resumeInterrupted(this@AppdonyApp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // چک آپدیت خودکار هر ۶ ساعت؛ خود Worker اول تنظیمات رو نگاه می‌کنه و اگه هر دو سوئیچ خاموش باشن بلافاصله تموم می‌شه
    private fun scheduleAutoUpdate() {
        try {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = PeriodicWorkRequestBuilder<AutoUpdateWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "AutoUpdateWork", ExistingPeriodicWorkPolicy.KEEP, request
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ⚠️ مهم — پیش‌بارگذاری صفحه‌ی اصلی؛ تحت هیچ عنوان حذف نشه ⚠️
    // همون اول کار، دسته‌ها و چند ردیف اول هر دو تب پشت صحنه گرفته می‌شن و تو کش دیسکی (ApiClient) می‌شینن؛
    // تا کاربر صفحه رو باز کنه داده آماده‌ست و جای انتظار، فوراً از کش پر می‌شه.
    private fun prefetchHome() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val repo = HomeRepository()
            for (games in listOf(true, false)) {
                launch {
                    try {
                        val structure = repo.loadStructure(games)
                        structure.sections.take(4).forEach { section ->
                            launch { try { repo.loadSection(section, structure.rootId) } catch (e: Exception) { } }
                        }
                    } catch (e: Exception) {
                        // شبکه نبود؛ مهم نیست، خود صفحه دوباره تلاش می‌کنه
                    }
                }
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(100L * 1024 * 1024).build() }
            .okHttpClient { ApiClient.imageClient() }
            .allowRgb565(true) // حافظه‌ی کمتر و دیکد سریع‌تر؛ کیفیت کارت‌های کوچیک عملاً یکیه
            .crossfade(150)
            .respectCacheHeaders(false)
            .build()
    }
}
