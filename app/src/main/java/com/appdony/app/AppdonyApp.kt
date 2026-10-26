package com.appdony.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.local.AppDatabase
import com.appdony.app.util.CrashReporter

class AppdonyApp : Application(), ImageLoaderFactory {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
        ApiClient.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache { DiskCache.Builder().directory(cacheDir.resolve("image_cache")).maxSizeBytes(100L * 1024 * 1024).build() }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }
}
