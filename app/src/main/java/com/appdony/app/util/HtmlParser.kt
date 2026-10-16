package com.appdony.app.util

import android.text.Html

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

object HtmlParser {

    // دامنه‌های تبلیغاتی که نباید نمایش داده بشن
    private val blockedDomains = listOf(
        "google.com/ads", "doubleclick", "googlesyndication",
        "facebook.com/tr", "google-analytics",
        "t.me/", "telegram.me/", "instagram.com/appdony",
        "twitter.com/intent", "pinterest.com/pin"
    )

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
                return m.groupValues[0].trim()
                    .replace("حجم", "")
                    .replace(":", "")
                    .replace("：", "")
                    .trim()
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

    /**
     * استخراج همه لینک‌های دانلود از HTML
     * هر لینکی که تو تگ <a> باشه و متنش شامل «دانلود» یا تو دکمه‌های دانلود باشه
     */
    private fun extractDownloadLinks(html: String): List<DownloadLink> {
        val result = mutableListOf<DownloadLink>()

        // ۱. لینک‌های با متن «دانلود»
        val anchorRegex = Regex(
            """<a[^>]+href=["']([^"']+)["'][^>]*>(.*?)</a>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        anchorRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1].trim()
            val rawText = Html.fromHtml(m.groupValues[2], Html.FROM_HTML_MODE_LEGACY).toString().trim()

            // فیلتر: فقط لینک‌های http معتبر، بدون دامنه‌های تبلیغاتی
            if (url.startsWith("http") &&
                blockedDomains.none { url.contains(it, ignoreCase = true) }
            ) {
                val title = rawText.ifBlank { "دانلود" }
                    .replace(Regex("\\s+"), " ")
                    .take(60)

                // اگه لینک تکراری نباشه
                if (result.none { it.url == url }) {
                    result.add(DownloadLink(title = title, url = url))
                }
            }
        }

        // ۲. لینک‌های با کلاس download یا button
        val btnRegex = Regex(
            """<a[^>]*class=["'][^"']*(?:download|btn)[^"']*["'][^>]*href=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        )
        btnRegex.findAll(html).forEach { m ->
            val url = m.groupValues[1].trim()
            if (url.startsWith("http") &&
                blockedDomains.none { url.contains(it, ignoreCase = true) } &&
                result.none { it.url == url }
            ) {
                result.add(DownloadLink(title = "دانلود مستقیم", url = url))
            }
        }

        return result.take(8)
    }

    private fun extractScreenshots(html: String): List<String> {
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
        val result = mutableListOf<String>()
        regex.findAll(html).forEach { m ->
            val url = m.groupValues[1]
            if (url.startsWith("http") &&
                (url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                 url.endsWith(".png") || url.endsWith(".webp"))) {
                if (!result.contains(url)) result.add(url)
            }
        }
        return result.take(8)
    }
}
