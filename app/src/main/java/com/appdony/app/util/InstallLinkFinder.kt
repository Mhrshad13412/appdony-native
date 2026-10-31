package com.appdony.app.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

// لینک «نصب کردن» رو از صفحه‌ی اصلی پست پیدا می‌کنه.
// لینک واقعی: url پر | دکمه‌ی قفل: url خالی با عنوان LOCKED_TITLE
object InstallLinkFinder {

    const val LOCKED_TITLE = "نصب کردن"

    data class FindResult(
        val links: List<DownloadLink>,
        val debug: String
    )

    private const val SITE = "https://appdony.ir/"
    private const val UA = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .followRedirects(true)
            .build()
    }

    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val BLOCKED_DOMAINS = listOf(
        "google.com", "doubleclick.net", "facebook.com", "t.me", "telegram.me", "telegram.org",
        "twitter.com", "x.com", "pinterest.com", "youtube.com", "youtu.be", "aparat.com",
        "instagram.com", "whatsapp.com", "wa.me", "linkedin.com", "tiktok.com", "apple.com",
        "cafebazaar.ir", "myket.ir", "wikipedia.org", "reddit.com"
    )
    private val ALLOWED_HOSTS = setOf("drive.google.com", "docs.google.com")
    private val INSTALL_TEXT = Regex("نصب|دانلود|download|دریافت", RegexOption.IGNORE_CASE)
    private val DL_PATH = Regex("/(dl|download|downloads|getfile|file|files)(/|\\z)")
    private val SIZE_REGEX = Regex(
        "([0-9]+(?:\\.[0-9]+)?)\\s*(MB|GB|KB|مگابایت|گیگابایت|کیلوبایت)",
        RegexOption.IGNORE_CASE
    )
    private val SKIP_ANCESTORS = setOf("header", "footer", "nav", "aside")

    // لینک پیام تلگرام (t.me/کانال/شماره) لینک دانلوده؛ لینک خالی کانال یا share نه
    private val TG_HOSTS = setOf("t.me", "telegram.me", "telegram.dog")
    private val TG_MESSAGE = Regex("^/(c/[0-9]+|[A-Za-z0-9_]{3,})/[0-9]+")
    private val TG_RAW = Regex("https?://(?:t|telegram)\\.me/(?:c/[0-9]+|[A-Za-z0-9_]{3,})/[0-9]+")

    suspend fun find(postId: Int, postLink: String?): Result<FindResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val valid = postLink != null && postLink.toHttpUrlOrNull() != null
                val pageUrl = if (valid) postLink!! else SITE + "?p=" + postId
                parse(fetch(pageUrl), pageUrl)
            }.onFailure { e ->
                if (e is CancellationException) throw e
            }
        }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", UA)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "fa,en;q=0.8")
            .build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("کد " + resp.code)
            return resp.body?.string() ?: ""
        }
    }

    private fun parse(html: String, pageUrl: String): FindResult {
        val doc = Jsoup.parse(html, pageUrl)
        val scope = doc.selectFirst("article, .entry-content, .post-content, main") ?: doc.body()

        val links = mutableListOf<DownloadLink>()
        val seen = mutableSetOf<String>()
        var lockedAdded = false
        var checked = 0
        val debug = StringBuilder()
        debug.append("page: ").append(pageUrl).append('\n')

        for (el in scope.select("a[href], button")) {
            if (el.parents().any { it.tagName().lowercase() in SKIP_ANCESTORS }) continue
            checked++

            val text = el.text().replace("\u00A0", " ").trim()
            val href = el.attr("href").trim()
            val abs = if (href.isBlank()) "" else el.absUrl("href")
            val looksInstall = INSTALL_TEXT.containsMatchIn(text)

            val classes = el.className().lowercase()
            val noHref = href.isBlank() || href == "#" || href.startsWith("javascript:")
            val isLockedEl = looksInstall &&
                (noHref || classes.contains("lock") || classes.contains("disabled") || el.hasAttr("disabled"))

            if (isLockedEl) {
                if (!lockedAdded) {
                    links.add(DownloadLink(title = LOCKED_TITLE, url = ""))
                    lockedAdded = true
                    debug.append("locked: ").append(text.take(40)).append('\n')
                }
                continue
            }

            if (abs.isBlank()) continue
            val httpUrl = abs.toHttpUrlOrNull() ?: continue
            val host = httpUrl.host.lowercase()
            val path = httpUrl.encodedPath
            val isTg = host in TG_HOSTS
            if (isTg) {
                if (!TG_MESSAGE.containsMatchIn(path)) continue
            } else if (isBlockedHost(host)) {
                continue
            }
            if (abs.substringBefore('#') == pageUrl.substringBefore('#')) continue

            val ext = path.substringAfterLast('/', "").substringAfterLast('.', "").lowercase()
            val isFile = ext in FILE_EXTS
            val isDlPath = looksInstall && DL_PATH.containsMatchIn(path)
            val external = !host.endsWith("appdony.ir")
            if (!isTg && !isFile && !isDlPath && !(looksInstall && external)) continue
            if (!seen.add(abs)) continue

            val m = SIZE_REGEX.find(text)
            val size = if (m != null) m.groupValues[1] + " " + m.groupValues[2] else null
            val title = if (text.isBlank()) LOCKED_TITLE else text.take(60)
            links.add(DownloadLink(title = title, url = abs, size = size))
            debug.append("link: ").append(abs).append('\n')
            if (links.count { it.url.isNotBlank() } >= 6) break
        }

        // اگه با تگ a پیدا نشد (مثلاً لینک تو دکمه/اسکریپت بود) خود HTML رو دنبال لینک تلگرام می‌گردیم
        if (links.none { it.url.isNotBlank() }) {
            var raws = TG_RAW.findAll(scope.outerHtml().replace("\\/", "/")).map { it.value }.distinct().toList()
            if (raws.isEmpty()) {
                raws = TG_RAW.findAll(html.replace("\\/", "/")).map { it.value }.distinct().toList()
            }
            for (u in raws.take(3)) {
                links.add(DownloadLink(title = "دانلود از تلگرام", url = u))
                debug.append("raw: ").append(u).append('\n')
            }
        }

        debug.append("checked: ").append(checked).append(" | found: ").append(links.size)
        return FindResult(links = links, debug = debug.toString())
    }

    private fun isBlockedHost(host: String): Boolean {
        val h = host.lowercase()
        if (h in ALLOWED_HOSTS) return false
        return BLOCKED_DOMAINS.any { h == it || h.endsWith("." + it) }
    }
}
