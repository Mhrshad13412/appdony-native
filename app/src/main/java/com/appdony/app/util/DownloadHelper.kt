package com.appdony.app.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.preferences.DownloadRecord
import com.appdony.app.data.preferences.DownloadStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object DownloadHelper {

    const val APK_MIME = "application/vnd.android.package-archive"
    const val MIN_VALID_BYTES = 50_000L

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val BINARY_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z")
    private val DOWNLOAD_WORDS = listOf("دانلود", "download", "لینک مستقیم", "دریافت")

    private class Resolved(val url: String, val fileName: String?, val mime: String, val size: Long)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { ApiClient.cookieJarInstance?.let { cookieJar(it) } }
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    fun isBinaryName(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in BINARY_EXTS

    suspend fun startDownload(
        context: Context,
        url: String,
        title: String,
        iconUrl: String? = null,
        wifiOnly: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val appContext = context.applicationContext

            val resolved = resolve(url, 0)
                ?: return@withContext Result.failure(Exception("خطا در دریافت لینک دانلود"))

            val fileName = buildFileName(title, resolved)
            val ext = fileName.substringAfterLast('.', "").lowercase()
            val mime = when {
                ext == "apk" -> APK_MIME
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            }

            val request = DownloadManager.Request(Uri.parse(resolved.url)).apply {
                setTitle(title)
                setDescription("دانلود از اپدونی")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Appdony/$fileName")
                setAllowedOverMetered(!wifiOnly)
                setAllowedOverRoaming(true)
                setMimeType(mime)
                addRequestHeader("User-Agent", UA)
                addRequestHeader("Referer", "https://appdony.ir/")
                cookieHeader(resolved.url)?.let { addRequestHeader("Cookie", it) }
            }

            val manager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = manager.enqueue(request)

            DownloadStore(appContext).add(
                DownloadRecord(
                    id = id,
                    title = title,
                    iconUrl = iconUrl,
                    fileName = fileName,
                    sourceUrl = url,
                    createdAt = System.currentTimeMillis(),
                    totalBytes = if (resolved.size > 0) resolved.size else 0L
                )
            )
            Result.success(id)
        } catch (e: UnknownHostException) {
            Result.failure(Exception("اتصال به اینترنت برقرار نیست."))
        } catch (e: Exception) {
            Result.failure(Exception("خطا در شروع دانلود"))
        }
    }

    // لینک رو دنبال می‌کنه تا به فایل واقعی برسه (حداکثر ۴ مرحله)
    private fun resolve(startUrl: String, depth: Int): Resolved? {
        if (depth > 3) return null

        val request = Request.Builder()
            .url(startUrl)
            .header("User-Agent", UA)
            .header("Referer", "https://appdony.ir/")
            .header("Accept", "*/*")
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null

            val finalUrl = resp.request.url.toString()
            val type = resp.header("Content-Type").orEmpty().lowercase().substringBefore(';').trim()
            val size = resp.header("Content-Length")?.toLongOrNull() ?: -1L
            val fileName = fileNameFrom(resp.header("Content-Disposition"), finalUrl)
            val ext = (fileName ?: finalUrl.substringBefore('?')).substringAfterLast('.', "").lowercase()

            val isPage = type.startsWith("text/") || type.contains("html") ||
                type.contains("json") || type.contains("xml")

            if (!isPage) {
                // فایل واقعیه؛ فقط اگه خیلی کوچیک بود خرابه
                if (ext in BINARY_EXTS && size in 0L until MIN_VALID_BYTES) return null
                return Resolved(finalUrl, fileName, type, size)
            }

            // صفحه‌ی وب: دنبال لینک فایل داخلش می‌گردیم
            val html = resp.body?.string().orEmpty().take(500_000)
            val next = findFileLink(html, finalUrl) ?: return null
            if (next == startUrl || next == finalUrl) return null
            return resolve(next, depth + 1)
        }
    }

    private fun absolute(base: String, raw: String): String? {
        val u = raw.trim()
        if (u.isBlank() || u.startsWith("#") || u.startsWith("javascript:") || u.startsWith("mailto:")) return null
        return try {
            base.toHttpUrlOrNull()?.resolve(u)?.toString()
        } catch (e: Exception) {
            null
        }
    }

    private fun findFileLink(html: String, baseUrl: String): String? {
        val doc = Jsoup.parse(html, baseUrl)

        // ریدایرکت متا
        val meta = doc.select("meta[http-equiv=refresh]").firstOrNull()?.attr("content")
        if (!meta.isNullOrBlank()) {
            val target = Regex("url\\s*=\\s*['\"]?([^'\";]+)", RegexOption.IGNORE_CASE).find(meta)?.groupValues?.get(1)
            val abs = target?.let { absolute(baseUrl, it) }
            if (abs != null) return abs
        }

        val candidates = doc.select("a[href], [data-href], [data-url], [data-link], [data-download]").mapNotNull { el ->
            val raw = listOf(
                el.attr("href"), el.attr("data-href"), el.attr("data-url"),
                el.attr("data-link"), el.attr("data-download")
            ).firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
            val abs = absolute(baseUrl, raw) ?: return@mapNotNull null
            Pair(abs, el.text().trim())
        }

        // اول: لینکی که پسوند فایل داره
        candidates.firstOrNull { (u, _) ->
            u.substringBefore('?').substringAfterLast('.', "").lowercase() in FILE_EXTS
        }?.let { return it.first }

        // بعد: لینکی که متنش «دانلود» داره
        candidates.firstOrNull { (u, text) ->
            u != baseUrl && DOWNLOAD_WORDS.any { text.contains(it, ignoreCase = true) }
        }?.let { return it.first }

        // آخر: ریدایرکت با جاوااسکریپت
        val js = Regex(
            "(?:location\\.href|window\\.location(?:\\.href)?|location\\.replace\\()\\s*=?\\s*\\(?[\"']([^\"']+)[\"']"
        ).find(html)?.groupValues?.get(1)
        return js?.let { absolute(baseUrl, it) }
    }

    private fun fileNameFrom(disposition: String?, url: String): String? {
        if (!disposition.isNullOrBlank()) {
            val star = Regex("filename\\*\\s*=\\s*(?:UTF-8|utf-8)''([^;]+)").find(disposition)?.groupValues?.get(1)
            val plain = Regex("filename\\s*=\\s*\"?([^\";]+)\"?").find(disposition)?.groupValues?.get(1)
            val raw = star ?: plain
            if (!raw.isNullOrBlank()) {
                return try {
                    URLDecoder.decode(raw.trim(), "UTF-8")
                } catch (e: Exception) {
                    raw.trim()
                }
            }
        }
        val segment = url.substringBefore('?').substringAfterLast('/')
        val decoded = try {
            URLDecoder.decode(segment, "UTF-8")
        } catch (e: Exception) {
            segment
        }
        return decoded.takeIf { it.contains('.') && it.length < 120 }
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')

    private fun buildFileName(title: String, resolved: Resolved): String {
        val server = resolved.fileName?.let { sanitize(it) }?.takeIf { it.contains('.') && it.length > 3 }
        if (server != null) return server

        val ext = resolved.url.substringBefore('?').substringAfterLast('.', "").lowercase()
            .takeIf { it in FILE_EXTS } ?: "apk"
        val base = sanitize(title).take(60).ifBlank { "appdony_file" }
        return "$base.$ext"
    }

    private fun cookieHeader(url: String): String? {
        val http = url.toHttpUrlOrNull() ?: return null
        if (!http.host.endsWith("appdony.ir")) return null
        val cookies = ApiClient.cookieJarInstance?.loadForRequest(http) ?: return null
        return cookies.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    }
}
