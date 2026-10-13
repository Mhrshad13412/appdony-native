package com.appdony.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.widget.Toast

object DownloadHelper {

    fun startDownload(
        context: Context,
        url: String,
        fileName: String
    ) {
        try {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(fileName)
                setDescription("در حال دانلود از اپدونی...")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    fileName
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                val ext = MimeTypeMap.getFileExtensionFromUrl(url).lowercase()
                val mime = when (ext) {
                    "apk" -> "application/vnd.android.package-archive"
                    "zip" -> "application/zip"
                    "rar" -> "application/x-rar-compressed"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }
                setMimeType(mime)

                // کوکی از WebView — برای لینک‌های نیازمند لاگین
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrBlank()) {
                    // setRequestHeader از API 30 — اینجا چک می‌کنیم
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        setRequestHeader("Cookie", cookies)
                        setRequestHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                                    "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        )
                        setRequestHeader("Referer", "https://appdony.ir/")
                    }
                }
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(
                context,
                "دانلود شروع شد ✓\nچک کن: پوشه Downloads",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "خطا در شروع دانلود: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun buildFileName(title: String, url: String): String {
        val clean = title
            .replace(Regex("[^\\w\\u0600-\\u06FF\\s.]"), "")
            .trim()
            .take(50)
            .ifBlank { "appdony_file" }

        val ext = url.substringAfterLast(".", "").substringBefore("?").lowercase()
        val safeExt = if (ext in listOf("apk", "zip", "rar", "pdf") && ext.isNotBlank()) {
            ".$ext"
        } else {
            ".apk"
        }

        return "$clean$safeExt"
    }
}
