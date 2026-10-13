package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.model.DownloadItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DownloadsViewModel(private val context: Context) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    init {
        startAutoRefresh()
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(1500)
            }
        }
    }

    fun refresh() {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_RUNNING or
            DownloadManager.STATUS_PENDING or
            DownloadManager.STATUS_PAUSED or
            DownloadManager.STATUS_SUCCESSFUL or
            DownloadManager.STATUS_FAILED
        )

        val cursor: Cursor = manager.query(query)
        val list = mutableListOf<DownloadItem>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: ""
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val mime = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                val fileName = localUri?.let { Uri.parse(it).lastPathSegment }

                list.add(
                    DownloadItem(
                        id = id,
                        title = title,
                        status = status,
                        bytesDownloaded = downloaded,
                        totalBytes = total,
                        localUri = localUri,
                        mimeType = mime,
                        fileName = fileName
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()

        // جدیدترین‌ها اول — مرتب‌سازی دستی
        val sorted = list.sortedByDescending { it.id }

        // فقط دانلودهای مربوط به اپدونی
        _downloads.value = sorted.filter {
            it.title.contains("اپدونی", ignoreCase = true) ||
            (it.fileName?.endsWith(".apk") == true)
        }
    }

    fun removeDownload(id: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.remove(id)
        refresh()
    }
}
