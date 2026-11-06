package com.appdony.app.data.seo

import android.content.Context
import com.appdony.app.data.api.AiRunner
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.EditorBlock
import org.json.JSONObject

/** طرز اعمال یک پیشنهاد روی بلوک: جایگزین کردن کل متن، یا اضافه شدن یه جمله به اول متن */
enum class SeoApply { REPLACE, PREPEND }

/** یک پیشنهاد هوش مصنوعی؛ تا کاربر تأیید نکنه روی نوشته اعمال نمی‌شه */
data class SeoProposal(
    val field: SeoField,
    val label: String,
    val oldValue: String,
    val newValue: String,
    val blockId: Long? = null,
    val apply: SeoApply = SeoApply.REPLACE
)

object SeoAi {

    private const val SYSTEM = """
تو یک متخصص سئوی فارسی (در حد افزونه‌ی Yoast SEO) و ویراستار سایت «اپدونی» (مرجع دانلود بازی و برنامه‌ی اندروید) هستی.
قوانین:
- خروجی فقط یک شیء JSON معتبر باشه؛ بدون توضیح، بدون بک‌تیک و بدون هیچ متن اضافه.
- همه‌ی متن‌ها فارسی روان و طبیعی باشن. اسم برنامه و برند رو همون‌طور که هست بنویس.
- هیچ اطلاعاتی (قیمت، نسخه، ویژگی، آمار، لینک) از خودت نساز؛ فقط از متن داده‌شده استفاده کن.
- کلمه‌ی کلیدی باید عیناً (با همین املا) و طبیعی توی هر بخشی که ازت می‌خوام بیاد؛ تکرار زورکی نکن.
- لحن جذاب و انسانی باشه؛ کلیک‌بیت و اغراق نکن.
"""

    suspend fun propose(
        context: Context,
        input: SeoInput,
        requested: Set<SeoField>
    ): Result<List<SeoProposal>> {
        val blocks = input.blocks
        val textBlocks = blocks.filter { it.type.supportsInline && it.text.isNotBlank() }
        if (input.title.isBlank() && textBlocks.isEmpty()) {
            return Result.failure(Exception("اول یه عنوان یا کمی متن بنویس تا هوش مصنوعی بتونه سئو رو بهینه کنه."))
        }

        val intro = blocks.firstOrNull { it.type == BlockType.PARAGRAPH && it.text.isNotBlank() }
        val heading = blocks.firstOrNull { it.type == BlockType.HEADING && it.text.isNotBlank() }
        val allImages = blocks.filter { it.type == BlockType.IMAGE }
        val imageTargets = allImages.filter { needsAlt(it, input.keyword) }.take(5)

        val wanted = LinkedHashSet<SeoField>()
        for (f in SeoField.entries) {
            val want = f in requested || (f == SeoField.KEYWORD && input.keyword.isBlank())
            if (!want) continue
            when (f) {
                SeoField.INTRO -> if (intro != null) wanted.add(f)
                SeoField.HEADING -> if (heading != null) wanted.add(f)
                SeoField.ALT -> if (imageTargets.isNotEmpty()) wanted.add(f)
                else -> wanted.add(f)
            }
        }
        if (wanted.isEmpty()) return Result.success(emptyList())

        val introHasSpans = intro != null && intro.spans.isNotEmpty()
        val user = buildPrompt(input, textBlocks, wanted, intro, introHasSpans, heading, imageTargets)

        val reply = AiRunner.ask(context, SYSTEM.trim(), user, maxTokens = 3500)
            .getOrElse { return Result.failure(it) }
        val json = parseJson(reply)
            ?: return Result.failure(Exception("جواب هوش مصنوعی خوانا نبود. دوباره امتحان کن."))

        val out = ArrayList<SeoProposal>()

        // کلمه‌ی کلیدی (ممکنه جدید باشه؛ بقیه‌ی بخش‌ها باید با همون هماهنگ باشن)
        var keyword = input.keyword.trim()
        if (SeoField.KEYWORD in wanted) {
            val k = clean(json.optString("focus_keyword")).take(60)
            if (k.isNotBlank() && k != keyword) {
                out += SeoProposal(SeoField.KEYWORD, SeoField.KEYWORD.label, input.keyword.trim(), k)
                keyword = k
            }
        }

        if (SeoField.TITLE in wanted) {
            var t = clamp(clean(json.optString("seo_title")), 60)
            if (t.isNotBlank() && keyword.isNotBlank() && !SeoText.containsKeyword(t, keyword)) {
                val prefixed = "$keyword | $t"
                if (prefixed.length <= 60) t = prefixed
            }
            if (t.isNotBlank() && t != input.seoTitle.trim()) {
                out += SeoProposal(SeoField.TITLE, SeoField.TITLE.label, input.seoTitle.trim(), t)
            }
        }

        if (SeoField.META in wanted) {
            val m = clamp(clean(json.optString("meta_description")), 160)
            if (m.isNotBlank() && m != input.metaDescription.trim()) {
                out += SeoProposal(SeoField.META, SeoField.META.label, input.metaDescription.trim(), m)
            }
        }

        if (SeoField.INTRO in wanted && intro != null) {
            val v = clean(json.optString("intro"))
            if (v.isNotBlank()) {
                out += if (introHasSpans) {
                    SeoProposal(
                        SeoField.INTRO, "جمله‌ی آغازین پاراگراف اول (به اولش اضافه می‌شه)",
                        "", v, intro.id, SeoApply.PREPEND
                    )
                } else if (v != intro.text.trim()) {
                    SeoProposal(SeoField.INTRO, "بازنویسی پاراگراف اول", intro.text.trim(), v, intro.id)
                } else {
                    null
                } ?: return@propose Result.success(out)
            }
        }

        if (SeoField.HEADING in wanted && heading != null) {
            val h = clamp(clean(json.optString("heading")), 90)
            if (h.isNotBlank() && h != heading.text.trim()) {
                out += SeoProposal(
                    SeoField.HEADING, "بازنویسی زیرعنوان", heading.text.trim(), h, heading.id
                )
            }
        }

        if (SeoField.ALT in wanted) {
            val arr = json.optJSONArray("alts")
            if (arr != null) {
                imageTargets.forEachIndexed { i, img ->
                    val a = clamp(clean(arr.optString(i)), 120)
                    if (a.isNotBlank()) {
                        val order = allImages.indexOfFirst { it.id == img.id } + 1
                        out += SeoProposal(
                            SeoField.ALT, "متن جایگزین تصویر $order",
                            img.extra.trim(), a, img.id
                        )
                    }
                }
            }
        }

        return Result.success(out)
    }

    private fun needsAlt(block: EditorBlock, keyword: String): Boolean {
        val alt = block.extra.trim()
        return alt.isEmpty() || (keyword.isNotBlank() && !SeoText.containsKeyword(alt, keyword))
    }

    private fun buildPrompt(
        input: SeoInput,
        textBlocks: List<EditorBlock>,
        wanted: Set<SeoField>,
        intro: EditorBlock?,
        introHasSpans: Boolean,
        heading: EditorBlock?,
        imageTargets: List<EditorBlock>
    ): String = buildString {
        appendLine("اطلاعات نوشته:")
        appendLine("عنوان نوشته: ${input.title.ifBlank { "(ننوشته)" }}")
        appendLine("کلمه‌ی کلیدی فعلی: ${input.keyword.ifBlank { "(ندارد)" }}")
        appendLine("عنوان سئوی فعلی: ${input.seoTitle.ifBlank { "(خالی)" }}")
        appendLine("توضیحات متای فعلی: ${input.metaDescription.ifBlank { "(خالی)" }}")

        val headings = textBlocks.filter { it.type == BlockType.HEADING }.map { it.text.trim() }
        if (headings.isNotEmpty()) {
            appendLine("زیرعنوان‌ها:")
            headings.take(10).forEach { appendLine("- $it") }
        }
        if (intro != null) {
            appendLine("پاراگراف اول (کامل):")
            appendLine(intro.text.trim().take(900))
        }
        val body = textBlocks.filter { it.type != BlockType.HEADING }.joinToString("\n") { it.text }
        if (body.isNotBlank()) {
            appendLine("بخشی از متن:")
            appendLine(body.take(1800))
        }

        if (SeoField.ALT in wanted) {
            appendLine("تصاویری که متن جایگزین (alt) می‌خوان، به ترتیب:")
            imageTargets.forEachIndexed { i, img ->
                val idx = input.blocks.indexOfFirst { it.id == img.id }
                val near = input.blocks.subList(0, idx.coerceAtLeast(0))
                    .lastOrNull { it.type.supportsInline && it.text.isNotBlank() }?.text?.take(140).orEmpty()
                appendLine("${i + 1}) alt فعلی: ${img.extra.ifBlank { "(خالی)" }} | نزدیک‌ترین متن قبلش: $near")
            }
        }

        appendLine()
        appendLine("فقط این کلیدها رو توی JSON بنویس:")
        if (SeoField.KEYWORD in wanted) {
            appendLine("\"focus_keyword\": عبارت کلیدی ۲ تا ۳ کلمه‌ای که کاربر فارسی‌زبان توی گوگل جستجو می‌کنه و موضوع اصلی همین متنه")
        } else {
            appendLine("(کلمه‌ی کلیدی همین «${input.keyword.trim()}» است و تغییرش نده)")
        }
        if (SeoField.TITLE in wanted) {
            appendLine("\"seo_title\": عنوان سئو، حداکثر ۵۸ نویسه، کلمه‌ی کلیدی اول عنوان، جذاب")
        }
        if (SeoField.META in wanted) {
            appendLine("\"meta_description\": توضیحات متا بین ۱۲۰ تا ۱۵۵ نویسه، شامل کلمه‌ی کلیدی عیناً، خلاصه‌ی جذاب با دعوت به خوندن")
        }
        if (SeoField.INTRO in wanted) {
            if (introHasSpans) {
                appendLine("\"intro\": فقط یک جمله‌ی آغازین کوتاه که کلمه‌ی کلیدی عیناً توش باشه و به اول پاراگراف اول اضافه می‌شه؛ اطلاعات جدید اضافه نکن")
            } else {
                appendLine("\"intro\": همین پاراگراف اول رو بازنویسی کن؛ معنی و واقعیت‌ها همون بمونه، کلمه‌ی کلیدی عیناً توی جمله‌ی اول بیاد، طولش حداکثر ۱۵ درصد بیشتر بشه")
            }
        }
        if (SeoField.HEADING in wanted && heading != null) {
            appendLine("\"heading\": این زیرعنوان رو بازنویسی کن: «${heading.text.trim()}» ← هم‌معنی، کوتاه (حداکثر ۷۰ نویسه) و با کلمه‌ی کلیدی عیناً")
        }
        if (SeoField.ALT in wanted) {
            appendLine("\"alts\": آرایه‌ای از ${imageTargets.size} متن جایگزین به همون ترتیب؛ هرکدوم ۵ تا ۱۲ کلمه، توصیفی، و کلمه‌ی کلیدی فقط وقتی به تصویر ربط داره")
        }
    }

    private fun parseJson(raw: String): JSONObject? {
        val t = raw.replace("```json", "").replace("```", "").trim()
        val a = t.indexOf('{')
        val b = t.lastIndexOf('}')
        if (a < 0 || b <= a) return null
        return try { JSONObject(t.substring(a, b + 1)) } catch (e: Exception) { null }
    }

    private fun clean(raw: String): String {
        var s = raw.trim()
        if (s.equals("null", ignoreCase = true)) return ""
        s = s.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        return s.trim('"', '“', '”', '«', '»', '\'', ' ')
    }

    /** اگه از سقف بلندتر بود، سر یه فاصله می‌بُره تا وسط کلمه قطع نشه */
    private fun clamp(s: String, max: Int): String {
        if (s.length <= max) return s
        val cut = s.substring(0, max)
        val sp = cut.lastIndexOf(' ')
        val base = if (sp > max * 0.6) cut.substring(0, sp) else cut
        return base.trimEnd(' ', '،', ',', '-', '|', ':', '؛')
    }
}
