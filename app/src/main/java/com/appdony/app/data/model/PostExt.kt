package com.appdony.app.data.model

import android.text.Html
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

fun Post.featuredImageUrl(): String? {
    val media = embedded?.featuredMedia?.firstOrNull()

    media?.mediaDetails?.sizes?.let { sizes ->
        sizes["medium"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
        sizes["medium_large"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
        sizes["thumbnail"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
    }

    media?.sourceUrl?.let { if (it.isNotBlank()) return it }

    val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
    regex.find(content.rendered)?.groupValues?.getOrNull(1)?.let {
        if (it.isNotBlank()) return it
    }

    return null
}

fun Post.cleanTitle(): String {
    return Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\u00A0", " ")
        .trim()
}

fun Post.cleanExcerpt(): String {
    // 👇 اگه سرور خلاصه متن رو نفرستاده بود، خودش از متن اصلی برمی‌داره
    val rawExcerpt = excerpt?.rendered ?: content.rendered.take(250)
    
    return Html.fromHtml(rawExcerpt, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\n", " ")
        .replace("\u00A0", " ")
        .trim()
}

fun Post.cleanContent(): String {
    return Html.fromHtml(content.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\u00A0", " ")
        .trim()
}

fun Post.primaryCategoryName(): String {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.name ?: "دسته‌بندی"
}

fun Post.primaryCategoryId(): Int? {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.id
}

// ───────────── اطلاعات کارت‌ها: «جدید»، اندروید، نسخه ─────────────

/** تا این‌قدر بعد از انتشار یا آخرین ویرایش، پست «جدید» حساب می‌شه (۴ روز) */
private const val NEW_WINDOW_MS = 4L * 24 * 60 * 60 * 1000

private fun parseWpDate(raw: String?): Long? {
    if (raw.isNullOrBlank() || raw.length < 19) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw.take(19))?.time
    } catch (e: Exception) {
        null
    }
}

/** اگه تا ۴ روز از انتشار یا آخرین ویرایشش گذشته باشه یعنی جدیده */
fun Post.isNew(now: Long = System.currentTimeMillis()): Boolean {
    val latest = listOfNotNull(parseWpDate(date), parseWpDate(modified)).maxOrNull() ?: return false
    return now - latest <= NEW_WINDOW_MS
}

/** دسته‌ی اصلی پست برای کارت (بدون متن پیش‌فرض اگه پیدا نشد) */
fun Post.cardCategory(): String? {
    val name = embedded?.terms?.firstOrNull()?.firstOrNull()?.name ?: return null
    return Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim().ifBlank { null }
}

data class PostMeta(val androidReq: String? = null, val version: String? = null)

private val metaCache = ConcurrentHashMap<Int, PostMeta>()
private val TAG_REGEX = Regex("<[^>]+>")
private val ANDROID_REGEX = Regex(
    "(?:اندروید|Android)[^0-9]{0,25}([0-9]+(?:\\.[0-9]+)*\\+?)",
    RegexOption.IGNORE_CASE
)
private val VERSION_REGEX = Regex("(?:نسخه|[Vv]ersion)\\s*[:：]?\\s*([0-9][0-9A-Za-z.\\-]*)")

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

fun Post.cachedMeta(): PostMeta? = metaCache[id]

/** اندروید مورد نیاز و نسخه از متن پست (یک بار حساب می‌شه و کش می‌شه) */
fun Post.meta(): PostMeta {
    metaCache[id]?.let { return it }
    val result = try {
        val text = normalizeDigits(content.rendered.replace(TAG_REGEX, " ").replace("&nbsp;", " "))
        PostMeta(
            androidReq = ANDROID_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim()?.ifBlank { null },
            version = VERSION_REGEX.find(text)?.groupValues?.getOrNull(1)?.trim()?.trimEnd('.', '-')?.ifBlank { null }
        )
    } catch (e: Exception) {
        PostMeta()
    }
    metaCache[id] = result
    return result
}
