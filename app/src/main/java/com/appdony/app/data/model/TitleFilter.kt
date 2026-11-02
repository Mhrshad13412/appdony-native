package com.appdony.app.data.model

import java.util.concurrent.ConcurrentHashMap

/**
 * فیلتر سنگین اسم کارت‌ها: فقط خودِ اسم بازی/برنامه می‌مونه.
 * ۱) اگه اسم داخل کوتیشن بود (“...” یا «...» یا "...") فقط همون برمی‌داره
 * ۲) اگه حرف انگلیسی داشت فقط بخش انگلیسیِ اسم می‌مونه (بدون MOD / APK / نسخه / for Android)
 * ۳) اگه انگلیسی نداشت فیلتر فارسی: دانلود، بازی، برنامه، برای اندروید، مود، هک، نسخه و ... پاک می‌شن
 */

private val QUOTE_CHARS = charArrayOf('“', '”', '"', '«', '»', '„', '‟', '❝', '❞', '＂')
private val ALL_QUOTE_LIKE = "“”\"«»„‟❝❞＂‘’‹›()[]{}<>【】「」『』"

// هر چیزی که از اینجا به بعدش آشغاله (فارسی)
private val FA_CUT_TOKENS = setOf(
    "برای", "مخصوص", "ویژه", "اندروید", "اندرویدی", "آندروید", "اندروید.",
    "نسخه", "ورژن", "مود", "مودشده", "هک", "هکی", "هکشده", "آپدیت", "اپدیت",
    "بدون", "همراه", "رایگان", "پولی", "فول", "دیتا", "اوبی", "obb", "apk",
    "+", "-", "–", "—", "|", "/", "\\", "~", "•", "·", "،", "؛"
)

// کلمه‌های اضافه که هرجای اسم باشن پاک می‌شن
private val FA_NOISE_TOKENS = setOf(
    "دانلود", "دانلودبازی", "بازی", "بازیه", "برنامه", "اپلیکیشن", "اپلیکیشنی", "اپ", "نرم", "افزار",
    "نرم‌افزار", "نرمافزار", "اندروید", "اندرویدی", "آندروید", "مود", "مودشده", "هک", "هکشده",
    "آپدیت", "اپدیت", "نسخه", "ورژن", "اپدونی", "رایگان", "کامل", "جدید", "جدیدترین", "آخرین",
    "دانلودی", "لینک", "مستقیم", "اصلی", "پرمیوم", "پریمیوم"
)

// کلمه‌های ربطی که لبه‌ی اسم نباید بمونن
private val FA_EDGE_STOP = setOf("و", "به", "با", "در", "از", "را", "برای", "که", "این", "آن")

// انگلیسی: از این توکن‌ها به بعد آشغاله
private val EN_CUT_TOKENS = setOf("mod", "apk", "obb", "android", "ios", "hack", "hacked", "cracked", "patched", "data", "download")
private val EN_PLATFORMS = setOf("android", "ios", "pc", "windows", "mac", "iphone", "ipad")
private val EN_VERSION = Regex("""^(v\d+(\.\d+)*[a-z]?|\d+\.\d+\.\d+(\.\d+)*[a-z]?)$""", RegexOption.IGNORE_CASE)
private val EN_CONNECTORS = setOf("&", ":", "+", "'", "’", "!", ".", "·")
private val EN_BREAKERS = setOf("-", "–", "—", "|", "/", "\\", "~", "•", ",", "،")

private val cache = ConcurrentHashMap<String, String>()

private fun isArabicScript(c: Char): Boolean =
    c in '\u0600'..'\u06FF' || c in '\u0750'..'\u077F' || c in '\u08A0'..'\u08FF' ||
        c in '\uFB50'..'\uFDFF' || c in '\uFE70'..'\uFEFF'

private fun isLatinLetter(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z'

private fun normalizeAll(s: String): String = buildString(s.length) {
    for (c in s) {
        when {
            c in '\u06F0'..'\u06F9' -> append('0' + (c - '\u06F0'))
            c in '\u0660'..'\u0669' -> append('0' + (c - '\u0660'))
            c == '\u0640' -> {} // کشیده
            c == '\u200C' -> append(' ') // نیم‌فاصله → فاصله (برای تشخیص کلمه)
            c in '\u200B'..'\u200F' || c in '\u202A'..'\u202E' || c in '\u2066'..'\u2069' || c == '\uFEFF' -> {}
            c == '\u00A0' -> append(' ')
            c == '…' -> append(' ')
            c == 'ي' -> append('ی')
            c == 'ك' -> append('ک')
            else -> append(c)
        }
    }
}.replace("...", " ").replace(Regex("\\s+"), " ").trim()

private fun stripPunct(s: String): String = buildString(s.length) {
    for (c in s) append(if (ALL_QUOTE_LIKE.indexOf(c) >= 0) ' ' else c)
}.replace(Regex("\\s+"), " ").trim()

/** اگه بین کوتیشن‌ها چیزی بود همونو برمی‌گردونه */
private fun quotedPart(s: String): String? {
    val first = s.indexOfFirst { it in QUOTE_CHARS }
    val last = s.indexOfLast { it in QUOTE_CHARS }
    if (first < 0 || last <= first) return null
    val inner = s.substring(first + 1, last).trim()
    return inner.ifBlank { null }
}

private fun hasLatin(s: String) = s.any { isLatinLetter(it) }

/** بهترین دنباله‌ی انگلیسی داخل متن؛ null اگه چیزی نبود */
private fun englishPart(raw: String): String? {
    val tokens = stripPunct(raw).split(' ').filter { it.isNotBlank() }
    val runs = ArrayList<List<String>>()
    var cur = ArrayList<String>()
    fun flush() {
        if (cur.isNotEmpty()) runs.add(cur)
        cur = ArrayList()
    }
    var i = 0
    while (i < tokens.size) {
        val t = tokens[i]
        val low = t.lowercase()
        val hasArabic = t.any { isArabicScript(it) }
        val hasAlnum = t.any { isLatinLetter(it) || it.isDigit() }
        when {
            hasArabic -> flush()
            low in EN_BREAKERS -> flush()
            low == "for" && i + 1 < tokens.size && tokens[i + 1].lowercase() in EN_PLATFORMS -> { flush(); i++ }
            low in EN_CUT_TOKENS -> flush()
            EN_VERSION.matches(t) -> flush()
            hasAlnum || low in EN_CONNECTORS -> cur.add(t)
            else -> flush()
        }
        i++
    }
    flush()

    var best: String? = null
    var bestLetters = 0
    for (run in runs) {
        val trimmed = run.dropWhile { it.lowercase() in EN_CONNECTORS }.dropLastWhile { it.lowercase() in EN_CONNECTORS && it != "!" && it != "." }
        if (trimmed.isEmpty()) continue
        val text = trimmed.joinToString(" ").trim(':', '&', '+', ',', '-', ' ')
        val letters = text.count { isLatinLetter(it) }
        if (letters >= 2 && letters > bestLetters) {
            best = text
            bestLetters = letters
        }
    }
    return best
}

/** فیلتر فارسی سنگین وقتی انگلیسی نبود */
private fun persianPart(raw: String): String {
    val tokens = stripPunct(raw).split(' ').filter { it.isNotBlank() }.toMutableList()

    // از اولین کلمه‌ی «بریدنی» به بعد دور ریخته می‌شه (اگه اسم رو کلاً خالی نکنه)
    var cutAt = -1
    for ((idx, t) in tokens.withIndex()) {
        val core = t.trim('.', ',', ':', '،', '؛', '!', '؟', '?')
        if (core in FA_CUT_TOKENS || t in FA_CUT_TOKENS) { cutAt = idx; break }
    }
    var kept: List<String> = if (cutAt >= 0) tokens.subList(0, cutAt) else tokens
    kept = kept.filter { it.trim('.', ',', ':', '،', '؛', '!', '؟', '?') !in FA_NOISE_TOKENS }
    if (kept.isEmpty()) {
        // همه‌ی اسم قبل از نقطه‌ی برش خالی بود: فقط کلمه‌های اضافه رو پاک کن و برش نزن
        kept = tokens.filter { it.trim('.', ',', ':', '،', '؛', '!', '؟', '?') !in FA_NOISE_TOKENS && it !in FA_CUT_TOKENS }
    }
    kept = kept.dropWhile { it in FA_EDGE_STOP }.dropLastWhile { it in FA_EDGE_STOP }
    return kept.joinToString(" ").trim(' ', ':', '-', '–', '—', '|', '/', '،', '؛', '.', ',')
}

/**
 * اسم تمیز برای کارت‌ها.
 * مثال: «دانلود بازی “Shadow Fight 3” برای اندروید» → «Shadow Fight 3»
 * مثال: «دانلود بازی فروشگاه شهری برای اندروید» → «فروشگاه شهری»
 */
fun String.gameCardName(): String {
    if (isBlank()) return this
    cache[this]?.let { return it }
    val result = try {
        val norm = normalizeAll(this)
        val quoted = quotedPart(norm)
        val english = (quoted?.let { englishPart(it) })
            ?: englishPart(norm)
        val out = when {
            english != null -> english
            quoted != null && persianPart(quoted).isNotBlank() -> persianPart(quoted)
            else -> persianPart(norm)
        }
        out.trim().ifBlank { stripPunct(norm).ifBlank { this.trim() } }
    } catch (e: Exception) {
        this
    }
    if (cache.size > 2000) cache.clear()
    cache[this] = result
    return result
}

fun Post.cardName(): String = cleanTitle().gameCardName()
