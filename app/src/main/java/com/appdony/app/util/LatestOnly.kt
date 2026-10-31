package com.appdony.app.util

import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle

/** از هر اپ/بازی فقط جدیدترین پست رو نگه می‌داره و بقیه‌ی نسخه‌های قدیمی رو حذف می‌کنه */
object LatestOnly {

    private val stopWords = setOf(
        "دانلود", "بازی", "برنامه", "اپلیکیشن", "نسخه", "ی", "نسخهی", "مود", "شده", "هک", "کرک",
        "بدون", "تحریم", "برای", "اندروید", "ویندوز", "رایگان", "آخرین", "جدید", "آنلاین",
        "mod", "apk", "premium", "unlocked", "hack", "hacked", "for", "android", "v"
    )

    private val parentheses = Regex("""\([^)]*\)|\[[^\]]*]|（[^）]*）""")
    private val junkChars = Regex("""[^\p{L}\p{N}.]+""")
    private val versionToken = Regex("""^v?\d+([.\-]\d+)+$|^v\d+$|^(19|20)\d{2}$""")

    private fun normalizeDigits(s: String): String = buildString(s.length) {
        for (c in s) {
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                }
            )
        }
    }

    /** اسم تمیز اپ بدون نسخه و کلمه‌های اضافه */
    fun key(post: Post): String {
        val title = normalizeDigits(post.cleanTitle().lowercase())
            .replace('\u200c', ' ')
            .replace(parentheses, " ")
            .replace(junkChars, " ")
        val tokens = title.split(' ')
            .map { it.trim('.') }
            .filter { it.isNotBlank() && it !in stopWords && !versionToken.matches(it) }
        return if (tokens.isEmpty()) "id:${post.id}" else tokens.joinToString(" ")
    }

    private fun stamp(p: Post): String = maxOf(p.date ?: "", p.modified ?: "")

    fun latestPerApp(posts: List<Post>): List<Post> {
        val best = HashMap<String, Post>()
        for (p in posts) {
            val k = key(p)
            val cur = best[k]
            if (cur == null || stamp(p) > stamp(cur)) best[k] = p
        }
        val seen = HashSet<String>()
        val out = ArrayList<Post>()
        for (p in posts) {
            val k = key(p)
            if (seen.add(k)) out.add(best.getValue(k))
        }
        return out
    }
}
