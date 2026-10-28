package com.appdony.app.ui.screens

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.preferences.DownloadStore
import com.appdony.app.util.DownloadHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DownloadItem(
    val id: Long,
    val title: String,
    val iconUrl: String?,
    val fileName: String,
    val sourceUrl: String,
    val status: Int,
    val progress: Int,
    val bytes: Long,
    val total: Long,
    val localUri: String?,
    val message: String?
)

class DownloadsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = DownloadStore(application)
    private val manager = application.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private class Row(val status: Int, val bytes: Long, val total: Long, val localUri: String?, val reason: Int)

    init {
        viewModelScope.launch {
            while (isActive) {
                val hasActive = withContext(Dispatchers.IO) { refreshOnce() }
                delay(if (hasActive) 800L else 4000L)
            }
        }
    }

    fun delete(item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                manager?.remove(item.id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            store.remove(item.id)
            refreshOnce()
        }
    }

    fun retry(item: DownloadItem, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val result = DownloadHelper.startDownload(getApplication(), item.sourceUrl, item.title, item.iconUrl)
            if (result.isSuccess) {
                withContext(Dispatchers.IO) {
                    try {
                        manager?.remove(item.id)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    store.remove(item.id)
                    refreshOnce()
                }
            }
            onResult(result)
        }
    }

    // true یعنی هنوز دانلود فعالی هست
    private fun refreshOnce(): Boolean {
        val records = store.getAll()
        val rows = queryRows(records.map { it.id })
        var active = false

        val list = records.map { record ->
            val row = rows[record.id]
            var status = DownloadManager.STATUS_FAILED
            var message: String? = null
            var bytes = 0L
            var total = record.totalBytes
            var localUri = record.localUri

            if (row != null) {
                status = row.status
                bytes = row.bytes
                if (row.total > 0) total = row.total
                if (row.localUri != null) localUri = row.localUri

                when (row.status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        val corrupt = total in 1L until DownloadHelper.MIN_VALID_BYTES &&
                            DownloadHelper.isBinaryName(record.fileName)
                        if (corrupt) {
                            status = DownloadManager.STATUS_FAILED
                            message = "فایل ناقص یا خراب دانلود شد"
                        } else if (!record.completed) {
                            store.update(record.copy(completed = true, localUri = localUri, totalBytes = total))
                        }
                        bytes = total
                    }
                    DownloadManager.STATUS_FAILED -> message = failMessage(row.reason)
                    DownloadManager.STATUS_PAUSED -> {
                        message = pausedMessage(row.reason)
                        active = true
                    }
                    else -> active = true
                }
            } else if (record.completed) {
                // سیستم رکورد رو پاک کرده ولی ما نگهش داشتیم
                status = DownloadManager.STATUS_SUCCESSFUL
                bytes = total
            } else {
                message = "این دانلود از سیستم حذف شده"
            }

            val progress = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0

            DownloadItem(
                id = record.id,
                title = record.title,
                iconUrl = record.iconUrl,
                fileName = record.fileName,
                sourceUrl = record.sourceUrl,
                status = status,
                progress = progress,
                bytes = bytes,
                total = total,
                localUri = localUri,
                message = message
            )
        }

        _items.value = list
        return active
    }

    private fun queryRows(ids: List<Long>): Map<Long, Row> {
        val dm = manager ?: return emptyMap()
        if (ids.isEmpty()) return emptyMap()
        val result = HashMap<Long, Row>()
        try {
            dm.query(DownloadManager.Query().setFilterById(*ids.toLongArray()))?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
                val statusIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                val bytesIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val totalIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val uriIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)
                val reasonIdx = c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
                while (c.moveToNext()) {
                    result[c.getLong(idIdx)] = Row(
                        status = c.getInt(statusIdx),
                        bytes = c.getLong(bytesIdx),
                        total = c.getLong(totalIdx),
                        localUri = c.getString(uriIdx),
                        reason = c.getInt(reasonIdx)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    private fun failMessage(reason: Int): String = when {
        reason == DownloadManager.ERROR_INSUFFICIENT_SPACE -> "فضای گوشی کافی نیست"
        reason == DownloadManager.ERROR_CANNOT_RESUME -> "امکان ادامه‌ی دانلود نیست"
        reason == DownloadManager.ERROR_FILE_ERROR || reason == DownloadManager.ERROR_DEVICE_NOT_FOUND ->
            "خطا در ذخیره‌ی فایل"
        reason == DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "لینک دانلود معتبر نیست"
        reason in 400..599 -> "سرور فایل را رد کرد (کد $reason)"
        else -> "دانلود ناموفق بود"
    }

    private fun pausedMessage(reason: Int): String = when (reason) {
        DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "در انتظار اینترنت..."
        DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "در انتظار وای‌فای..."
        DownloadManager.PAUSED_WAITING_TO_RETRY -> "در انتظار تلاش مجدد..."
        else -> "متوقف شد"
    }
}
