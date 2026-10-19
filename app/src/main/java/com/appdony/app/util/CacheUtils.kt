package com.appdony.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object CacheUtils {

    suspend fun getCacheSize(context: Context): String = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val size = getDirSize(cacheDir)
        formatSize(size)
    }

    suspend fun clearCache(context: Context) = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }

    private fun getDirSize(dir: File): Long {
        var size = 0L
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) getDirSize(file) else file.length()
            }
        }
        return size
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        // 👇 غلط املایی اینجا اصلاح شد (digitGroups)
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
