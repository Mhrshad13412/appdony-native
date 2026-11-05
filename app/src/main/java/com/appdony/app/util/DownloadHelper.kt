package com.appdony.app.util

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.appdony.app.MainActivity
import com.appdony.app.R
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.PostMemory
import com.appdony.app.data.preferences.DownloadRecord
import com.appdony.app.data.preferences.DownloadStore
import com.appdony.app.data.preferences.SettingsManager
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
import kotlinx.coroutines.flow.first
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
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URLDecoder
import java.net.UnknownHostException
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

/** لینک آخرش به تلگرام رسید؛ فایل نیست، باید با اپ تلگرام باز بشه */
class TelegramRedirect(val url: String) : IOException("این لینک تلگرامه؛ با دکمه‌ی خودش از صفحه‌ی برنامه باز کن")

object DownloadHelper {

    const val APK_MIME = "application/vnd.android.package-archive"
    const val MIN_VALID_BYTES = 50_000L
    const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
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

    /**
     * اول لینک رو واقعاً چک می‌کنه (به فایل می‌رسه یا نه). اگه لینک خراب/قفل بود
     * اصلاً وارد صف دانلود نمی‌شه و خطا برمی‌گرده. فقط لینک سالم میره توی صف.
     * اگه لینک آخرش به تلگرام برسه، TelegramRedirect برمی‌گرده (نه دانلود).
     * اگه همین فایل قبلاً متوقف شده بود، از همون‌جا که مونده ادامه پیدا می‌کنه.
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
            // همین فایل قبلاً متوقف شده؛ شروع از صفر نکن، ادامه بده
            store.getAll().firstOrNull {
                it.sourceUrl == url && !it.completed && it.status == DownloadManager.STATUS_PAUSED
            }?.let { paused ->
                DownloadEngine.start(app, paused.id)
                return@withContext Result.success(paused.id)
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

            // پستی که کاربر داره ازش دانلود می‌کنه (برای آپدیت خودکار برنامه‌های دانلودشده)
            val post = PostMemory.findByTitle(title)

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
                    wifiOnly = wifiOnly,
                    postId = post?.id ?: 0,
                    postModified = post?.modified
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

/**
 * موتور دانلود اپدونی: دانلود واقعی با درصد زنده، توقف/ادامه، ادامه بعد از قطعی
 * (حتی اگه اینترنت قطع بشه یا وای‌فای نباشه صبر می‌کنه و خودش ادامه می‌ده)
 * و چک سالم بودن فایل
 */
object DownloadEngine {

    data class Live(val title: String, val status: Int, val bytes: Long, val total: Long, val message: String?)

    private const val PENDING = DownloadManager.STATUS_PENDING
    private const val RUNNING = DownloadManager.STATUS_RUNNING
    private const val PAUSED = DownloadManager.STATUS_PAUSED
    private const val OK = DownloadManager.STATUS_SUCCESSFUL
    private const val FAILED = DownloadManager.STATUS_FAILED
    private val ZIP_FAMILY = setOf("apk", "xapk", "apks", "apkm", "zip")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    // کارِ لغوشده‌ای که هنوز داره جمع می‌کنه؛ کار جدید اول صبر می‌کنه تموم بشه (دو نفر با هم توی یه فایل ننویسن)
    private val stopping = ConcurrentHashMap<Long, Job>()
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

    private fun currentBytes(rec: DownloadRecord): Long =
        rec.filePath?.let { File(it) }?.takeIf { it.exists() }?.length() ?: rec.bytes

    fun start(context: Context, id: Long, resolved: DownloadHelper.Resolved? = null) {
        val app = context.applicationContext
        NetworkMonitor.init(app)
        val store = DownloadStore(app)
        val rec = store.getAll().firstOrNull { it.id == id } ?: return
        if (rec.completed) return
        synchronized(jobs) {
            if (isActive(id)) return
            if (resolved != null) pre[id] = resolved
            // اگه قبلاً متوقف/ناموفق بود، وضعیت ذخیره‌شده هم «در صف» بشه تا بعد از کشته شدن پروسه دوباره ادامه پیدا کنه
            if (rec.status != PENDING && rec.status != RUNNING) {
                store.update(rec.copy(status = PENDING, message = "در صف دانلود..."))
            }
            DownloadNotifications.cancelResult(app, id)
            setLive(id, Live(rec.title, PENDING, currentBytes(rec), rec.totalBytes, "در صف دانلود..."))
            val job = scope.launch(start = CoroutineStart.LAZY) { runDownload(app, id) }
            jobs[id] = job
            job.invokeOnCompletion { jobs.remove(id, job) }
            job.start()
        }
        DownloadService.start(app)
    }

    /** توقف دستی: فایل نیمه‌کاره می‌مونه و با «ادامه» از همون‌جا گرفته می‌شه */
    fun pause(context: Context, id: Long) {
        val app = context.applicationContext
        val store = DownloadStore(app)
        val rec = store.getAll().firstOrNull { it.id == id } ?: return
        if (rec.completed) return
        val live = _live.value[id]
        jobs.remove(id)?.let {
            it.cancel()
            stopping[id] = it
        }
        pre.remove(id)
        store.update(
            rec.copy(
                status = PAUSED,
                bytes = live?.bytes ?: currentBytes(rec),
                totalBytes = if ((live?.total ?: 0L) > 0) live!!.total else rec.totalBytes,
                message = "متوقف شد"
            )
        )
        setLive(id, null)
        DownloadNotifications.paused(app, id, rec.title)
    }

    fun pauseAll(context: Context) {
        _live.value.keys.toList().forEach { pause(context, it) }
    }

    fun cancelAndDelete(context: Context, id: Long) {
        jobs.remove(id)?.cancel()
        stopping.remove(id)?.cancel()
        pre.remove(id)
        val app = context.applicationContext
        val store = DownloadStore(app)
        store.getAll().firstOrNull { it.id == id }?.filePath?.let { runCatching { File(it).delete() } }
        store.remove(id)
        setLive(id, null)
        DownloadNotifications.cancelResult(app, id)
    }

    /**
     * دانلودهایی که وسط کار بودن (پروسه کشته شد، گوشی ریستارت شد و ...) رو دوباره راه می‌ندازه.
     * اونایی که کاربر خودش متوقف کرده یا ناموفق شدن دست نمی‌خورن.
     */
    fun resumeInterrupted(context: Context): Int {
        val app = context.applicationContext
        var n = 0
        DownloadStore(app).getAll().forEach { r ->
            if (!r.completed && (r.status == PENDING || r.status == RUNNING) && !isActive(r.id)) {
                start(app, r.id)
                n++
            }
        }
        return n
    }

    /** وقتی سیستم زمان سرویس پیش‌زمینه رو تموم کرد (اندروید ۱۵)؛ دانلودها نیمه‌کاره می‌مونن و بعداً ادامه پیدا می‌کنن */
    fun interruptAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _live.value = emptyMap()
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

    private fun isNetworkError(e: IOException): Boolean =
        e is UnknownHostException || e is SocketException || e is SocketTimeoutException || e is SSLException

    private suspend fun wifiOnly(ctx: Context, rec: DownloadRecord): Boolean =
        rec.wifiOnly || SettingsManager(ctx).wifiOnly.first()

    // اگه اینترنت (یا وای‌فای، وقتی «فقط وای‌فای» روشنه) نیست، همین‌جا صبر می‌کنه تا برگرده
    private suspend fun waitUsable(id: Long, title: String, bytes: Long, total: Long, wifiOnly: Boolean) {
        if (NetworkMonitor.usable(wifiOnly)) return
        val msg = if (NetworkMonitor.state.value.connected) "در انتظار وای‌فای..." else "در انتظار اینترنت..."
        setLive(id, Live(title, PENDING, bytes, total, msg))
        NetworkMonitor.awaitUsable(wifiOnly)
    }

    private suspend fun resolveWithRetry(
        ctx: Context, id: Long, rec: DownloadRecord, bytes: Long, total: Long
    ): DownloadHelper.Resolved {
        var tries = 0
        while (true) {
            val wifi = wifiOnly(ctx, rec)
            waitUsable(id, rec.title, bytes, total, wifi)
            setLive(id, Live(rec.title, PENDING, bytes, total, "در حال آماده‌سازی لینک..."))
            try {
                return DownloadHelper.resolve(rec.sourceUrl)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                if (isNetworkError(e)) {
                    if (!NetworkMonitor.usable(wifi)) continue
                    tries++
                    if (tries > 4) throw Failure("اتصال به اینترنت یا سرور برقرار نیست")
                    delay(2000L * tries)
                } else {
                    throw Failure(e.message ?: "لینک دانلود کار نمی‌کنه")
                }
            }
        }
    }

    private suspend fun runDownload(ctx: Context, id: Long) {
        stopping.remove(id)?.join()
        val store = DownloadStore(ctx)
        var rec = store.getAll().firstOrNull { it.id == id } ?: return
        var lastBytes = currentBytes(rec)
        var lastTotal = rec.totalBytes
        try {
            val resolved = pre.remove(id) ?: resolveWithRetry(ctx, id, rec, lastBytes, lastTotal)

            val file: File = rec.filePath?.let { File(it) }?.takeIf { it.parentFile?.exists() == true }
                ?: uniqueFile(downloadsDir(ctx), DownloadHelper.buildFileName(rec.title, resolved), store, id)
            file.parentFile?.mkdirs()
            currentCoroutineContext().ensureActive()
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
                val wifi = wifiOnly(ctx, rec)
                waitUsable(id, rec.title, lastBytes, lastTotal, wifi)
                val startBytes = lastBytes
                try {
                    gate.withPermit {
                        transfer(id, rec.title, resolved.url, file, lastTotal, wifi) { b, t ->
                            lastBytes = b
                            if (t > 0) lastTotal = t
                        }
                    }
                    break
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Failure) {
                    throw e
                } catch (e: IOException) {
                    currentCoroutineContext().ensureActive()
                    // اگه وسط راه چیزی گرفته شده، شمارنده‌ی تلاش از نو شروع بشه
                    if (lastBytes > startBytes) attempt = 0
                    if (!NetworkMonitor.usable(wifi)) {
                        // مشکل از اینترنته؛ شکست حساب نمی‌شه، بالای حلقه صبر می‌کنه تا برگرده
                        setLive(id, Live(rec.title, PENDING, lastBytes, lastTotal, "ارتباط قطع شد؛ در انتظار اینترنت..."))
                        continue
                    }
                    attempt++
                    if (attempt > 6) throw Failure("ارتباط قطع شد؛ «تلاش دوباره» رو بزن، از همین‌جا ادامه می‌ده")
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
            DownloadNotifications.result(ctx, id, rec.title, msg, retry = true)
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
        id: Long, title: String, url: String, file: File, knownTotal: Long, wifiOnly: Boolean,
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
                                // اینترنت رفت، یا «فقط وای‌فای» روشنه و روی دیتا افتادیم: همین‌جا قطع کن؛ بالا صبر می‌کنه و ادامه می‌ده
                                val net = NetworkMonitor.state.value
                                if (!net.connected || (wifiOnly && !net.unmetered)) {
                                    throw IOException("ارتباط یا وای‌فای قطع شد")
                                }
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

/** اعلان‌های دانلود (درصد زنده + دکمه‌های توقف/ادامه/لغو + نتیجه) */
object DownloadNotifications {
    const val PROGRESS_ID = 2001
    const val ACTION_PAUSE = "com.appdony.app.DL_PAUSE"
    const val ACTION_PAUSE_ALL = "com.appdony.app.DL_PAUSE_ALL"
    const val ACTION_RESUME = "com.appdony.app.DL_RESUME"
    const val ACTION_CANCEL = "com.appdony.app.DL_CANCEL"
    const val EXTRA_ID = "dl_id"

    private const val CH_PROGRESS = "downloads_progress"
    private const val CH_DONE = "downloads_done"
    private const val CH_PAUSED = "downloads_paused"

    private fun notifId(id: Long): Int = (id % 100000).toInt() + 3000

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CH_PROGRESS, "در حال دانلود", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "نتیجه‌ی دانلود", NotificationManager.IMPORTANCE_DEFAULT)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_PAUSED, "دانلودهای متوقف‌شده", NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) }
        )
    }

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(ctx: Context, act: String, id: Long): PendingIntent {
        val i = Intent(ctx, DownloadActionReceiver::class.java)
            .setAction(act)
            .setData(Uri.parse("appdony://download/$id"))
            .putExtra(EXTRA_ID, id)
        return PendingIntent.getBroadcast(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** singleId: اگه فقط یه دانلود فعاله، دکمه‌های «توقف» و «لغو» همون؛ وگرنه «توقف همه» */
    fun progress(ctx: Context, title: String, text: String, percent: Int, singleId: Long? = null): Notification {
        val b = NotificationCompat.Builder(ctx, CH_PROGRESS)
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
        if (singleId != null) {
            b.addAction(R.drawable.ic_notification_box, "توقف", action(ctx, ACTION_PAUSE, singleId))
            b.addAction(R.drawable.ic_notification_box, "لغو", action(ctx, ACTION_CANCEL, singleId))
        } else {
            b.addAction(R.drawable.ic_notification_box, "توقف همه", action(ctx, ACTION_PAUSE_ALL, 1L))
        }
        return b.build()
    }

    /** اعلان «متوقف شد» با دکمه‌ی ادامه؛ حتی وقتی سرویس خاموشه از همین اعلان می‌شه ادامه داد */
    fun paused(ctx: Context, id: Long, title: String) {
        try {
            ensureChannels(ctx)
            val n = NotificationCompat.Builder(ctx, CH_PAUSED)
                .setSmallIcon(R.drawable.ic_notification_box)
                .setContentTitle(title)
                .setContentText("دانلود متوقف شد")
                .setOngoing(false)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setContentIntent(openApp(ctx))
                .addAction(R.drawable.ic_notification_box, "ادامه", action(ctx, ACTION_RESUME, id))
                .addAction(R.drawable.ic_notification_box, "لغو", action(ctx, ACTION_CANCEL, id))
                .build()
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notifId(id), n)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelResult(ctx: Context, id: Long) {
        try {
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(notifId(id))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun result(ctx: Context, id: Long, title: String, text: String, retry: Boolean = false) {
        try {
            ensureChannels(ctx)
            val b = NotificationCompat.Builder(ctx, CH_DONE)
                .setSmallIcon(R.drawable.ic_notification_box)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(openApp(ctx))
            if (retry) b.addAction(R.drawable.ic_notification_box, "تلاش دوباره", action(ctx, ACTION_RESUME, id))
            (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(notifId(id), b.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/** دکمه‌های اعلان دانلود */
class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadNotifications.EXTRA_ID, 0L)
        when (intent.action) {
            DownloadNotifications.ACTION_PAUSE -> if (id != 0L) DownloadEngine.pause(context, id)
            DownloadNotifications.ACTION_PAUSE_ALL -> DownloadEngine.pauseAll(context)
            DownloadNotifications.ACTION_RESUME -> if (id != 0L) DownloadEngine.start(context, id)
            DownloadNotifications.ACTION_CANCEL -> if (id != 0L) {
                DownloadEngine.cancelAndDelete(context, id)
                DownloadNotifications.cancelResult(context, id)
            }
        }
    }
}

/**
 * سرویس پیش‌زمینه: تا وقتی دانلود فعاله (یا منتظر اینترنته) پروسه زنده می‌مونه، حتی با بستن برنامه.
 * START_STICKY: اگه سیستم پروسه رو کشت، سرویس دوباره بالا میاد و دانلودهای نیمه‌کاره ادامه پیدا می‌کنن.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collector: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NetworkMonitor.init(this)
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
        if (intent == null) {
            // سیستم بعد از کشته شدن پروسه سرویس رو دوباره بالا آورده؛ دانلودهای نیمه‌کاره رو ادامه بده
            DownloadEngine.resumeInterrupted(this)
        }
        if (collector == null) {
            collector = scope.launch {
                DownloadEngine.live.collect { map ->
                    val active = map.filterValues {
                        it.status == DownloadManager.STATUS_PENDING || it.status == DownloadManager.STATUS_RUNNING
                    }
                    if (active.isEmpty()) {
                        releaseWake()
                        ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collect
                    }
                    acquireWake()
                    (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                        .notify(DownloadNotifications.PROGRESS_ID, build(active))
                }
            }
        }
        return START_STICKY
    }

    // اندروید ۱۵: سرویس dataSync بعد از چند ساعت تایم‌اوت می‌شه؛ باید همین‌جا تمومش کنیم
    override fun onTimeout(startId: Int, fgsType: Int) {
        DownloadEngine.interruptAll()
        stopSelf()
    }

    private fun acquireWake() {
        try {
            val wl = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "appdony:download")
                .apply { setReferenceCounted(false) }
                .also { wakeLock = it }
            if (!wl.isHeld) wl.acquire(10 * 60 * 1000L)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseWake() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun build(active: Map<Long, DownloadEngine.Live>): Notification {
        if (active.size == 1) {
            val (id, d) = active.entries.first()
            val pct = if (d.total > 0) ((d.bytes * 100) / d.total).toInt().coerceIn(0, 100) else -1
            val text = when {
                d.status == DownloadManager.STATUS_PENDING && !d.message.isNullOrBlank() -> d.message
                d.total > 0 -> "$pct٪ • ${fmt(d.bytes)} از ${fmt(d.total)}"
                else -> d.message ?: "در حال دانلود... ${fmt(d.bytes)}"
            }
            return DownloadNotifications.progress(this, d.title, text, pct, singleId = id)
        }
        val list = active.values
        val total = list.sumOf { it.total }
        val got = list.sumOf { it.bytes }
        val pct = if (total > 0 && list.all { it.total > 0 }) ((got * 100) / total).toInt() else -1
        return DownloadNotifications.progress(this, "${list.size} دانلود در حال انجام", "${fmt(got)} دانلود شده", pct)
    }

    private fun fmt(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb) else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    }

    override fun onDestroy() {
        releaseWake()
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
