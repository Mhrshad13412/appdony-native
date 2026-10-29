package com.appdony.app.util

import com.appdony.app.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

data class InstallResult(val links: List<DownloadLink>, val debug: String)

/**
 * مثل گوگل‌پلی: فقط دکمه‌ی «نصب کردن» صفحه‌ی اصلی سایت رو پیدا می‌کنه
 * و لینکی که پشتشه رو برمی‌گردونه. هیچ لینک دیگه‌ای از صفحه برداشته نمی‌شه.
 */
object InstallLinkFinder {

    private const val BASE = "https://appdony.ir/"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val INSTALL_TEXT = Regex("^(نصب کردن|install)$", RegexOption.IGNORE_CASE)
    private val URL_ATTRS = listOf(
        "href", "data-href", "data-url", "data-link", "data-download", "data-file", "formaction"
    )
    private val TARGET_ATTRS = listOf("data-target", "data-bs-target", "data-modal", "aria-controls")
    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "svg")
    private val BLOCKED_HOSTS = listOf(
        "t.me", "telegram.me", "telegram.org", "instagram.com", "twitter.com", "x.com", "facebook.com",
        "wa.me", "whatsapp.com", "youtube.com", "youtu.be", "aparat.com", "play.google.com",
        "cafebazaar.ir", "myket.ir", "linkedin.com", "pinterest.com"
    )
    private val SIZE_REGEX = Regex(
        "([0-9]+(?:\\.[0-9]+)?)\\s*(MB|GB|KB|مگابایت|گیگابایت|کیلوبایت)",
        RegexOption.IGNORE_CASE
    )
    private val SCRIPT_KEY = Regex(
        "\"(?:download_?url|downloadUrl|dl_?url|file_?url|apk_?url|install_?url)\"\\s*[:=]\\s*\"([^\"]+)\"",
        RegexOption.IGNORE_CASE
    )
    private val SCRIPT_FILE = Regex(
        "https?:\\\\?/\\\\?/[^\"'\\s]+?\\.(?:apk|xapk|apks|apkm|zip|rar|7z|obb)",
        RegexOption.IGNORE_CASE
    )

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply { ApiClient.cookieJarInstance?.let { cookieJar(it) } }
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    private fun norm(s: String): String =
        s.replace('\u200c', ' ').replace('\u00A0', ' ').replace(Regex("\\s+"), " ").trim()

    suspend fun find(postId: Int, permalink: String?): Result<InstallResult> = withContext(Dispatchers.IO) {
        try {
            val pageUrl = permalink?.takeIf { it.startsWith("http") } ?: "${BASE}?p=$postId"
            val request = Request.Builder()
                .url(pageUrl)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "fa,en;q=0.8")
                .get()
                .build()

            val (finalUrl, html) = client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("HTTP ${resp.code}"))
                Pair(resp.request.url.toString(), resp.body?.string().orEmpty().take(1_500_000))
            }
            if (html.isBlank()) return@withContext Result.failure(Exception("صفحه خالی بود"))

            val doc = Jsoup.parse(html, finalUrl)
            Result.success(extract(doc, finalUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extract(doc: Document, pageUrl: String): InstallResult {
        val found = LinkedHashMap<String, String>() // url -> title

        // ساختار واقعی قالب سایت: دکمه‌ی نصب بالا + نصب چسبیده پایین + لیست لینک‌های دانلود
        val installBtns = doc.select("a#install-btn, a#install-btn-sticky, .detail-actions a.btn-install")
        for (a in installBtns) {
            addUrl(resolveUrl(a.attr("href"), pageUrl), "نصب کردن", pageUrl, found)
        }

        val listLinks = LinkedHashMap<String, String>()
        for (a in doc.select("#downloads .download-list a[href]")) {
            val title = norm(a.text()).ifBlank { "دانلود" }.take(60)
            addUrl(resolveUrl(a.attr("href"), pageUrl), title, pageUrl, listLinks)
        }
        for ((url, title) in listLinks) {
            when {
                !found.containsKey(url) -> found[url] = title
                listLinks.size > 1 -> found[url] = title // چند فایل داریم؛ اسم هرکدوم مهمه
            }
        }

        // قالب‌های دیگه: هر دکمه‌ای که متنش دقیقاً «نصب کردن» باشه
        val candidates = if (found.isEmpty()) {
            doc.select("a, button, input[type=button], input[type=submit], div, span, li, p").filter { el ->
                val t = if (el.tagName() == "input") el.attr("value") else el.text()
                INSTALL_TEXT.matches(norm(t))
            }
        } else emptyList()
        for (c in candidates) collectFromButton(c, doc, pageUrl, found)

        // اگه دکمه لینک مستقیم نداشت: لینک فایل داخل اسکریپت‌های صفحه
        if (found.isEmpty()) {
            for (script in doc.select("script")) {
                val data = script.data()
                if (data.isBlank()) continue
                for (m in SCRIPT_KEY.findAll(data)) addUrl(m.groupValues[1].replace("\\/", "/"), "نصب کردن", pageUrl, found)
                for (m in SCRIPT_FILE.findAll(data)) addUrl(m.value.replace("\\/", "/"), "نصب کردن", pageUrl, found)
            }
        }

        val links = found.map { (url, title) ->
            val size = SIZE_REGEX.find(title)?.let { "${it.groupValues[1]} ${it.groupValues[2]}" }
            DownloadLink(title = title, url = url, size = size)
        }
        return InstallResult(links, buildDebug(doc, pageUrl, installBtns + candidates))
    }

    private fun collectFromButton(c: Element, doc: Document, pageUrl: String, out: MutableMap<String, String>) {
        val chain = listOf(c) + c.parents().take(3)
        for (e in chain) {
            for (attr in URL_ATTRS) {
                val v = e.attr(attr).trim()
                if (v.isBlank()) continue
                if (v.startsWith("#")) collectFromFragment(v, doc, pageUrl, out)
                else addUrl(resolveUrl(v, pageUrl), "نصب کردن", pageUrl, out)
            }
            val onclick = e.attr("onclick")
            if (onclick.isNotBlank()) {
                val u = Regex("https?://[^'\"\\s)]+").find(onclick)?.value
                    ?: Regex("(?:location(?:\\.href)?|window\\.open|location\\.assign|location\\.replace)\\s*[=(]\\s*['\"]([^'\"]+)['\"]")
                        .find(onclick)?.groupValues?.get(1)
                if (u != null) addUrl(resolveUrl(u, pageUrl), "نصب کردن", pageUrl, out)
            }
            for (attr in TARGET_ATTRS) {
                val v = e.attr(attr).trim()
                if (v.isNotBlank()) collectFromFragment(if (v.startsWith("#") || v.startsWith(".")) v else "#$v", doc, pageUrl, out)
            }
            if (e.tagName() == "form") {
                val action = e.attr("action").trim()
                if (action.isNotBlank()) addUrl(resolveUrl(action, pageUrl), "نصب کردن", pageUrl, out)
            }
        }
    }

    private fun collectFromFragment(sel: String, doc: Document, pageUrl: String, out: MutableMap<String, String>) {
        val container = try {
            if (sel.startsWith("#")) doc.getElementById(sel.removePrefix("#")) else doc.selectFirst(sel)
        } catch (e: Exception) {
            null
        } ?: return
        for (el in container.select("a[href], [data-href], [data-url], [data-download]")) {
            val raw = listOf(el.attr("href"), el.attr("data-href"), el.attr("data-url"), el.attr("data-download"))
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") } ?: continue
            val title = norm(el.text()).ifBlank { "دانلود" }.take(60)
            addUrl(resolveUrl(raw, pageUrl), title, pageUrl, out)
        }
    }

    private fun resolveUrl(raw: String, pageUrl: String): String? {
        val u = raw.trim()
        if (u.isBlank() || u.startsWith("#") || u.startsWith("javascript:") ||
            u.startsWith("mailto:") || u.startsWith("tel:") || u.startsWith("data:")
        ) return null
        return try {
            when {
                u.startsWith("//") -> "https:$u"
                else -> pageUrl.toHttpUrlOrNull()?.resolve(u)?.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun addUrl(url: String?, title: String, pageUrl: String, out: MutableMap<String, String>) {
        if (url == null) return
        val http = url.toHttpUrlOrNull() ?: return
        val host = http.host.lowercase()
        if (BLOCKED_HOSTS.any { host == it || host.endsWith(".$it") }) return
        if (extensionOf(url) in IMAGE_EXTS) return
        if (url == pageUrl) return
        if (!out.containsKey(url)) out[url] = title
    }

    private fun extensionOf(url: String): String =
        url.substringBefore("?").substringBefore("#").substringAfterLast('/', "")
            .substringAfterLast('.', "").lowercase()

    private fun buildDebug(doc: Document, pageUrl: String, candidates: List<Element>): String {
        val sb = StringBuilder()
        sb.append("page: ").append(pageUrl).append('\n')
        sb.append("install elements: ").append(candidates.size).append('\n')
        if (candidates.isEmpty()) {
            val texts = doc.select("a, button").map { norm(it.text()) }.filter { it.isNotBlank() }.distinct().take(40)
            sb.append("buttons/links: ").append(texts.joinToString(" | "))
        } else {
            for (c in candidates.take(3)) {
                sb.append("\n--- ELEMENT ---\n").append(c.outerHtml().take(600))
                c.parent()?.let { sb.append("\n--- PARENT ---\n").append(it.outerHtml().take(1000)) }
            }
        }
        return sb.toString()
    }
}
