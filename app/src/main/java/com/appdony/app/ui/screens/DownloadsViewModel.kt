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

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            _downloads.value = fetchDownloads()
        }
    }

    private suspend fun fetchDownloads(): List<DownloadItem> = withContext(Dispatchers.IO) {
        val list = mutableListOf<DownloadItem>()
        try {
            val query = DownloadManager.Query().setFilterByStatus(
                DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_FAILED or DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PAUSED
            )
            val cursor: Cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                val titleIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val localUriIdx = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                val sizeIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                do {
                    val id = cursor.getLong(idIdx)
                    val title = cursor.getString(titleIdx) ?: "نامشخص"
                    val status = cursor.getInt(statusIdx)
                    val localUri = cursor.getString(localUriIdx)
                    val totalSize = cursor.getLong(sizeIdx)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIdx)
                    
                    var progress = 0
                    if (totalSize > 0) progress = ((bytesDownloaded * 100) / totalSize).toInt()

                    val filePath = localUri?.replace("file://", "") ?: ""
                    val fileSizeMb = String.format("%.1f MB", totalSize / (1024.0 * 1024.0))

                    list.add(DownloadItem(id, title, status, progress, filePath, fileSizeMb))
                } while (cursor.moveToNext())
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.reversed()
    }
}
