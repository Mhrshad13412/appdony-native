package com.appdony.app.ui.screens

import android.app.Application
import android.app.DownloadManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.preferences.DownloadStore
import com.appdony.app.util.DownloadEngine
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

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    // تا اولین بارگذاری تموم نشده، اسکلت براق نشون داده می‌شه
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    init {
        viewModelScope.launch {
            // هر تغییر زنده‌ی موتور دانلود (درصد/حجم) فوراً توی لیست دیده می‌شه
            DownloadEngine.live.collect {
                withContext(Dispatchers.IO) { refreshOnce() }
            }
        }
        viewModelScope.launch {
            delay(450L)
            while (isActive) {
                withContext(Dispatchers.IO) { refreshOnce() }
                _loaded.value = true
                delay(3000L)
            }
        }
    }

    fun delete(item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            DownloadEngine.cancelAndDelete(getApplication(), item.id)
            refreshOnce()
        }
    }

    /** ادامه‌ی همون دانلود (از جایی که مونده)، نه شروع از صفر */
    fun retry(item: DownloadItem, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val rec = store.getAll().firstOrNull { it.id == item.id }
            if (rec == null) {
                withContext(Dispatchers.Main) { onResult(Result.failure(Exception("این مورد دیگه وجود نداره"))) }
                return@launch
            }
            store.update(rec.copy(status = DownloadManager.STATUS_PENDING, message = "در حال آماده‌سازی لینک..."))
            DownloadEngine.start(getApplication(), rec.id)
            refreshOnce()
            withContext(Dispatchers.Main) { onResult(Result.success(rec.id)) }
        }
    }

    private fun refreshOnce() {
        val live = DownloadEngine.live.value
        val list = store.getAll().map { record ->
            val l = live[record.id]
            var status: Int
            var bytes = record.bytes
            var total = record.totalBytes
            var message = record.message

            if (l != null) {
                status = l.status
                bytes = l.bytes
                if (l.total > 0) total = l.total
                message = l.message
            } else {
                status = when {
                    record.completed || record.status == DownloadManager.STATUS_SUCCESSFUL -> DownloadManager.STATUS_SUCCESSFUL
                    record.status == DownloadManager.STATUS_FAILED -> DownloadManager.STATUS_FAILED
                    record.status == 0 -> DownloadManager.STATUS_FAILED // رکورد قدیمی نسخه‌های قبل
                    else -> {
                        // وضعیت «در حال دانلود» ثبت شده ولی موتور فعال نیست = اپ وسط کار بسته شده
                        message = "دانلود نیمه‌کاره ماند؛ «تلاش دوباره» رو بزن تا ادامه پیدا کنه"
                        DownloadManager.STATUS_FAILED
                    }
                }
                if (status == DownloadManager.STATUS_FAILED && message.isNullOrBlank()) {
                    message = "دانلود ناموفق بود"
                }
                if (status == DownloadManager.STATUS_SUCCESSFUL) bytes = total.takeIf { it > 0 } ?: bytes
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
                localUri = record.localUri,
                message = message
            )
        }
        _items.value = list
    }
}
