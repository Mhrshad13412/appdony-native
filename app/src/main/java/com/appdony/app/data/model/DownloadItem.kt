package com.appdony.app.data.model

data class DownloadItem(
    val id: Long,
    val title: String,
    val status: Int,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val localUri: String?,
    val mimeType: String?,
    val fileName: String?
) {
    fun progress(): Float {
        if (totalBytes <= 0) return 0f
        return (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
    }

    fun sizeText(): String {
        return if (totalBytes > 0) {
            "${formatSize(bytesDownloaded)} / ${formatSize(totalBytes)}"
        } else {
            formatSize(bytesDownloaded)
        }
    }

    private fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun isCompleted(): Boolean = status == android.app.DownloadManager.STATUS_SUCCESSFUL
    fun isFailed(): Boolean = status == android.app.DownloadManager.STATUS_FAILED
    fun isRunning(): Boolean = status == android.app.DownloadManager.STATUS_RUNNING ||
            status == android.app.DownloadManager.STATUS_PENDING ||
            status == android.app.DownloadManager.STATUS_PAUSED
}
