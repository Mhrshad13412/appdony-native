package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val progress: Int,
    val filePath: String,
    val size: String
)

class DownloadsViewModel(private val context: Context) : ViewModel() {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _downloads.value = fetchDownloads()
        }
    }

    // 👈 استفاده مستقیم از متد اندروید بدون reflection (چون minSdk 24 هست)
    fun pauseDownload(id: Long) {
        try {
            downloadManager?.pauseDownload(id)
            loadDownloads() // آپدیت کردن لیست برای نمایش وضعیت جدید
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 👈 استفاده مستقیم از متد اندروید
    fun resumeDownload(id: Long) {
        try {
            downloadManager?.resumeDownload(id)
            loadDownloads()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadItem>()
        if (downloadManager == null) return@withContext list

        var cursor: Cursor? = null
        try {
            val query = DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_FAILED or 
                DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED
            )
            cursor = downloadManager.query(query)
            
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val sizeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                do {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "نامشخص" else "نامشخص"
                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_FAILED
                    val localUri = if (localUriIdx >= 0) cursor.getString(localUriIdx) else null
                    val totalSize = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else 0L
                    val bytesDownloaded = if (bytesDownloadedIdx >= 0) cursor.getLong(bytesDownloadedIdx) else 0L
                    
                    var progress = 0
                    if (totalSize > 0) progress = ((bytesDownloaded * 100) / totalSize).toInt()

                    val filePath = localUri?.replace("file://", "") ?: ""
                    val fileSizeMb = String.format("%.1f MB", totalSize / (1024.0 * 1024.0))

                    list.add(DownloadItem(id, title, status, progress, filePath, fileSizeMb))
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        list.reversed()
    }
}
