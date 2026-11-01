package com.appdony.app.util

import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import java.net.URLDecoder

/**
 * از هر اپ/بازی فقط یک پست رو نگه می‌داره: بالاترین نسخه، و اگه نسخه نداشتن جدیدترین تاریخ (انتشار یا ویرایش).
 * هم‌اپ بودن از روی «اسکلت صامت» اسم تشخیص داده می‌شه، پس فارسی و انگلیسی یکی حساب می‌شن:
 * ماینکرافت = Minecraft = MNKRFT
 */
object LatestOnly {

    private val stopWords = setOf(
        "دانلود", "بازی", "برنامه", "اپلیکیشن", "ی", "مود", "شده", "هک", "کرک", "بدون", "تحریم",
        "برای", "اندروید", "ویندوز", "رایگان", "آخرین", "جدید", "آنلاین", "آپدیت", "اصلی",
        "نامحدود", "پولی", "نصب", "mod", "apk", "premium", "unlocked", "hack", "hacked", "for",
        "android", "download", "free", "latest", "game", "games", "app", "apps", "application",
        "new", "update", "full", "unlimited", "online", "and", "the", "of"
    )

    /** بعد از این کلمه‌ها شماره‌ی نسخه میاد و نباید جزو اسم حساب بشه */
    private val markers = setOf("نسخه", "v", "version")

    private val brackets = Regex("""\([^)]*\)|\[[^\]]*]""")
    private val junk = Regex("""[^\p{L}\p{N}.]+""")
    private val versionToken = Regex("""^v?\d+([.\-]\d+)+$|^v\d+$|^(19|20)\d{2}$""")

    private val digraphs = listOf(
        "sh" to "ش", "ch" to "چ", "kh" to "k", "gh" to "k",
        "ph" to "f", "th" to "t", "ck" to "k", "qu" to "k"
    )

    /** صامت‌های هم‌صدا یک حرف می‌شن؛ مصوت‌ها (a e i o u y w h و ا ی و ه ...) حذف می‌شن */
    private val classes: Map<Char, Char> = HashMap<Char, Char>().also { m ->
        fun put(chars: String, v: Char) {
            for (c in chars) m[c] = v
        }
        put("bpبپ", 'B')
        put("fف", 'F')
        put("kcgqکگقغك", 'K')
        put("szxثسصزذضظژ", 'S')
        put("tdتدط", 'T')
        put("jج", 'J')
        put("lل", 'L')
        put("mم", 'M')
        put("nن", 'N')
        put("rر", 'R')
        put("ش", 'S')
        put("چ", 'C')
    }

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

    private fun skel(token: String): String {
        var t = token
        for ((a, b) in digraphs) t = t.replace(a, b)
        val sb = StringBuilder()
        for (c in t) {
            val m = classes[c] ?: continue
            if (sb.isEmpty() || sb.last() != m) sb.append(m)
        }
        return sb.toString()
    }

    private fun isMixedLatinDigit(x: String): Boolean =
        x.any { it.isDigit() } && x.any { it in 'a'..'z' } && x.none { it in 'آ'..'ی' }

    private fun key(text: String, dropNumbers: Boolean = false): String {
        val t = normalizeDigits(text.lowercase())
            .replace('\u200c', ' ')
            .replace(brackets, " ")
            .replace(junk, " ")
        val sb = StringBuilder()
        var skipNext = false
        for (raw in t.split(' ')) {
            val x = raw.trim('.')
            if (x.isEmpty()) continue
            if (x in markers) {
                skipNext = true
                continue
            }
            if (x in stopWords) continue
            if (versionToken.matches(x)) {
                skipNext = false
                continue
            }
            if (x.all { it.isDigit() }) {
                if (skipNext) {
                    skipNext = false
                    continue
                }
                // شماره‌ی بلند یعنی نسخه؛ عدد کوتاه (مثل 2 در Subway Surfers 2) جزو اسمه
                if (dropNumbers || x.length > 2) continue
                sb.append('#').append(x)
                continue
            }
            skipNext = false
            if (isMixedLatinDigit(x)) continue // مثل 3d و 4k
            sb.append(skel(x))
        }
        return sb.toString()
    }

    private fun same(a: String, b: String): Boolean {
        if (a.isEmpty() || b.isEmpty()) return false
        if (a == b) return true
        val s = if (a.length <= b.length) a else b
        val l = if (a.length <= b.length) b else a
        // اسم کوتاه‌تر ابتدای اسم بلندتر باشه (مثل «توکا بوکا» با «توکا بوکا ورلد»)، ولی نه با شماره‌ی دنباله
        return s.length >= 4 && l.startsWith(s) && !l.substring(s.length).startsWith("#")
    }

    private fun keysOf(p: Post): List<String> {
        val out = ArrayList<String>(2)
        val titleKey = key(p.cleanTitle())
        if (titleKey.isNotEmpty()) out.add(titleKey)

        // اسم داخل آدرس پست؛ بعضی پست‌های فارسی آدرس انگلیسی دارن (مثل /minecraft/)
        val seg = p.link?.trimEnd('/')?.substringAfterLast('/').orEmpty()
        if (seg.isNotEmpty()) {
            val slug = try {
                URLDecoder.decode(seg, "UTF-8")
            } catch (e: Exception) {
                seg
            }.replace('-', ' ')
            val slugKey = key(slug, dropNumbers = true)
            if (slugKey.length >= 4 && slugKey !in out) out.add(slugKey)
        }
        if (out.isEmpty()) out.add("id:${p.id}")
        return out
    }

    private fun stamp(p: Post): String = maxOf(p.date ?: "", p.modified ?: "")

    private val dottedVersion = Regex("""(?<![\d.])v?(\d+(?:\.\d+)+)""")
    private val shortVersion = Regex("""(?<![\w.])v(\d+)(?![\w])""")

    /** شماره‌ی نسخه از روی عنوان (مثل 29.2.4 یا v41)؛ اگه نداشت null */
    private fun version(p: Post): List<Int>? {
        val t = normalizeDigits(p.cleanTitle().lowercase())
        val m = dottedVersion.find(t)?.groupValues?.get(1) ?: shortVersion.find(t)?.groupValues?.get(1)
        ?: return null
        return m.split('.').map { it.toIntOrNull() ?: 0 }
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    /** a از b بهتره؟ اگه هر دو نسخه دارن، نسخه‌ی بالاتر؛ وگرنه (یا نسخه‌ی مساوی) تاریخ جدیدتر */
    private fun better(a: Post, b: Post): Boolean {
        val va = version(a)
        val vb = version(b)
        if (va != null && vb != null) {
            val c = compareVersions(va, vb)
            if (c != 0) return c > 0
        }
        return stamp(a) > stamp(b)
    }

    private class Cluster(val keys: MutableList<String>, val members: MutableList<Post>)

    fun latestPerApp(posts: List<Post>): List<Post> {
        val clusters = ArrayList<Cluster>()
        for (p in posts) {
            val ks = keysOf(p)
            val hit = clusters.firstOrNull { c -> ks.any { a -> c.keys.any { b -> same(a, b) } } }
            if (hit == null) {
                clusters.add(Cluster(ks.toMutableList(), mutableListOf(p)))
            } else {
                hit.keys.addAll(ks)
                hit.members.add(p)
            }
        }
        // از هر گروه بهترین پست (بالاترین نسخه، بعد جدیدترین تاریخ)؛ جای گروه همون جای اولین نتیجه‌ی خودش می‌مونه
        return clusters.map { c -> c.members.reduce { best, p -> if (better(p, best)) p else best } }
    }
}
