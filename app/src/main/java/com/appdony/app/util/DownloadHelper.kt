package com.appdony.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object DownloadHelper {
    suspend fun startDownload(context: Context, url: String, fileName: String, wifiOnly: Boolean = false) {
        Toast.makeText(context, "در حال آماده‌سازی لینک دانلود...", Toast.LENGTH_SHORT).show()
        val finalUrl = resolveDirectUrl(url)
        if (finalUrl == null) {
            Toast.makeText(context, "لینک دانلود مستقیم پیدا نشد!", Toast.LENGTH_LONG).show()
            return
        }

        withContext(Dispatchers.Main) {
            try {
                val request = DownloadManager.Request(Uri.parse(finalUrl)).apply {
                    setTitle(fileName)
                    setDescription("در حال دانلود از اپدونی...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    setAllowedOverMetered(!wifiOnly)
                    setAllowedOverRoaming(false)

                    val ext = MimeTypeMap.getFileExtensionFromUrl(finalUrl).lowercase()
                    val mime = when (ext) {
                        "apk" -> "application/vnd.android.package-archive"
                        "zip" -> "application/zip"
                        "rar" -> "application/x-rar-compressed"
                        else -> "application/octet-stream"
                    }
                    setMimeType(mime)

                    val cookies = CookieManager.getInstance().getCookie(finalUrl)
                    if (!cookies.isNullOrBlank()) {
                        addRequestHeader("Cookie", cookies)
                    }
                    addRequestHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    addRequestHeader("Referer", "https://appdony.ir/")
                }

                val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                manager.enqueue(request)
                Toast.makeText(context, "دانلود شروع شد ✓\nچک کن: پوشه Downloads", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در شروع دانلود: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun resolveDirectUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val finalUrl = response.request.url.toString()
                    val contentType = response.header("Content-Type") ?: ""
                    if (finalUrl.contains(".apk") || contentType.contains("application/vnd.android.package-archive")) {
                        return@withContext finalUrl
                    }
                }
                return@withContext url
            }
        } catch (e: Exception) {
            return@withContext url
        }
    }

    fun buildFileName(title: String, url: String): String {
        val clean = title.replace(Regex("[^\\w\\u0600-\\u06FF\\s.]"), "").trim().take(50).ifBlank { "appdony_file" }
        val ext = url.substringAfterLast(".", "").substringBefore("?").lowercase()
        val safeExt = if (ext.length in 2..5 && ext.all { it.isLetterOrDigit() }) ext else "apk"
        return "$clean.$safeExt"
    }
}
