package com.appdony.app.data.model

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * تبدیل نشانه‌گذاری گوتنبرگ (<!-- wp:paragraph --> ...) به بلوک‌های ادیتور.
 * هر بلوکی که بدون از دست رفتن چیزی قابل تبدیل نباشه (ویژگی‌های اضافه، قالب‌بندی داخل متن، بلوک‌های ناشناخته)
 * عیناً به‌صورت بلوک «خام» نگه داشته می‌شه و موقع ذخیره دقیقاً همون‌طور برمی‌گرده.
 */
object BlockParser {

    private val TOKEN = Regex(
        """<!--\s+(/)?wp:([a-z][a-z0-9_-]*(?:/[a-z][a-z0-9_-]*)?)(?:\s+(\{.*?\}))?\s*(/)?-->""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val COMMENT = Regex("""<!--.*?-->""", RegexOption.DOT_MATCHES_ALL)
    private val BLANK_LINE = Regex("""\n\s*\n""")
    private val P_ONLY = Regex("""^<p>(.*)</p>$""", RegexOption.DOT_MATCHES_ALL)
    private val P_ITEM = Regex("""<p>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val LI_ITEM = Regex("""<li>(.*?)</li>""", RegexOption.DOT_MATCHES_ALL)
    private val HEADING = Regex("""^<h([1-6]) class="wp-block-heading">(.*)</h\1>$""", RegexOption.DOT_MATCHES_ALL)
    private val LIST = Regex("""^<(ul|ol)(?: class="wp-block-list")?>(.*)</\1>$""", RegexOption.DOT_MATCHES_ALL)
    private val QUOTE = Regex("""^<blockquote class="wp-block-quote">(.*)</blockquote>$""", RegexOption.DOT_MATCHES_ALL)
    private val PULLQUOTE = Regex(
        """^<figure class="wp-block-pullquote"><blockquote><p>(.*)</p></blockquote></figure>$""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val CODE = Regex("""^<pre class="wp-block-code"><code>(.*)</code></pre>$""", RegexOption.DOT_MATCHES_ALL)
    private val PRE = Regex("""^<pre class="wp-block-preformatted">(.*)</pre>$""", RegexOption.DOT_MATCHES_ALL)
    private val VERSE = Regex("""^<pre class="wp-block-verse">(.*)</pre>$""", RegexOption.DOT_MATCHES_ALL)
    private val BUTTONS = Regex(
        """^<div class="wp-block-buttons">\s*<!-- wp:button -->\s*<div class="wp-block-button"><a class="wp-block-button__link wp-element-button"(?: href="([^"]*)")?>(.*?)</a></div>\s*<!-- /wp:button -->\s*</div>$""",
        RegexOption.DOT_MATCHES_ALL
    )
    private val BR = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val EMBED_KEYS = setOf("url", "type", "providerNameSlug", "responsive", "className")

    /** [content] = content.raw وردپرس. شناسه‌ی همه‌ی بلوک‌ها صفره و ViewModel شناسه‌ی واقعی می‌ده. */
    fun parse(content: String): List<EditorBlock> {
        val out = ArrayList<EditorBlock>()
        var depth = 0
        var cursor = 0
        var start = 0
        var innerStart = 0
        var name = ""
        var attrs: String? = null

        for (m in TOKEN.findAll(content)) {
            val closing = m.groupValues[1] == "/"
            val selfClosing = m.groupValues[4] == "/"
            val tokenName = normalize(m.groupValues[2])
            val tokenAttrs = m.groupValues[3].ifEmpty { null }

            if (depth == 0) {
                if (closing) continue
                freeform(content.substring(cursor, m.range.first), out)
                if (selfClosing) {
                    out.add(convert(tokenName, tokenAttrs, "", m.value))
                    cursor = m.range.last + 1
                } else {
                    depth = 1
                    start = m.range.first
                    innerStart = m.range.last + 1
                    name = tokenName
                    attrs = tokenAttrs
                }
            } else {
                when {
                    selfClosing -> Unit
                    closing -> {
                        depth--
                        if (depth == 0) {
                            val inner = content.substring(innerStart, m.range.first)
                            val whole = content.substring(start, m.range.last + 1)
                            out.add(convert(name, attrs, inner, whole))
                            cursor = m.range.last + 1
                        }
                    }
                    else -> depth++
                }
            }
        }

        if (depth > 0) {
            // بلوک بسته‌نشده: همه‌ی باقی‌مونده عیناً نگه داشته می‌شه
            out.add(raw(content.substring(start)))
            cursor = content.length
        }
        freeform(content.substring(cursor), out)
        return out
    }

    private fun normalize(name: String): String = if (name.contains('/')) name else "core/$name"

    private fun raw(markup: String): EditorBlock =
        EditorBlock(id = 0, type = BlockType.HTML, text = markup.trim(), extra = BlockSerializer.RAW)

    private fun block(type: BlockType, text: String, extra: String = ""): EditorBlock =
        EditorBlock(id = 0, type = type, text = text, extra = extra)

    // ───────────── متن آزاد بین بلوک‌ها (نوشته‌های قدیمیِ کلاسیک) ─────────────

    private fun freeform(text: String, out: MutableList<EditorBlock>) {
        val t = text.trim()
        if (t.isEmpty()) return
        t.split(BLANK_LINE).map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            val simple = if (!part.contains('<')) {
                unescape(part)
            } else {
                P_ONLY.find(part)?.let { plain(it.groupValues[1]) }
            }
            out.add(if (simple != null) block(BlockType.PARAGRAPH, simple) else raw(part))
        }
    }

    // ───────────── تبدیل هر بلوک ─────────────

    private fun convert(name: String, attrsJson: String?, inner: String, whole: String): EditorBlock {
        val attrs = attrsOf(attrsJson) ?: return raw(whole)
        val keys = attrs.keySet()
        val body = inner.trim()

        val converted: EditorBlock? = when (name) {
            "core/paragraph" ->
                if (keys.isEmpty()) P_ONLY.find(body)?.let { m -> plain(m.groupValues[1])?.let { block(BlockType.PARAGRAPH, it) } }
                else null

            "core/heading" -> {
                val m = HEADING.find(body)
                val level = m?.groupValues?.get(1)
                val declared = attrs.get("level")?.let { runCatching { it.asInt.toString() }.getOrNull() }
                if (m != null && keys.all { it == "level" } && (declared == null || declared == level)) {
                    plain(m.groupValues[2])?.takeIf { !it.contains('\n') }?.let { block(BlockType.HEADING, it, level ?: "2") }
                } else null
            }

            "core/list" -> list(attrs, body)

            "core/quote" ->
                if (keys.isEmpty()) quote(body) else null

            "core/pullquote" ->
                if (keys.isEmpty()) PULLQUOTE.find(body)?.let { m -> plain(m.groupValues[1])?.let { block(BlockType.PULLQUOTE, it) } }
                else null

            "core/code" ->
                if (keys.isEmpty()) CODE.find(body)?.let { block(BlockType.CODE, unescape(it.groupValues[1])) } else null

            "core/preformatted" ->
                if (keys.isEmpty()) PRE.find(body)?.let { m -> plain(m.groupValues[1])?.let { block(BlockType.PREFORMATTED, it) } }
                else null

            "core/verse" ->
                if (keys.isEmpty()) VERSE.find(body)?.let { m -> plain(m.groupValues[1])?.let { block(BlockType.VERSE, it) } }
                else null

            "core/buttons" ->
                if (keys.isEmpty()) BUTTONS.find(body)?.let { m ->
                    plain(m.groupValues[2])?.takeIf { it.isNotBlank() && !it.contains('\n') }
                        ?.let { text -> block(BlockType.BUTTON, text, unescape(m.groupValues[1])) }
                } else null

            "core/separator" ->
                if (keys.isEmpty() && body.startsWith("<hr")) block(BlockType.SEPARATOR, "") else null

            "core/spacer" ->
                if (keys.isEmpty()) block(BlockType.SPACER, "") else null

            "core/more" ->
                if (keys.isEmpty()) block(BlockType.MORE, "") else null

            "core/html" ->
                if (keys.isEmpty() && body.isNotEmpty()) block(BlockType.HTML, body) else null

            "core/shortcode" ->
                if (keys.isEmpty() && body.isNotEmpty()) block(BlockType.SHORTCODE, body) else null

            "core/latest-posts" ->
                if (keys.isEmpty() && body.isEmpty()) block(BlockType.LATEST_POSTS, "") else null

            "core/categories" ->
                if (keys.isEmpty() && body.isEmpty()) block(BlockType.CATEGORIES, "") else null

            "core/embed" -> embed(attrs, body)

            else -> null
        }
        return converted ?: raw(whole)
    }

    private fun list(attrs: JsonObject, body: String): EditorBlock? {
        if (!attrs.keySet().all { it == "ordered" }) return null
        val ordered = attrs.get("ordered")?.let { runCatching { it.asBoolean }.getOrNull() } == true
        val m = LIST.find(body) ?: return null
        if ((m.groupValues[1] == "ol") != ordered) return null
        val content = m.groupValues[2]
        if (content.contains("<ul") || content.contains("<ol")) return null
        val leftover = content.replace(COMMENT, "").replace(LI_ITEM, "").trim()
        if (leftover.isNotEmpty()) return null
        val items = ArrayList<String>()
        for (match in LI_ITEM.findAll(content)) {
            val text = plain(match.groupValues[1]) ?: return null
            items.add(text)
        }
        if (items.isEmpty() || items.any { it.contains('\n') }) return null
        return block(if (ordered) BlockType.ORDERED_LIST else BlockType.LIST, items.joinToString("\n"))
    }

    private fun quote(body: String): EditorBlock? {
        val m = QUOTE.find(body) ?: return null
        val content = m.groupValues[1]
        val leftover = content.replace(COMMENT, "").replace(P_ITEM, "").trim()
        if (leftover.isNotEmpty()) return null
        val paragraphs = ArrayList<String>()
        for (match in P_ITEM.findAll(content)) {
            val text = plain(match.groupValues[1]) ?: return null
            paragraphs.add(text)
        }
        if (paragraphs.isEmpty() || paragraphs.any { it.contains('\n') }) return null
        return block(BlockType.QUOTE, paragraphs.joinToString("\n"))
    }

    private fun embed(attrs: JsonObject, body: String): EditorBlock? {
        if (!EMBED_KEYS.containsAll(attrs.keySet())) return null
        if (body.contains("<figcaption")) return null
        val url = attrs.get("url")?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()
        if (url.isEmpty()) return null
        val provider = attrs.get("providerNameSlug")?.takeIf { it.isJsonPrimitive }?.asString
        val type = when (provider) {
            "youtube" -> BlockType.YOUTUBE
            "twitter" -> BlockType.TWITTER
            else -> BlockType.EMBED
        }
        return block(type, url)
    }

    // ───────────── ابزارها ─────────────

    private fun attrsOf(json: String?): JsonObject? {
        if (json.isNullOrBlank()) return JsonObject()
        return runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull()
    }

    /** متن ساده: <br> می‌شه خط جدید؛ اگه تگ دیگه‌ای (لینک، بولد، ...) داشت null تا بلوک خام بمونه */
    private fun plain(html: String): String? {
        val withBreaks = html.replace(BR, "\n")
        if (withBreaks.contains('<')) return null
        return unescape(withBreaks)
    }

    private fun unescape(s: String): String {
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
