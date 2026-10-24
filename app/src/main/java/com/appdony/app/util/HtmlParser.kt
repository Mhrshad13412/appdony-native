package com.appdony.app.util

import android.text.Html
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class DownloadLink(
    val title: String,
    val url: String
)

data class AppInfo(
    val version: String? = null,
    val size: String? = null,
    val androidReq: String? = null,
    val developer: String? = null,
    val downloadLinks: List<DownloadLink> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val cleanDescription: String = ""
)

sealed class ContentBlock {
    data class Header(val text: String, val level: Int) : ContentBlock()
    data class Paragraph(val text: String) : ContentBlock()
    data class Image(val url: String) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
    data class ListBlock(val items: List<String>, val isOrdered: Boolean) : ContentBlock()
}

object HtmlParser {
    private val blockedDomains = listOf(
        "google.com", "doubleclick", "googlesyndication",
        "facebook.com", "google-analytics", "t.me/", "telegram.me/", "twitter.com",
        "pinterest.com", "gamerant", "ign.com", "youtube.com", "aparat.com", "youtu.be"
    )

    private fun cleanText(text: String): String {
        return text.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    fun parseContent(html: String): List<ContentBlock> {
        val blocks = mutableListOf<ContentBlock>()
        if (html.isBlank()) return blocks

        val doc = Jsoup.parse(html)
        val body = doc.body()

        for (element in body.children()) {
            parseElement(element, blocks)
        }

        return blocks
    }

    private fun parseElement(element: Element, blocks: MutableList<ContentBlock>) {
        when (element.tagName().lowercase()) {
            "h1", "h2", "h3", "h4" -> {
                val text = cleanText(element.text())
                if (text.isNotEmpty()) {
                    blocks.add(ContentBlock.Header(text, element.tagName().substring(1).toIntOrNull() ?: 2))
                }
            }
            "p" -> {
                val text = cleanText(element.text())
                if (text.isNotEmpty()) {
                    blocks.add(ContentBlock.Paragraph(text))
                }
                element.select("img").forEach { img ->
                    val src = img.attr("src")
                    if (src.isNotEmpty()) blocks.add(ContentBlock.Image(src))
                }
            }
            "img" -> {
                val src = element.attr("src")
                if (src.isNotEmpty()) blocks.add(ContentBlock.Image(src))
            }
            "blockquote" -> {
                val text = cleanText(element.text())
                if (text.isNotEmpty()) {
                    blocks.add(ContentBlock.Quote(text))
                }
            }
            "ul", "ol" -> {
                val items = element.select("li").map { cleanText(it.text()) }.filter { it.isNotEmpty() }
                if (items.isNotEmpty()) {
                    blocks.add(ContentBlock.ListBlock(items, element.tagName() == "ol"))
                }
            }
            "figure" -> {
                element.select("img").forEach { img ->
                    val src = img.attr("src")
                    if (src.isNotEmpty()) blocks.add(ContentBlock.Image(src))
                }
                val caption = cleanText(element.select("figcaption").text())
                if (caption.isNotEmpty()) {
                    blocks.add(ContentBlock.Paragraph(caption))
                }
            }
            "div" -> {
                for (child in element.children()) {
                    parseElement(child, blocks)
                }
            }
        }
    }

    fun parsePostContent(html: String): AppInfo {
        val version = extractVersion(html)
        val size = extractSize(html)
        val androidReq = extractAndroidReq(html)
        val developer = extractDeveloper(html)
        val downloads = extractDownloadLinks(html)
        val screenshots = extractScreenshots(html)
        val clean = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace("\u00A0", " ")
            .trim()
        return AppInfo(
            version = version,
            size = size,
            androidReq = androidReq,
            developer = developer,
            downloadLinks = downloads,
            screenshots = screenshots,
            cleanDescription = clean
        )
    }

    private fun extractVersion(html: String): String? {
        val patterns = listOf(
            Regex("""نسخه\s*[:：]?\s*([0-9][0-9\.a-zA-Z\-]*)"""),
            Regex("""[Vv]ersion\s*[:：]?\s*([0-9][0-9\.a-zA-Z\-]*)""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun extractSize(html: String): String? {
        val patterns = listOf(
            Regex("""حجم\s*[:：]?\s*([0-9]+(\.[0-9]+)?)\s*(مگابایت|گیگابایت|کیلوبایت|MB|GB|KB)"""),
            Regex("""([0-9]+(\.[0-9]+)?)\s*(مگابایت|گیگابایت)""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) {
                return m.groupValues[0].trim().replace("حجم", "").replace(":", "").replace("：", "").trim()
            }
        }
        return null
    }

    private fun extractAndroidReq(html: String): String? {
        val patterns = listOf(
            Regex("""اندروید\s*([0-9]+(\.[0-9]+)?)\s*و\s*بالاتر"""),
            Regex("""[Aa]ndroid\s*([0-9]+(\.[0-9]+)?)\s*(\+|و بالاتر)?""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[0].trim()
        }
        return null
    }

    private fun extractDeveloper(html: String): String? {
        val patterns = listOf(
            Regex("""سازنده\s*[:：]\s*([^<\n]+)"""),
            Regex("""[Dd]eveloper\s*[:：]\s*([^<\n]+)""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun extractDownloadLinks(html: String): List<DownloadLink> {
        val result = mutableListOf<DownloadLink>()
        val anchorRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        anchorRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1].trim()
            val rawText = Html.fromHtml(m.groupValues[2], Html.FROM_HTML_MODE_LEGACY).toString().trim()
            val isBlocked = blockedDomains.any { url.contains(it, ignoreCase = true) }
            // 👈 بازتر کردن شرط تشخیص لینک دانلود (هر لینکی که کلمه download یا apk توش باشه)
            val isDirectFile = url.contains(".apk", true) || url.contains(".zip", true) || url.contains(".rar", true) || 
                    url.contains("download", true) || url.contains("dl.", true)
            
            if (url.startsWith("http") && !isBlocked && isDirectFile) {
                val title = rawText.ifBlank { "دانلود" }.replace(Regex("\\s+"), " ").take(60)
                if (result.none { it.url == url }) {
                    result.add(DownloadLink(title = title, url = url))
                }
            }
        }
        return result.take(8)
    }

    private fun extractScreenshots(html: String): List<String> {
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
        val result = mutableListOf<String>()
        regex.findAll(html).forEach { m ->
            val url = m.groupValues[1]
            if (url.startsWith("http") && (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".webp"))) {
                if (!result.contains(url)) result.add(url)
            }
        }
        return result.take(8)
    }
}
