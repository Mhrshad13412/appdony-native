package com.appdony.app.util

import android.text.Html

data class AppInfo(
    val version: String? = null,
    val size: String? = null,
    val androidReq: String? = null,
    val developer: String? = null,
    val downloadLinks: List<String> = emptyList(),
    val screenshots: List<String> = emptyList(),
    val cleanDescription: String = ""
)

object HtmlParser {

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
            Regex("""[Vv]ersion\s*[:：]?\s*([0-9][0-9\.a-zA-Z\-]*)"""),
            Regex("""v([0-9]+\.[0-9]+(\.[0-9]+)?)""")
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
            Regex("""([0-9]+(\.[0-9]+)?)\s*(مگابایت|گیگابایت)"""),
            Regex("""[Ss]ize\s*[:：]?\s*([0-9]+(\.[0-9]+)?\s*(MB|GB|KB))""")
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
            Regex("""[Aa]ndroid\s*([0-9]+(\.[0-9]+)?)\s*(\+|و بالاتر)?"""),
            Regex("""نیازمند\s*اندروید\s*([0-9]+(\.[0-9]+)?)""")
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
            Regex("""[Dd]eveloper\s*[:：]\s*([^<\n]+)"""),
            Regex("""توسعه\s*دهنده\s*[:：]\s*([^<\n]+)""")
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun extractDownloadLinks(html: String): List<String> {
        val regex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>([^<]*دانلود[^<]*)</a>""", RegexOption.IGNORE_CASE)
        val directRegex = Regex("""<a[^>]+href=["']([^"']+)["'][^>]*>""")

        val result = mutableListOf<String>()
        regex.findAll(html).forEach { m ->
            val url = m.groupValues[1]
            if (url.isNotBlank() && !result.contains(url)) result.add(url)
        }

        // اگه هیچی پیدا نشد، همه لینک‌های خارجی رو بگیر
        if (result.isEmpty()) {
            directRegex.findAll(html).forEach { m ->
                val url = m.groupValues[1]
                if (url.startsWith("http") &&
                    !url.contains("appdony.ir") &&
                    !url.contains("wordpress") &&
                    !result.contains(url)) {
                    result.add(url)
                }
            }
        }
        return result.take(5)
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
