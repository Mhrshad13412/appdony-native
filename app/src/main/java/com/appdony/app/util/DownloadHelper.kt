package com.appdony.app.util

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.appdony.app.MainActivity
import com.appdony.app.R
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.preferences.DownloadRecord
import com.appdony.app.data.preferences.DownloadStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URLDecoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** لینک آخرش به تلگرام رسید؛ فایل نیست، باید با اپ تلگرام باز بشه */
class TelegramRedirect(val url: String) : IOException("این لینک تلگرامه؛ با دکمه‌ی خودش از صفحه‌ی برنامه باز کن")

object DownloadHelper {

    const val APK_MIME = "application/vnd.android.package-archive"
    const val MIN_VALID_BYTES = 50_000L
    const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val BINARY_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z")
    private val DOWNLOAD_WORDS = listOf("دانلود", "download", "لینک مستقیم", "دریافت")

    class Resolved(val url: String, val fileName: String?, val mime: String, val size: Long)

    val client: OkHttpClient by lazy {
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

    /**
     * اول لینک رو واقعاً چک می‌کنه (به فایل می‌رسه یا نه). اگه لینک خراب/قفل بود
     * اصلاً وارد صف دانلود نمی‌شه و خطا برمی‌گرده. فقط لینک سالم میره توی صف.
     * اگه لینک آخرش به تلگرام برسه، TelegramRedirect برمی‌گرده (نه دانلود).
     */
    suspend fun startDownload(
        context: Context,
        url: String,
        title: String,
        iconUrl: String? = null,
        wifiOnly: Boolean = false
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext
            if (url.isBlank() || url.toHttpUrlOrNull() == null) {
                return@withContext Result.failure(Exception("این لینک قفله؛ هنوز لینک دانلود واقعی نداره"))
            }
            val store = DownloadStore(app)
            if (store.getAll().any { it.sourceUrl == url && DownloadEngine.isActive(it.id) }) {
                return@withContext Result.failure(Exception("این فایل همین الان توی صف دانلوده"))
            }
            val resolved = try {
                resolve(url)
            } catch (e: TelegramRedirect) {
                return@withContext Result.failure(e)
            } catch (e: UnknownHostException) {
                return@withContext Result.failure(Exception("اتصال به اینترنت برقرار نیست"))
            } catch (e: IOException) {
                return@withContext Result.failure(Exception(e.message ?: "لینک دانلود کار نمی‌کنه"))
            }

            val id = System.currentTimeMillis()
            store.add(
                DownloadRecord(
                    id = id,
                    title = title,
                    iconUrl = iconUrl,
                    fileName = buildFileName(title, resolved),
                    sourceUrl = url,
                    createdAt = id,
                    totalBytes = if (resolved.size > 0) resolved.size else 0L,
                    status = DownloadManager.STATUS_PENDING,
                    message = "در صف دانلود...",
                    wifiOnly = wifiOnly
                )
            )
            DownloadEngine.start(app, id, resolved)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("خطا در شروع دانلود"))
        }
    }

    // لینک رو دنبال می‌کنه تا به فایل واقعی برسه (حداکثر ۴ مرحله)
    // اگه وسط راه یا آخرش به تلگرام رسید، TelegramRedirect پرتاب می‌شه
    fun resolve(startUrl: String, depth: Int = 0): Resolved {
        if (depth > 3) throw IOException("لینک دانلود معتبر نیست (ریدایرکت زیاد)")

        val request = Request.Builder()
            .url(startUrl)
            .header("User-Agent", UA)
            .header("Referer", "https://appdony.ir/")
            .header("Accept", "*/*")
            .get()
            .build()

        client.newCall(request).execute().use { resp ->
            val finalUrl = resp.request.url.toString()

            // بعد از ریدایرکت‌ها به تلگرام رسیدیم
            if (TelegramLinks.isTelegram(finalUrl)) throw TelegramRedirect(finalUrl)

            // ریدایرکتی که اوکی‌اچ‌تی‌تی‌پی دنبالش نمی‌ره (مثل tg://)
            if (resp.code in 300..399) {
                val loc = resp.header("Location").orEmpty().trim()
                if (loc.isNotEmpty() && TelegramLinks.isTelegram(loc)) throw TelegramRedirect(loc)
            }

            if (!resp.isSuccessful) throw IOException("سرور فایل را رد کرد (کد ${resp.code})")

            val type = resp.header("Content-Type").orEmpty().lowercase().substringBefore(';').trim()
            val size = resp.header("Content-Length")?.toLongOrNull() ?: -1L
            val fileName = fileNameFrom(resp.header("Content-Disposition"), finalUrl)

            val isPage = type.startsWith("text/") || type.contains("html") ||
                type.contains("json") || type.contains("xml")

            if (!isPage) {
                // فایل واقعیه؛ ولی اگه حجمش معلومه و خیلی کوچیکه (صفحه‌ی خطا/توکن)، خرابه
                // این چک برای هر پسوندی اعمال می‌شه، نه فقط apk و zip
                if (size in 0L until MIN_VALID_BYTES) throw IOException("فایل روی سرور خراب است")
                return Resolved(finalUrl, fileName, type, size)
            }

            // صفحه‌ی وب: دنبال لینک فایل (یا تلگرام) داخلش می‌گردیم
            val html = resp.body?.string().orEmpty().take(500_000)
            val next = findFileLink(html, finalUrl)
                ?: throw IOException("لینک مستقیم فایل پیدا نشد")
            if (TelegramLinks.isTelegram(next)) throw TelegramRedirect(next)
            if (next == startUrl || next == finalUrl) throw IOException("لینک مستقیم فایل پیدا نشد")
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

    // مثل absolute ولی لینک tg:// رو هم نگه می‌داره
    private fun pick(base: String, raw: String): String? {
        val u = raw.trim()
        return if (u.startsWith("tg:", ignoreCase = true)) u else absolute(base, u)
    }

    private fun findFileLink(html: String, baseUrl: String): String? {
        val doc = Jsoup.parse(html, baseUrl)

        // ریدایرکت متا
        val meta = doc.select("meta[http-equiv=refresh]").firstOrNull()?.attr("content")
        if (!meta.isNullOrBlank()) {
            val target = Regex("url\\s*=\\s*['\"]?([^'\";]+)", RegexOption.IGNORE_CASE).find(meta)?.groupValues?.get(1)
            val abs = target?.let { pick(baseUrl, it) }
            if (abs != null) return abs
        }

        val candidates = doc.select("a[href], [data-href], [data-url], [data-link], [data-download]").mapNotNull { el ->
            val raw = listOf(
                el.attr("href"), el.attr("data-href"), el.attr("data-url"),
                el.attr("data-link"), el.attr("data-download")
            ).firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
            val abs = pick(baseUrl, raw) ?: return@mapNotNull null
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

        // بعد: هر لینک تلگرام داخل صفحه
        candidates.firstOrNull { (u, _) -> TelegramLinks.isTelegram(u) }?.let { return it.first }

        // آخر: ریدایرکت با جاوااسکریپت
        val js = Regex(
            "(?:location\\.href|window\\.location(?:\\.href)?|location\\.replace\\()\\s*=?\\s*\\(?[\"']([^\"']+)[\"']"
        ).find(html)?.groupValues?.get(1)
        return js?.let { pick(baseUrl, it) }
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

    fun sanitize(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')

    fun buildFileName(title: String, resolved: Resolved): String {
        // اسم سرور فقط وقتی قبوله که پسوندش واقعاً فایل نصبی/آرشیو باشه (نه get.php و index.html)
        val server = resolved.fileName?.let { sanitize(it) }
            ?.takeIf {
                it.contains('.') && it.length > 3 &&
                    it.substringAfterLast('.', "").lowercase() in FILE_EXTS
            }
        if (server != null) return server

        val ext = resolved.url.substringBefore('?').substringAfterLast('.', "").lowercase()
            .takeIf { it in FILE_EXTS } ?: "apk"
        val base = sanitize(title).take(60).ifBlank { "appdony_file" }
        return "$base.$ext"
    }

    fun cookieHeader(url: String): String? {
        val http = url.toHttpUrlOrNull() ?: return null
        if (!http.host.endsWith("appdony.ir")) return null
        val cookies = ApiClient.cookieJarInstance?.loadForRequest(http) ?: return null
        return cookies.takeIf { it.isNotEmpty() }?.joinToString("; ") { "${it.name}=${it.value}" }
    }
}

/** موتور دانلود اپدونی: دانلود واقعی با درصد زنده، ادامه بعد از قطعی و چک سالم بودن فایل */
object DownloadEngine {

    data class Live(val title: String, val status: Int, val bytes: Long, val total: Long, val message: String?)

    private const val PENDING = DownloadManager.STATUS_PENDING
    private const val RUNNING = DownloadManager.STATUS_RUNNING
    private const val OK = DownloadManager.STATUS_SUCCESSFUL
    private const val FAILED = DownloadManager.STATUS_FAILED
    private val ZIP_FAMILY = setOf("apk", "xapk", "apks", "apkm", "zip")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    private val pre = ConcurrentHashMap<Long, DownloadHelper.Resolved>()
    private val gate = Semaphore(2)

    private val _live = MutableStateFlow<Map<Long, Live>>(emptyMap())
    val live: StateFlow<Map<Long, Live>> = _live.asStateFlow()

    private val client: OkHttpClient by lazy {
        DownloadHelper.client.newBuilder()
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private class Failure(message: String, val deleteFile: Boolean = false) : IOException(message)

    fun isActive(id: Long): Boolean = jobs[id]?.isActive == true

    private fun setLive(id: Long, value: Live?) {
        while (true) {
            val cur = _live.value
            val next = if (value == null) cur - id else cur + (id to value)
            if (_live.compareAndSet(cur, next)) return
        }
    }

    fun start(context: Context, id: Long, resolved: DownloadHelper.Resolved? = null) {
        val app = context.applicationContext
        if (isActive(id)) return
        val rec = DownloadStore(app).getAll().firstOrNull { it.id == id } ?: return
        if (resolved != null) pre[id] = resolved
        setLive(id, Live(rec.title, PENDING, rec.bytes, rec.totalBytes, "در صف دانلود..."))
        DownloadService.start(app)
        val job = scope.launch(start = CoroutineStart.LAZY) { gate.withPermit { run(app, id) } }
        jobs[id] = job
        job.invokeOnCompletion { jobs.remove(id, job) }
        job.start()
    }

    fun cancelAndDelete(context: Context, id: Long) {
        jobs.remove(id)?.cancel()
        pre.remove(id)
        val store = DownloadStore(context.applicationContext)
        store.getAll().firstOrNull { it.id == id }?.filePath?.let { runCatching { File(it).delete() } }
        store.remove(id)
        setLive(id, null)
    }

    private fun downloadsDir(ctx: Context): File {
        val base = ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: File(ctx.filesDir, "downloads")
        return File(base, "Appdony").apply { mkdirs() }
    }

    private fun uniqueFile(dir: File, name: String, store: DownloadStore, selfId: Long): File {
        val taken = store.getAll().filter { it.id != selfId }.mapNotNull { it.filePath }.toSet()
        var f = File(dir, name)
        if (!f.exists() && f.path !in taken) return f
        val base = name.substringBeforeLast('.', name)
        val ext = name.substringAfterLast('.', "")
        var i = 1
        while (true) {
            f = File(dir, if (ext.isEmpty()) "$base ($i)" else "$base ($i).$ext")
            if (!f.exists() && f.path !in taken) return f
            i++
        }
    }

    private suspend fun run(ctx: Context, id: Long) {
        val store = DownloadStore(ctx)
        var rec = store.getAll().firstOrNull { it.id == id } ?: return
        var lastBytes = rec.bytes
        var lastTotal = rec.totalBytes
        try {
            val resolved = pre.remove(id) ?: try {
                setLive(id, Live(rec.title, PENDING, lastBytes, lastTotal, "در حال آماده‌سازی لینک..."))
                DownloadHelper.resolve(rec.sourceUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                throw Failure("اتصال به اینترنت یا سرور برقرار نیست")
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                throw Failure(e.message ?: "لینک دانلود کار نمی‌کنه")
            }

            val file: File = rec.filePath?.let { File(it) }?.takeIf { it.parentFile?.exists() == true }
                ?: uniqueFile(downloadsDir(ctx), DownloadHelper.buildFileName(rec.title, resolved), store, id)
            file.parentFile?.mkdirs()
            rec = rec.copy(
                fileName = file.name,
                filePath = file.path,
                totalBytes = if (resolved.size > 0) resolved.size else rec.totalBytes,
                status = RUNNING,
                message = null
            )
            store.update(rec)
            lastTotal = rec.totalBytes

            var attempt = 0
            while (true) {
                try {
                    transfer(id, rec.title, resolved.url, file, lastTotal) { b, t ->
                        lastBytes = b
                        if (t > 0) lastTotal = t
                    }
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Failure) {
                    throw e
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    attempt++
                    if (attempt > 4) throw Failure("ارتباط قطع شد؛ «تلاش دوباره» رو بزن، از همین‌جا ادامه می‌ده")
                    setLive(id, Live(rec.title, PENDING, lastBytes, lastTotal, "قطع شد؛ تلاش مجدد ($attempt)..."))
                    delay(1500L * attempt)
                }
            }

            val size = file.length()
            val ext = file.name.substringAfterLast('.', "").lowercase()

            // چک سالم بودن برای هر فایلی، فارغ از پسوند
            if (size < DownloadHelper.MIN_VALID_BYTES) {
                throw Failure("فایل خراب یا ناقص بود", deleteFile = true)
            }
            if (looksLikeText(file)) {
                throw Failure("سرور به‌جای فایل یک صفحه‌ی وب فرستاد؛ این لینک خرابه", deleteFile = true)
            }
            if (ext in ZIP_FAMILY && !hasZipSignature(file)) {
                throw Failure("فایل دریافتی سالم نیست (فایل نصبی نبود)", deleteFile = true)
            }

            store.update(
                rec.copy(
                    status = OK, completed = true, bytes = size, totalBytes = size,
                    localUri = "file://${file.path}", message = null
                )
            )
            setLive(id, null)
            DownloadNotifications.result(ctx, id, rec.title, "دانلود کامل شد ✓")
            AssistantBus.say(
                AssistantSay(
                    "دانلود «${rec.title.removePrefix("دانلود").trim().take(40)}» کامل شد. بزن تا بری سراغ نصبش.",
                    SayKind.DONE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Failure) {
            if (e.deleteFile) rec.filePath?.let { runCatching { File(it).delete() } }
            fail(ctx, store, id, rec, lastBytes, lastTotal, e.message ?: "دانلود ناموفق بود", e.deleteFile)
        } catch (e: Exception) {
            fail(ctx, store, id, rec, lastBytes, lastTotal, e.message ?: "دانلود ناموفق بود", false)
        }
    }

    private fun fail(
        ctx: Context, store: DownloadStore, id: Long, rec: DownloadRecord,
        bytes: Long, total: Long, msg: String, wiped: Boolean
    ) {
        val cur = store.getAll().firstOrNull { it.id == id }
        if (cur != null) {
            store.update(
                cur.copy(
                    status = FAILED, message = msg,
                    bytes = if (wiped) 0L else bytes,
                    totalBytes = if (total > 0) total else cur.totalBytes,
                    filePath = if (wiped) null else cur.filePath
                )
            )
        }
        setLive(id, null)
        if (cur != null) {
            DownloadNotifications.result(ctx, id, rec.title, msg)
            AssistantBus.say(
                AssistantSay(
                    "دانلود «${rec.title.removePrefix("دانلود").trim().take(40)}» انجام نشد: $msg",
                    SayKind.FAIL
                )
            )
        }
    }

    private fun hasZipSignature(f: File): Boolean = try {
        f.inputStream().use { val a = it.read(); val b = it.read(); a == 'P'.code && b == 'K'.code }
    } catch (e: Exception) {
        false
    }

    // اگه اول فایل با < یا { یا [ شروع بشه یعنی HTML/JSON بوده، نه فایل نصبی
    private fun looksLikeText(f: File): Boolean = try {
        f.inputStream().use { input ->
            val buf = ByteArray(512)
            val n = input.read(buf)
            var i = 0
            if (n >= 3 && buf[0] == 0xEF.toByte() && buf[1] == 0xBB.toByte() && buf[2] == 0xBF.toByte()) i = 3
            while (i < n && (buf[i] == ' '.code.toByte() || buf[i] == '\n'.code.toByte() ||
                    buf[i] == '\r'.code.toByte() || buf[i] == '\t'.code.toByte())
            ) i++
            i < n && (buf[i] == '<'.code.toByte() || buf[i] == '{'.code.toByte() || buf[i] == '['.code.toByte())
        }
    } catch (e: Exception) {
        false
    }

    private suspend fun transfer(
        id: Long, title: String, url: String, file: File, knownTotal: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        var existing = if (file.exists()) file.length() else 0L
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", DownloadHelper.UA)
            .header("Referer", "https://appdony.ir/")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")
        DownloadHelper.cookieHeader(url)?.let { builder.header("Cookie", it) }
        if (existing > 0) builder.header("Range", "bytes=$existing-")

        val call = client.newCall(builder.build())
        val handle = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { resp ->
                if (resp.code == 416 && existing > 0) {
                    if (knownTotal <= 0 || existing >= knownTotal) return
                    file.delete()
                    throw IOException("شروع دوباره")
                }
                if (!resp.isSuccessful) throw Failure("سرور فایل را رد کرد (کد ${resp.code})")
                val type = resp.header("Content-Type").orEmpty().lowercase()
                if (type.startsWith("text/") || type.contains("html") ||
                    type.contains("json") || type.contains("xml")
                ) {
                    throw Failure("سرور به‌جای فایل یک صفحه‌ی وب فرستاد؛ این لینک خرابه")
                }
                val body = resp.body ?: throw IOException("پاسخ خالی از سرور")

                val append = resp.code == 206 && existing > 0
                if (!append) existing = 0L
                val len = body.contentLength()
                val total = when {
                    len > 0 -> existing + len
                    knownTotal > 0 -> knownTotal
                    else -> 0L
                }

                var done = existing
                var lastTick = 0L
                FileOutputStream(file, append).use { out ->
                    body.byteStream().use { input ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val now = System.currentTimeMillis()
                            if (now - lastTick >= 400) {
                                lastTick = now
                                onProgress(done, total)
                                setLive(id, Live(title, RUNNING, done, total, null))
                            }
                        }
                        out.flush()
                    }
                }
                onProgress(done, total)
                setLive(id, Live(title, RUNNING, done, total, null))
                if (total > 0 && done < total) throw IOException("اتصال زودتر از موعد بسته شد")
            }
        } finally {
            handle?.dispose()
        }
    }
}

/** اعلان‌های دانلود (درصد زنده + نتیجه) */
object DownloadNotifications {
    const val PROGRESS_ID = 2001
    private const val CH_PROGRESS = "downloads_progress"
    private const val CH_DONE = "downloads_done"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "در حال دانلود", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "نتیجه‌ی دانلود", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun progress(ctx: Context, title: String, text: String, percent: Int): Notification =
        NotificationCompat.Builder(ctx, CH_PROGRESS)
            .setSmallIcon(R.drawable.ic_notification_box)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, percent.coerceIn(0, 100), percent < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp(ctx))
            .build()

    fun result(ctx: Context, id: Long, title: String, text: String) {
        try {
            val n = NotificationCompat.Builder(ctx, CH_DONE)
                .setSmallIcon(R.drawable.ic_notification_box)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx))
                .build()
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify((id % 100000).toInt() + 3000, n)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/** سرویس پیش‌زمینه: تا وقتی دانلود فعاله پروسه زنده می‌مونه و اعلان درصد/حجم نشون می‌ده */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collector: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DownloadNotifications.ensureChannels(this)
        val first = DownloadNotifications.progress(this, "اپدونی", "در حال آماده‌سازی دانلود...", -1)
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            ServiceCompat.startForeground(this, DownloadNotifications.PROGRESS_ID, first, type)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }
        if (collector == null) {
            collector = scope.launch {
                DownloadEngine.live.collect { map ->
                    val active = map.values.filter {
                        it.status == DownloadManager.STATUS_PENDING || it.status == DownloadManager.STATUS_RUNNING
                    }
                    if (active.isEmpty()) {
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .notify(DownloadNotifications.PROGRESS_ID, build(active))
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun build(active: List<DownloadEngine.Live>): Notification {
        if (active.size == 1) {
            val d = active[0]
            return if (d.total > 0) {
                val pct = ((d.bytes * 100) / d.total).toInt()
                DownloadNotifications.progress(this, d.title, "$pct٪ • ${fmt(d.bytes)} از ${fmt(d.total)}", pct)
            } else {
                DownloadNotifications.progress(this, d.title, d.message ?: "در حال دانلود... ${fmt(d.bytes)}", -1)
            }
        }
        val total = active.sumOf { it.total }
        val got = active.sumOf { it.bytes }
        val pct = if (total > 0 && active.all { it.total > 0 }) ((got * 100) / total).toInt() else -1
        return DownloadNotifications.progress(this, "${active.size} دانلود در حال انجام", "${fmt(got)} دانلود شده", pct)
    }

    private fun fmt(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, DownloadService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
