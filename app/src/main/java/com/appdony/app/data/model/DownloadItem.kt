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
}
