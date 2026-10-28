package com.appdony.app.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

data class DownloadLink(
    val title: String,
    val url: String,
    val size: String? = null
)

data class AppInfo(
    val version: String? = null,
    val size: String? = null,
    val androidReq: String? = null,
    val developer: String? = null,
    val downloadLinks: List<DownloadLink> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val cleanDescription: String = "",
    val allLinks: List<String> = emptyList() // فقط برای گزارش فنی
)

sealed class ContentBlock {
    data class Header(val text: String, val level: Int) : ContentBlock()
    data class Paragraph(val text: String) : ContentBlock()
    data class Image(val url: String) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
    data class ListBlock(val items: List<String>, val isOrdered: Boolean) : ContentBlock()
}

object HtmlParser {

    private const val BASE = "https://appdony.ir/"

    private val BLOCKED = listOf(
        "google.com", "doubleclick", "googlesyndication", "google-analytics", "facebook.com",
        "t.me/", "telegram.me/", "telegram.org", "twitter.com", "x.com/", "pinterest.com",
        "youtube.com", "youtu.be", "aparat.com", "instagram.com", "whatsapp.com", "wa.me",
        "linkedin.com", "tiktok.com", "play.google.com", "apps.apple.com",
        "cafebazaar.ir", "myket.ir", "gamerant", "ign.com"
    )
    private val FILE_EXTS = setOf("apk", "xapk", "apks", "apkm", "zip", "rar", "7z", "obb")
    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif")
    private val DOWNLOAD_WORDS = listOf(
        "دانلود", "download", "لینک مستقیم", "دریافت", "نسخه مود", "mod apk", "مگابایت", "گیگابایت"
    )
    private val JUNK_IMAGE = listOf("emoji", "avatar", "gravatar", "logo", "icon", "wp-includes", "/themes/", "sprite")
    private val DL_PATH = Regex("(^|/)(dl|download|downloads|get|getfile|file|files)(/|$)")
    private val SIZE_REGEX = Regex(
        "([0-9]+(?:\\.[0-9]+)?)\\s*(MB|GB|KB|مگابایت|گیگابایت|کیلوبایت)",
        RegexOption.IGNORE_CASE
    )

    // ───────────── ابزارهای کمکی ─────────────

    private fun normalizeDigits(s: String): String = buildString(s.length) {
        for (c in s) {
            append(
                when (c) {
                    in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                    else -> c
                }
            )
        }
    }

    private fun absolute(raw: String): String? {
        val u = raw.trim()
        if (u.isBlank() || u.startsWith("#") || u.startsWith("javascript:") ||
            u.startsWith("mailto:") || u.startsWith("tel:") || u.startsWith("data:")
        ) return null
        return try {
            when {
                u.startsWith("//") -> "https:$u"
                u.startsWith("http") -> u
                else -> BASE.toHttpUrlOrNull()?.resolve(u)?.toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun bestFromSrcset(srcset: String): String {
        if (srcset.isBlank()) return ""
        return srcset.split(",")
            .map { it.trim().split(Regex("\\s+")) }
            .lastOrNull { it.isNotEmpty() }
            ?.firstOrNull()
            .orEmpty()
    }

    // عکس‌های lazy-load هم پشتیبانی می‌شن
    private fun imageUrl(img: Element): String? {
        val srcset = img.attr("data-srcset").ifBlank { img.attr("srcset") }
        val candidates = listOf(
            img.attr("data-src"), img.attr("data-lazy-src"), img.attr("data-original"),
            bestFromSrcset(srcset), img.attr("src")
        )
        for (raw in candidates) {
            val abs = absolute(raw)
            if (abs != null) return abs
        }
        return null
    }

    private fun isJunkImage(url: String): Boolean {
        val lower = url.lowercase()
        return JUNK_IMAGE.any { lower.contains(it) }
    }

    // ───────────── محتوای متنی (توضیحات / وبلاگ) ─────────────

    fun parseContent(html: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        if (html.isBlank()) return blocks
        val doc = Jsoup.parse(html, BASE)
        val seen = mutableSetOf<String>()
        for (element in doc.body().children()) {
            parseElement(element, blocks, seen)
        }
        return blocks
    }

    private fun addImage(img: Element, blocks: MutableList<ContentBlock>, seen: MutableSet<String>) {
        val url = imageUrl(img) ?: return
        if (isJunkImage(url)) return
        if (seen.add(url)) blocks.add(ContentBlock.Image(url))
    }

    private fun parseElement(element: Element, blocks: MutableList<ContentBlock>, seen: MutableSet<String>) {
        when (element.tagName().lowercase()) {
            "script", "style", "noscript", "iframe", "form", "button", "nav", "header", "footer", "aside", "svg" -> return

            "h1", "h2", "h3", "h4", "h5", "h6" -> {
                val text = element.text().trim()
                if (text.isNotEmpty()) {
                    blocks.add(ContentBlock.Header(text, element.tagName().substring(1).toIntOrNull() ?: 2))
                }
            }

            "p" -> {
                val text = element.text().trim()
                val linkText = element.select("a").text().trim()
                val onlyLinks = linkText.isNotEmpty() && text == linkText
                if (text.isNotEmpty() && !onlyLinks) blocks.add(ContentBlock.Paragraph(text))
                element.select("img").forEach { addImage(it, blocks, seen) }
            }

            "img" -> addImage(element, blocks, seen)

            "a" -> element.select("img").forEach { addImage(it, blocks, seen) }

            "blockquote" -> {
                val text = element.text().trim()
                if (text.isNotEmpty()) blocks.add(ContentBlock.Quote(text))
            }

            "ul", "ol" -> {
                val items = element.select("li").map { it.text().trim() }.filter { it.isNotEmpty() }
                if (items.isNotEmpty()) blocks.add(ContentBlock.ListBlock(items, element.tagName() == "ol"))
            }

            "table" -> {
                val rows = element.select("tr")
                    .map { tr -> tr.select("th,td").joinToString(" : ") { it.text().trim() } }
                    .filter { it.isNotBlank() }
                if (rows.isNotEmpty()) blocks.add(ContentBlock.ListBlock(rows, false))
            }

            "figure" -> {
                element.select("img").forEach { addImage(it, blocks, seen) }
                val caption = element.select("figcaption").text().trim()
                if (caption.isNotEmpty()) blocks.add(ContentBlock.Paragraph(caption))
            }

            else -> {
                val own = element.ownText().trim()
                if (own.length > 1) blocks.add(ContentBlock.Paragraph(own))
                element.children().forEach { parseElement(it, blocks, seen) }
            }
        }
    }

    // ───────────── اطلاعات بازی/برنامه ─────────────

    fun parsePostContent(html: String): AppInfo {
        val doc = Jsoup.parse(html, BASE)
        val text = normalizeDigits(doc.text())
        val lines = normalizeDigits(doc.wholeText())

        return AppInfo(
            version = extractVersion(text),
            size = extractSize(text),
            androidReq = extractAndroidReq(text),
            developer = extractDeveloper(lines),
            downloadLinks = extractDownloadLinks(doc),
            screenshots = extractScreenshots(doc),
            cleanDescription = doc.text().replace("\u00A0", " ").trim(),
            allLinks = doc.select("a[href]").take(40).map { "${it.text().trim().take(40)} -> ${it.attr("href")}" }
        )
    }

    private fun extractVersion(text: String): String? {
        val patterns = listOf(
            Regex("نسخه\\s*[:：]?\\s*([0-9][0-9A-Za-z.\\-]*)"),
            Regex("[Vv]ersion\\s*[:：]?\\s*([0-9][0-9A-Za-z.\\-]*)")
        )
        for (p in patterns) {
            val m = p.find(text)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun extractSize(text: String): String? {
        val labeled = Regex(
            "(?:حجم|Size)\\s*[:：]?\\s*([0-9]+(?:\\.[0-9]+)?)\\s*(MB|GB|KB|مگابایت|گیگابایت|کیلوبایت)",
            RegexOption.IGNORE_CASE
        ).find(text)
        if (labeled != null) return "${labeled.groupValues[1]} ${labeled.groupValues[2]}"
        val any = SIZE_REGEX.find(text) ?: return null
        return "${any.groupValues[1]} ${any.groupValues[2]}"
    }

    private fun extractAndroidReq(text: String): String? {
        val m = Regex("(?:اندروید|Android)\\s*([0-9]+(?:\\.[0-9]+)?)", RegexOption.IGNORE_CASE).find(text)
            ?: return null
        return "اندروید ${m.groupValues[1]} به بالا"
    }

    private fun extractDeveloper(text: String): String? {
        val m = Regex("(?:سازنده|توسعه\\s*دهنده|Developer)\\s*[:：]\\s*([^\\n|،,]{2,40})").find(text)
            ?: return null
        return m.groupValues[1].trim()
    }

    // فقط لینک‌هایی که واقعاً دانلود هستن؛ لینک دسته‌بندی و پست‌های داخلی سایت حذف می‌شن
    private fun extractDownloadLinks(doc: Document): List<DownloadLink> {
        val result = LinkedHashMap<String, DownloadLink>()
        val candidates = doc.select("a[href], [data-href], [data-url], [data-link], [data-download]")

        for (el in candidates) {
            val raw = listOf(
                el.attr("href"), el.attr("data-href"), el.attr("data-url"),
                el.attr("data-link"), el.attr("data-download")
            ).firstOrNull { it.isNotBlank() } ?: continue

            val url = absolute(raw) ?: continue
            val http = url.toHttpUrlOrNull() ?: continue
            val host = http.host.lowercase()
            val path = http.encodedPath.lowercase()

            if (BLOCKED.any { url.contains(it, ignoreCase = true) }) continue

            val ext = path.substringAfterLast('.', "")
            if (ext in IMAGE_EXTS) continue

            val text = el.text().trim()
            val hasFileExt = ext in FILE_EXTS
            val isInternal = host == "appdony.ir" || host == "www.appdony.ir"
            val pathLooksDl = DL_PATH.containsMatchIn(path)
            val hostLooksDl = host.startsWith("dl") || host.contains("download") ||
                host.contains("files") || host.contains("cdn")
            val textLooksDl = DOWNLOAD_WORDS.any { text.contains(it, ignoreCase = true) }

            val internalContent = isInternal && !hasFileExt && !pathLooksDl
            val isDownload = !internalContent && (hasFileExt || hostLooksDl || pathLooksDl || textLooksDl)
            if (!isDownload) continue

            val fileName = path.substringAfterLast('/').takeIf { it.contains('.') }
            val title = text.ifBlank { fileName ?: "لینک دانلود" }
                .replace(Regex("\\s+"), " ")
                .take(60)
            val size = SIZE_REGEX.find(normalizeDigits(text))?.value

            if (!result.containsKey(url)) result[url] = DownloadLink(title, url, size)
        }
        return result.values.take(10)
    }

    private fun extractScreenshots(doc: Document): List<String> {
        val result = LinkedHashSet<String>()
        for (img in doc.select("img")) {
            // اگه عکس داخل لینکِ عکس بزرگ بود، همون نسخه‌ی بزرگ رو برمی‌داریم
            val parentHref = img.parent()?.takeIf { it.tagName() == "a" }?.attr("href").orEmpty()
            val parentExt = parentHref.substringBefore('?').substringAfterLast('.', "").lowercase()
            val url = if (parentExt in IMAGE_EXTS) absolute(parentHref) else imageUrl(img)
            if (url == null || isJunkImage(url)) continue

            val w = img.attr("width").toIntOrNull()
            val h = img.attr("height").toIntOrNull()
            if ((w != null && w in 1..99) || (h != null && h in 1..99)) continue

            val ext = url.substringBefore('?').substringAfterLast('.', "").lowercase()
            if (ext !in IMAGE_EXTS && !url.lowercase().contains("/wp-content/uploads/")) continue
            result.add(url)
        }
        return result.take(10)
    }
}
