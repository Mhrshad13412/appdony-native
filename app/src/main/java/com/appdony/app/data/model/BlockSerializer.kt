package com.appdony.app.data.model

/**
 * تبدیل بلوک‌های ادیتور به نشانه‌گذاری گوتنبرگ (<!-- wp:paragraph --> ...).
 * بلوک‌هایی که هنوز ویرایش کاملشون نیومده توی [Result.skipped] میان و ذخیره نمی‌شن.
 * بلوک «خام» (HTML با extra = [RAW]) همون نشانه‌گذاریِ اصلی سایته و بدون هیچ تغییری برمی‌گرده.
 */
object BlockSerializer {

    /** نشانه‌ی بلوک خام: بلوک‌هایی که از سایت اومدن و ادیتور نمی‌تونه بدون از دست دادن چیزی ویرایششون کنه */
    const val RAW = "raw"

    class Result(val html: String, val skipped: List<String>)

    fun serialize(blocks: List<EditorBlock>): Result {
        val parts = ArrayList<String>()
        val skipped = ArrayList<String>()
        for (block in blocks) {
            if (block.type.editing == BlockEditing.PLACEHOLDER && !isDynamic(block.type)) {
                skipped.add(block.type.label)
                continue
            }
            val html = one(block) ?: continue
            parts.add(html)
        }
        return Result(parts.joinToString("\n\n"), skipped)
    }

    private fun isDynamic(type: BlockType): Boolean =
        type == BlockType.LATEST_POSTS || type == BlockType.CATEGORIES || type == BlockType.SEARCH

    private fun one(block: EditorBlock): String? {
        val text = block.text.trim()
        return when (block.type) {
            BlockType.PARAGRAPH -> {
                if (text.isEmpty()) null else wrap("paragraph", null, "<p>${inline(text)}</p>")
            }

            BlockType.HEADING -> {
                if (text.isEmpty()) return null
                val level = block.extra.ifBlank { "2" }
                val attrs = if (level == "2") null else "{\"level\":$level}"
                wrap("heading", attrs, "<h$level class=\"wp-block-heading\">${inline(text)}</h$level>")
            }

            BlockType.LIST, BlockType.ORDERED_LIST -> {
                val items = block.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (items.isEmpty()) return null
                val ordered = block.type.ordered
                val tag = if (ordered) "ol" else "ul"
                val inner = items.joinToString("\n\n") {
                    "<!-- wp:list-item -->\n<li>${inline(it)}</li>\n<!-- /wp:list-item -->"
                }
                wrap(
                    "list",
                    if (ordered) "{\"ordered\":true}" else null,
                    "<$tag class=\"wp-block-list\">$inner</$tag>"
                )
            }

            BlockType.QUOTE -> {
                val lines = block.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
                if (lines.isEmpty()) return null
                val inner = lines.joinToString("\n\n") { wrap("paragraph", null, "<p>${inline(it)}</p>") }
                wrap("quote", null, "<blockquote class=\"wp-block-quote\">$inner</blockquote>")
            }

            BlockType.PULLQUOTE -> {
                if (text.isEmpty()) return null
                wrap(
                    "pullquote",
                    null,
                    "<figure class=\"wp-block-pullquote\"><blockquote><p>${inline(text)}</p></blockquote></figure>"
                )
            }

            BlockType.CODE -> {
                if (text.isEmpty()) return null
                wrap("code", null, "<pre class=\"wp-block-code\"><code>${esc(block.text.trimEnd())}</code></pre>")
            }

            BlockType.PREFORMATTED -> {
                if (text.isEmpty()) return null
                wrap("preformatted", null, "<pre class=\"wp-block-preformatted\">${inline(block.text.trimEnd())}</pre>")
            }

            BlockType.VERSE -> {
                if (text.isEmpty()) return null
                wrap("verse", null, "<pre class=\"wp-block-verse\">${inline(block.text.trimEnd())}</pre>")
            }

            BlockType.BUTTON -> {
                if (text.isEmpty()) return null
                val link = block.extra.trim()
                val href = if (link.isEmpty()) "" else " href=\"${attr(link)}\""
                val button = wrap(
                    "button",
                    null,
                    "<div class=\"wp-block-button\"><a class=\"wp-block-button__link wp-element-button\"$href>${inline(text)}</a></div>"
                )
                wrap("buttons", null, "<div class=\"wp-block-buttons\">$button</div>")
            }

            BlockType.SEPARATOR -> wrap(
                "separator",
                null,
                "<hr class=\"wp-block-separator has-alpha-channel-opacity\"/>"
            )

            BlockType.SPACER -> wrap(
                "spacer",
                null,
                "<div style=\"height:100px\" aria-hidden=\"true\" class=\"wp-block-spacer\"></div>"
            )

            BlockType.MORE -> wrap("more", null, "<!--more-->")

            BlockType.HTML -> {
                when {
                    text.isEmpty() -> null
                    block.extra == RAW -> text // نشانه‌گذاری اصلیِ سایت؛ بدون دست‌کاری
                    else -> wrap("html", null, text)
                }
            }

            BlockType.SHORTCODE -> {
                if (text.isEmpty()) null else wrap("shortcode", null, block.text.trim())
            }

            BlockType.LATEST_POSTS -> "<!-- wp:latest-posts /-->"
            BlockType.CATEGORIES -> "<!-- wp:categories /-->"
            BlockType.SEARCH -> "<!-- wp:search {\"label\":\"جستجو\",\"buttonText\":\"جستجو\"} /-->"

            BlockType.YOUTUBE -> embed(
                url = text,
                attrs = "\"type\":\"video\",\"providerNameSlug\":\"youtube\",\"responsive\":true,\"className\":\"wp-embed-aspect-16-9 wp-has-aspect-ratio\"",
                classes = "is-type-video is-provider-youtube wp-block-embed-youtube wp-embed-aspect-16-9 wp-has-aspect-ratio"
            )

            BlockType.TWITTER -> embed(
                url = text,
                attrs = "\"type\":\"rich\",\"providerNameSlug\":\"twitter\",\"responsive\":true",
                classes = "is-type-rich is-provider-twitter wp-block-embed-twitter"
            )

            BlockType.EMBED, BlockType.TELEGRAM -> embed(
                url = text,
                attrs = "\"responsive\":true",
                classes = ""
            )

            else -> null
        }
    }

    private fun embed(url: String, attrs: String, classes: String): String? {
        if (url.isEmpty()) return null
        val json = "{\"url\":${jsonStr(url)},$attrs}"
        val cls = if (classes.isEmpty()) "wp-block-embed" else "wp-block-embed $classes"
        return wrap(
            "embed",
            json,
            "<figure class=\"$cls\"><div class=\"wp-block-embed__wrapper\">\n${esc(url)}\n</div></figure>"
        )
    }

    private fun wrap(name: String, attrs: String?, body: String): String {
        val open = if (attrs == null) "<!-- wp:$name -->" else "<!-- wp:$name $attrs -->"
        return "$open\n$body\n<!-- /wp:$name -->"
    }

    /** متن ساده → HTML امن؛ خط جدید داخل یک بلوک می‌شه <br> */
    private fun inline(s: String): String = esc(s).replace("\r\n", "\n").replace("\n", "<br>")

    private fun esc(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun attr(s: String): String = esc(s).replace("\"", "&quot;")

    /** رشته‌ی JSON برای ویژگی‌های کامنتِ بلوک (همون escape که خود وردپرس می‌زنه) */
    private fun jsonStr(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\u0022")
                '\\' -> sb.append("\\\\")
                '<' -> sb.append("\\u003c")
                '>' -> sb.append("\\u003e")
                '&' -> sb.append("\\u0026")
                '\n', '\r', '\t' -> Unit
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString().replace("--", "\\u002d\\u002d")
    }
}
