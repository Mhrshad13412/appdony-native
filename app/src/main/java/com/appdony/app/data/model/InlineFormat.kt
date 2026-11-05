package com.appdony.app.data.model

import java.util.TreeSet

/**
 * قالب‌بندی داخل متن (بولد، کج، خط‌خورده، لینک): محاسبه‌ی بازه‌ها، خوندن از HTML گوتنبرگ و ساختن HTML.
 * متن بلوک همیشه ساده می‌مونه و قالب‌بندی‌ها جدا (توی [InlineSpan]) نگه داشته می‌شن.
 */
object InlineFormat {

    class Parsed(val text: String, val spans: List<InlineSpan>)

    private val TOKEN = Regex(
        """<(/?)(strong|em|s)>|<a href="([^"]*)">|</a>|<br\s*/?>""",
        RegexOption.IGNORE_CASE
    )

    /** ترتیب تو در تو بودن تگ‌ها موقع ساخت HTML: لینک بیرونی‌تره */
    private fun rank(kind: SpanKind): Int = when (kind) {
        SpanKind.LINK -> 0
        SpanKind.BOLD -> 1
        SpanKind.ITALIC -> 2
        SpanKind.STRIKE -> 3
    }

    // ───────────── محاسبه‌ی بازه‌ها ─────────────

    /** بازه‌ها رو توی [0, length] نگه می‌داره و خالی‌ها رو دور می‌ریزه */
    fun clip(spans: List<InlineSpan>, length: Int): List<InlineSpan> =
        spans.mapNotNull {
            val a = it.start.coerceIn(0, length)
            val b = it.end.coerceIn(0, length)
            if (b > a) it.copy(start = a, end = b) else null
        }

    /** فقط بخش [from, to) رو برمی‌داره و موقعیتش رو از صفر شروع می‌کنه */
    fun slice(spans: List<InlineSpan>, from: Int, to: Int): List<InlineSpan> =
        spans.mapNotNull {
            val a = maxOf(it.start, from)
            val b = minOf(it.end, to)
            if (b > a) it.copy(start = a - from, end = b - from) else null
        }

    /** بازه‌های هم‌نوع که به هم چسبیدن یا روی هم افتادن رو یکی می‌کنه */
    fun normalize(spans: List<InlineSpan>): List<InlineSpan> {
        val out = ArrayList<InlineSpan>()
        spans.filter { it.end > it.start }
            .sortedWith(compareBy({ it.kind.ordinal }, { it.url }, { it.start }))
            .forEach { s ->
                val last = out.lastOrNull()
                if (last != null && last.kind == s.kind && last.url == s.url && s.start <= last.end) {
                    out[out.lastIndex] = last.copy(end = maxOf(last.end, s.end))
                } else {
                    out.add(s)
                }
            }
        return out.sortedWith(compareBy({ it.start }, { it.end }, { it.kind.ordinal }))
    }

    /**
     * وقتی متن از [old] به [new] عوض شد، بازه‌ها رو هم جابه‌جا می‌کنه (تایپ، پاک کردن، جایگزینی).
     * تغییر رو به‌صورت یک تکه‌ی وسط پیدا می‌کنه: بخش مشترک اول و آخر متن.
     */
    fun shift(old: String, new: String, spans: List<InlineSpan>): List<InlineSpan> {
        if (spans.isEmpty() || old == new) return spans
        val max = minOf(old.length, new.length)
        var p = 0
        while (p < max && old[p] == new[p]) p++
        var q = 0
        while (q < max - p && old[old.length - 1 - q] == new[new.length - 1 - q]) q++
        val oldEnd = old.length - q
        val newEnd = new.length - q
        val delta = new.length - old.length
        val pureInsert = p == oldEnd

        val moved = spans.mapNotNull { s ->
            val a = when {
                s.start >= oldEnd -> s.start + delta
                s.start <= p -> s.start
                else -> newEnd
            }
            val b = when {
                s.end >= oldEnd -> if (pureInsert && s.end == p && s.kind == SpanKind.LINK) s.end else s.end + delta
                s.end <= p -> s.end
                else -> newEnd
            }
            if (b > a) s.copy(start = a, end = b) else null
        }
        return normalize(clip(moved, new.length))
    }

    /** آیا کل بازه‌ی [start, end) قالب [kind] داره؟ */
    fun covers(spans: List<InlineSpan>, kind: SpanKind, start: Int, end: Int): Boolean =
        spans.any { it.kind == kind && it.start <= start && it.end >= end }

    /** اولین لینکی که با بازه‌ی [start, end) همپوشانی داره */
    fun linkAt(spans: List<InlineSpan>, start: Int, end: Int): InlineSpan? =
        spans.firstOrNull { it.kind == SpanKind.LINK && it.start < end && it.end > start }

    /** قالب [kind] رو از بازه‌ی [start, end) برمی‌داره */
    fun remove(spans: List<InlineSpan>, kind: SpanKind, start: Int, end: Int): List<InlineSpan> {
        val out = ArrayList<InlineSpan>()
        for (s in spans) {
            if (s.kind != kind || s.end <= start || s.start >= end) {
                out.add(s)
                continue
            }
            if (s.start < start) out.add(s.copy(end = start))
            if (s.end > end) out.add(s.copy(start = end))
        }
        return normalize(out)
    }

    /** اگه کل بازه قالب داره برش می‌داره، وگرنه به کل بازه اضافه‌ش می‌کنه */
    fun toggle(spans: List<InlineSpan>, kind: SpanKind, start: Int, end: Int): List<InlineSpan> {
        if (end <= start) return spans
        return if (covers(spans, kind, start, end)) {
            remove(spans, kind, start, end)
        } else {
            normalize(remove(spans, kind, start, end) + InlineSpan(start, end, kind))
        }
    }

    /** لینک رو روی بازه می‌ذاره و لینک‌های قبلیِ همون بازه رو عوض می‌کنه */
    fun setLink(spans: List<InlineSpan>, start: Int, end: Int, url: String): List<InlineSpan> {
        if (end <= start) return spans
        val cleaned = remove(spans, SpanKind.LINK, start, end)
        return normalize(cleaned + InlineSpan(start, end, SpanKind.LINK, url))
    }

    // ───────────── محدوده‌های خط و متن برای ذخیره ─────────────

    /** محدوده‌ی متن بعد از حذف فاصله‌های اول و آخر؛ اگه متن خالی بود null */
    fun trimRange(text: String): Pair<Int, Int>? {
        var a = 0
        while (a < text.length && text[a].isWhitespace()) a++
        var b = text.length
        while (b > a && text[b - 1].isWhitespace()) b--
        return if (b > a) a to b else null
    }

    /** محدوده‌ی هر خط غیرخالیِ متن (بدون فاصله‌های اول و آخرِ خط) */
    fun lineRanges(text: String): List<Pair<Int, Int>> {
        val out = ArrayList<Pair<Int, Int>>()
        var pos = 0
        for (line in text.split('\n')) {
            trimRange(line)?.let { (a, b) -> out.add((pos + a) to (pos + b)) }
            pos += line.length + 1
        }
        return out
    }

    // ───────────── خوندن از HTML ─────────────

    /**
     * HTML داخل یک بلوک رو به متن ساده + بازه‌ها تبدیل می‌کنه. فقط strong/em/s/لینک ساده و br پشتیبانی می‌شه؛
     * هر چیز دیگه‌ای یا هر شکلی که موقع ذخیره عیناً برنگرده، null می‌ده تا بلوک خام بمونه.
     */
    fun parse(source: String): Parsed? {
        class Open(val kind: SpanKind, val start: Int, val url: String)

        val sb = StringBuilder()
        val spans = ArrayList<InlineSpan>()
        val stack = ArrayList<Open>()
        var last = 0
        for (m in TOKEN.findAll(source)) {
            val piece = source.substring(last, m.range.first)
            if (piece.contains('<')) return null
            sb.append(unescape(piece))
            last = m.range.last + 1

            val value = m.value.lowercase()
            when {
                value.startsWith("<br") -> sb.append('\n')
                value == "</a>" -> {
                    val top = stack.removeLastOrNull() ?: return null
                    if (top.kind != SpanKind.LINK) return null
                    spans.add(InlineSpan(top.start, sb.length, SpanKind.LINK, top.url))
                }
                value.startsWith("<a ") -> stack.add(Open(SpanKind.LINK, sb.length, unescape(m.groupValues[3])))
                else -> {
                    val kind = when (m.groupValues[2].lowercase()) {
                        "strong" -> SpanKind.BOLD
                        "em" -> SpanKind.ITALIC
                        else -> SpanKind.STRIKE
                    }
                    if (m.groupValues[1] == "/") {
                        val top = stack.removeLastOrNull() ?: return null
                        if (top.kind != kind) return null
                        spans.add(InlineSpan(top.start, sb.length, kind))
                    } else {
                        stack.add(Open(kind, sb.length, ""))
                    }
                }
            }
        }
        val tail = source.substring(last)
        if (tail.contains('<')) return null
        sb.append(unescape(tail))
        if (stack.isNotEmpty()) return null

        val parsed = Parsed(sb.toString(), normalize(spans))
        // اگه بعد از ساختن دوباره، تگ‌ها دقیقاً همون نبودن (ترتیب یا ساختار فرق می‌کرد)، بلوک خام می‌مونه
        val again = html(parsed.text, parsed.spans)
        return if (skeleton(source) == skeleton(again)) parsed else null
    }

    private fun skeleton(text: String): List<String> =
        TOKEN.findAll(text).map { m ->
            val v = m.value.lowercase()
            when {
                v.startsWith("<br") -> "<br>"
                v.startsWith("<a ") -> "a:" + unescape(m.groupValues[3])
                else -> v
            }
        }.toList()

    // ───────────── ساختن HTML ─────────────

    /** بخش [from, to) از [text] رو با قالب‌بندی‌هاش به HTML امن تبدیل می‌کنه؛ خط جدید می‌شه <br> */
    fun html(text: String, spans: List<InlineSpan>, from: Int = 0, to: Int = text.length): String {
        val a0 = from.coerceIn(0, text.length)
        val b0 = to.coerceIn(a0, text.length)
        if (a0 == b0) return ""
        val clean = clip(spans, text.length)

        val cuts = TreeSet<Int>()
        cuts.add(a0)
        cuts.add(b0)
        clean.forEach {
            if (it.start in a0..b0) cuts.add(it.start)
            if (it.end in a0..b0) cuts.add(it.end)
        }
        val points = cuts.toList()

        val keys = ArrayList<List<InlineSpan>>()
        val pieces = ArrayList<StringBuilder>()
        for (k in 0 until points.size - 1) {
            val x = points[k]
            val y = points[k + 1]
            val key = clean
                .filter { it.start <= x && it.end >= y }
                .map { InlineSpan(0, 0, it.kind, it.url) }
                .distinctBy { it.kind }
                .sortedBy { rank(it.kind) }
            if (keys.isNotEmpty() && keys.last() == key) {
                pieces.last().append(text, x, y)
            } else {
                keys.add(key)
                pieces.add(StringBuilder().append(text, x, y))
            }
        }

        val out = StringBuilder()
        for (i in keys.indices) {
            val body = esc(pieces[i].toString()).replace("\r\n", "\n").replace("\n", "<br>")
            val close = StringBuilder()
            for (s in keys[i]) {
                when (s.kind) {
                    SpanKind.LINK -> {
                        out.append("<a href=\"").append(attr(s.url)).append("\">")
                        close.insert(0, "</a>")
                    }
                    SpanKind.BOLD -> {
                        out.append("<strong>")
                        close.insert(0, "</strong>")
                    }
                    SpanKind.ITALIC -> {
                        out.append("<em>")
                        close.insert(0, "</em>")
                    }
                    SpanKind.STRIKE -> {
                        out.append("<s>")
                        close.insert(0, "</s>")
                    }
                }
            }
            out.append(body).append(close)
        }
        return out.toString()
    }

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun attr(s: String): String = esc(s).replace("\"", "&quot;")

    fun unescape(s: String): String {
        var r = s.replace("&nbsp;", "\u00A0")
        r = Regex("&#(\\d+);").replace(r) { m ->
            m.groupValues[1].toIntOrNull()
                ?.takeIf { it in 1..0x10FFFF }
                ?.let { String(Character.toChars(it)) } ?: m.value
        }
        r = Regex("&#[xX]([0-9a-fA-F]+);").replace(r) { m ->
            m.groupValues[1].toIntOrNull(16)
                ?.takeIf { it in 1..0x10FFFF }
                ?.let { String(Character.toChars(it)) } ?: m.value
        }
        return r.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }
}
