package com.appdony.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateHelper {
    // آدرس API گیت‌هاب برای گرفتن آخرین ریلیز
    private const val GITHUB_API = "https://api.github.com/repos/Mhrshad13412/appdony-native/releases/latest"
    private const val APK_MIME = "application/vnd.android.package-archive"

    data class UpdateInfo(
        val latestVersion: String,
        val downloadUrl: String,
        val changelog: String,
        val sizeBytes: Long
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    fun currentVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    } catch (e: Exception) {
        "0"
    }

    /** true یعنی remote از local جدیدتره (مثلا 2.9.36 > 2.9.35) */
    fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").split('.')
            .map { it.takeWhile { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    /** فقط وقتی نسخه‌ی گیت‌هاب از نسخه‌ی نصب‌شده جدیدتر باشه چیزی برمی‌گردونه */
    suspend fun checkForUpdate(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(GITHUB_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Appdony-Android")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = JSONObject(response.body?.string() ?: return@withContext null)
                val version = json.getString("tag_name").removePrefix("v")
                if (!isNewer(version, currentVersion(context))) return@withContext null

                val body = json.optString("body", "").ifBlank { "نسخه جدید با بهبود عملکرد و رفع باگ‌ها." }
                val assets = json.getJSONArray("assets")
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.getString("name").endsWith(".apk", ignoreCase = true)) {
                        return@withContext UpdateInfo(
                            latestVersion = version,
                            downloadUrl = a.getString("browser_download_url"),
                            changelog = body,
                            sizeBytes = a.optLong("size", 0L)
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /** دانلود APK توی کش برنامه؛ progress بین ۰ تا ۱. اگه شکست بخوره null */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.cacheDir, "updates").apply {
                    mkdirs()
                    listFiles()?.forEach { it.delete() }
                }
                val out = File(dir, "appdony-${info.latestVersion}.apk")
                val request = Request.Builder()
                    .url(info.downloadUrl)
                    .header("User-Agent", "Appdony-Android")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body ?: return@withContext null
                    val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                    body.byteStream().use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(32 * 1024)
                            var done = 0L
                            var lastPercent = -1
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                output.write(buf, 0, n)
                                done += n
                                if (total > 0) {
                                    val percent = (done * 100 / total).toInt()
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                                    }
                                }
                            }
                        }
                    }
                }
                if (out.length() < 100_000L) {
                    out.delete()
                    null
                } else out
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }

    /** نصب APK؛ اگه اجازه‌ی «نصب از منابع ناشناس» نباشه، صفحه‌ی تنظیماتش باز می‌شه و false برمی‌گرده */
    fun install(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
