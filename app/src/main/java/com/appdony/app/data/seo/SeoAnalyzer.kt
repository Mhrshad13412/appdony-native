package com.appdony.app.data.seo

import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.EditorBlock
import com.appdony.app.data.model.SpanKind
import java.net.URLDecoder
import java.util.Locale

/** وضعیت هر بررسی؛ مثل چراغ‌های قرمز / نارنجی / سبز Yoast */
enum class SeoLevel { PROBLEM, IMPROVE, GOOD }

/** بخش‌هایی که هوش مصنوعی می‌تونه برای سئو بنویسه یا درست کنه */
enum class SeoField(val label: String) {
    KEYWORD("کلمه‌ی کلیدی کانونی"),
    TITLE("عنوان سئو"),
    META("توضیحات متا"),
    INTRO("پاراگراف اول"),
    HEADING("زیرعنوان"),
    ALT("متن جایگزین تصویر")
}

/** نتیجه‌ی یک بررسی. [aiField] اگه پر باشه، دکمه‌ی «درستش کن» با هوش مصنوعی نشون داده می‌شه */
data class SeoCheck(
    val title: String,
    val level: SeoLevel,
    val message: String,
    val aiField: SeoField? = null,
    val suggestLinks: Boolean = false
)

/** ورودی آنالیز: یه کپی غیرقابل‌تغییر از چیزی که توی ادیتوره */
data class SeoInput(
    val title: String,
    val blocks: List<EditorBlock>,
    val keyword: String,
    val seoTitle: String,
    val metaDescription: String,
    val slug: String,
    val hasFeatured: Boolean
)

/** ابزارهای متن فارسی: یکسان‌سازی حروف و نیم‌فاصله، شمارش و پیدا کردن کلمه‌ی کلیدی */
object SeoText {

    private val SPACES = Regex("\\s+")

    fun norm(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when {
                ch == 'ي' || ch == 'ى' -> sb.append('ی')
                ch == 'ك' -> sb.append('ک')
                ch == '\u200c' || ch == '\u200f' || ch == '\u200e' || ch == '\u00a0' -> sb.append(' ')
                ch in '\u064B'..'\u065F' || ch == '\u0670' || ch == 'ـ' -> Unit
                ch in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                ch in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return SPACES.replace(sb.toString(), " ").trim()
    }

    fun words(s: String): List<String> {
        val n = norm(s)
        if (n.isEmpty()) return emptyList()
        return n.split(' ').filter { w -> w.any { it.isLetterOrDigit() } }
    }

    /** عبارت کامل توی متن باشه، یا (برای چندکلمه‌ای) همه‌ی کلمه‌هاش توی متن باشن */
    fun containsKeyword(text: String, keyword: String): Boolean {
        val k = norm(keyword)
        if (k.isEmpty()) return false
        val t = norm(text)
        if (t.contains(k)) return true
        val parts = k.split(' ').filter { it.length >= 2 }
        return parts.size > 1 && parts.all { t.contains(it) }
    }

    fun countKeyword(text: String, keyword: String): Int {
        val k = norm(keyword)
        if (k.isEmpty()) return 0
        val t = norm(text)
        var n = 0
        var i = t.indexOf(k)
        while (i >= 0) {
            n++
            i = t.indexOf(k, i + k.length)
        }
        return n
    }
}

/** آنالیزور سئو؛ مجموعه‌ای از بررسی‌های شبیه Yoast که روی نوشته‌ی توی ادیتور اجرا می‌شه */
object SeoAnalyzer {

    const val SITE_HOST = "appdony.ir"

    private fun isInternal(url: String): Boolean {
        val u = url.trim().lowercase(Locale.US)
        if (u.startsWith("//")) {
            val rest = u.removePrefix("//")
            return rest.startsWith(SITE_HOST) || rest.startsWith("www.$SITE_HOST")
        }
        if (u.startsWith("/")) return true
        if (u.startsWith("http://") || u.startsWith("https://")) {
            val host = u.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore('#')
            return host == SITE_HOST || host.endsWith(".$SITE_HOST")
        }
        return false
    }

    private fun isOutbound(url: String): Boolean {
        val u = url.trim().lowercase(Locale.US)
        val web = u.startsWith("http://") || u.startsWith("https://") || u.startsWith("//")
        return web && !isInternal(u)
    }

    fun analyze(input: SeoInput): List<SeoCheck> {
        val out = ArrayList<SeoCheck>()
        val keyword = input.keyword.trim()
        val hasKeyword = keyword.isNotEmpty()
        val kwWords = SeoText.words(keyword).size

        val textBlocks = input.blocks.filter { it.type.supportsInline && it.text.isNotBlank() }
        val body = textBlocks.joinToString("\n") { it.text }
        val wordCount = SeoText.words(body).size
        val firstPara = input.blocks
            .firstOrNull { it.type == BlockType.PARAGRAPH && it.text.isNotBlank() }?.text.orEmpty()
        val headings = input.blocks.filter { it.type == BlockType.HEADING && it.text.isNotBlank() }
        val images = input.blocks.filter { it.type == BlockType.IMAGE }

        val seoTitle = input.seoTitle.trim()
        val effTitle = seoTitle.ifEmpty { input.title.trim() }
        val meta = input.metaDescription.trim()

        // ───── کلمه‌ی کلیدی ─────
        if (!hasKeyword) {
            out += SeoCheck(
                "کلمه‌ی کلیدی کانونی", SeoLevel.PROBLEM,
                "کلمه‌ی کلیدی کانونی تعیین نشده؛ بدونش بیشتر بررسی‌های سئو انجام نمی‌شه.",
                SeoField.KEYWORD
            )
        } else {
            out += SeoCheck("کلمه‌ی کلیدی کانونی", SeoLevel.GOOD, "کلمه‌ی کلیدی تعیین شده: «$keyword».")
            if (kwWords > 4) {
                out += SeoCheck(
                    "طول کلمه‌ی کلیدی", SeoLevel.IMPROVE,
                    "کلمه‌ی کلیدی $kwWords کلمه‌ست؛ بهتره حداکثر ۴ کلمه باشه تا هدفمند بمونه.",
                    SeoField.KEYWORD
                )
            } else {
                out += SeoCheck("طول کلمه‌ی کلیدی", SeoLevel.GOOD, "طول کلمه‌ی کلیدی مناسبه.")
            }
        }

        // ───── عنوان سئو ─────
        if (hasKeyword) {
            val which = if (seoTitle.isEmpty()) "نوشته" else "سئو"
            when {
                effTitle.isEmpty() -> out += SeoCheck(
                    "کلمه‌ی کلیدی در عنوان سئو", SeoLevel.PROBLEM,
                    "هیچ عنوانی نوشته نشده.", SeoField.TITLE
                )
                !SeoText.containsKeyword(effTitle, keyword) -> out += SeoCheck(
                    "کلمه‌ی کلیدی در عنوان سئو", SeoLevel.PROBLEM,
                    "کلمه‌ی کلیدی توی عنوان $which نیست.", SeoField.TITLE
                )
                !SeoText.norm(effTitle).startsWith(SeoText.norm(keyword)) -> out += SeoCheck(
                    "کلمه‌ی کلیدی در عنوان سئو", SeoLevel.IMPROVE,
                    "کلمه‌ی کلیدی توی عنوان هست ولی بهتره اول عنوان بیاد.", SeoField.TITLE
                )
                else -> out += SeoCheck(
                    "کلمه‌ی کلیدی در عنوان سئو", SeoLevel.GOOD,
                    "کلمه‌ی کلیدی اول عنوان اومده؛ عالیه."
                )
            }
        }
        val tl = seoTitle.length
        out += when {
            tl == 0 -> SeoCheck(
                "طول عنوان سئو", SeoLevel.IMPROVE,
                "عنوان سئو خالیه و گوگل از عنوان نوشته استفاده می‌کنه. یه عنوان ۳۰ تا ۶۰ نویسه‌ای بنویس.",
                SeoField.TITLE
            )
            tl < 30 -> SeoCheck(
                "طول عنوان سئو", SeoLevel.IMPROVE,
                "عنوان سئو کوتاهه ($tl نویسه)؛ ۳۰ تا ۶۰ نویسه ایده‌آله.", SeoField.TITLE
            )
            tl > 60 -> SeoCheck(
                "طول عنوان سئو", SeoLevel.IMPROVE,
                "عنوان سئو بلنده ($tl نویسه) و ممکنه توی گوگل بریده بشه؛ تا ۶۰ نویسه.", SeoField.TITLE
            )
            else -> SeoCheck("طول عنوان سئو", SeoLevel.GOOD, "طول عنوان سئو مناسبه ($tl نویسه).")
        }

        // ───── توضیحات متا ─────
        val ml = meta.length
        out += when {
            ml == 0 -> SeoCheck(
                "طول توضیحات متا", SeoLevel.PROBLEM,
                "توضیحات متا ننوشتی؛ گوگل خودش یه تکه‌ی تصادفی از متن رو نشون می‌ده.",
                SeoField.META
            )
            ml < 120 -> SeoCheck(
                "طول توضیحات متا", SeoLevel.IMPROVE,
                "توضیحات متا کوتاهه ($ml نویسه)؛ ۱۲۰ تا ۱۶۰ نویسه بهتره.", SeoField.META
            )
            ml > 160 -> SeoCheck(
                "طول توضیحات متا", SeoLevel.IMPROVE,
                "توضیحات متا بلنده ($ml نویسه) و توی گوگل بریده می‌شه؛ تا ۱۶۰ نویسه.", SeoField.META
            )
            else -> SeoCheck("طول توضیحات متا", SeoLevel.GOOD, "طول توضیحات متا مناسبه ($ml نویسه).")
        }
        if (hasKeyword) {
            out += when {
                ml == 0 -> SeoCheck(
                    "کلمه‌ی کلیدی در توضیحات متا", SeoLevel.PROBLEM,
                    "عبارت کلیدی توی توضیحات متا نیست (توضیحات خالیه).", SeoField.META
                )
                !SeoText.containsKeyword(meta, keyword) -> SeoCheck(
                    "کلمه‌ی کلیدی در توضیحات متا", SeoLevel.PROBLEM,
                    "عبارت کلیدی توی توضیحات متا نیست؛ یه توضیح متا شامل اون بنویس.", SeoField.META
                )
                else -> SeoCheck(
                    "کلمه‌ی کلیدی در توضیحات متا", SeoLevel.GOOD,
                    "کلمه‌ی کلیدی توی توضیحات متا هست."
                )
            }
        }

        // ───── پاراگراف اول ─────
        if (hasKeyword) {
            out += when {
                firstPara.isBlank() -> SeoCheck(
                    "کلمه‌ی کلیدی در پاراگراف اول", SeoLevel.PROBLEM,
                    "هنوز پاراگرافی ننوشتی."
                )
                !SeoText.containsKeyword(firstPara, keyword) -> SeoCheck(
                    "کلمه‌ی کلیدی در پاراگراف اول", SeoLevel.PROBLEM,
                    "کلمه‌ی کلیدی یا مترادفش توی پاراگراف اول نیست. با آوردنش، خواننده و گوگل زودتر می‌فهمن متن درباره‌ی چیه.",
                    SeoField.INTRO
                )
                else -> SeoCheck(
                    "کلمه‌ی کلیدی در پاراگراف اول", SeoLevel.GOOD,
                    "کلمه‌ی کلیدی توی پاراگراف اول هست."
                )
            }
        }

        // ───── زیرعنوان‌ها ─────
        if (headings.isEmpty()) {
            if (wordCount >= 300) {
                out += SeoCheck(
                    "زیرعنوان‌ها", SeoLevel.IMPROVE,
                    "متنت بلنده ولی هیچ زیرعنوانی نداره؛ زیرعنوان‌ها خوندن و سئو رو بهتر می‌کنن."
                )
            }
        } else if (hasKeyword) {
            out += if (headings.any { SeoText.containsKeyword(it.text, keyword) }) {
                SeoCheck("کلمه‌ی کلیدی در زیرعنوان‌ها", SeoLevel.GOOD, "کلمه‌ی کلیدی توی زیرعنوان‌ها هست.")
            } else {
                SeoCheck(
                    "کلمه‌ی کلیدی در زیرعنوان‌ها", SeoLevel.IMPROVE,
                    "هیچ زیرعنوانی کلمه‌ی کلیدی رو نداره؛ حداقل یکی‌شون باید داشته باشه.",
                    SeoField.HEADING
                )
            }
        }

        // ───── تراکم و طول متن ─────
        if (hasKeyword && wordCount >= 50) {
            val count = SeoText.countKeyword(body, keyword)
            val density = count * 100.0 / wordCount
            val pct = String.format(Locale.US, "%.1f", density)
            out += when {
                count == 0 -> SeoCheck(
                    "تراکم کلمه‌ی کلیدی", SeoLevel.PROBLEM,
                    "کلمه‌ی کلیدی توی متن اصلاً نیومده؛ چند بار و طبیعی توی متن بیارش."
                )
                density < 0.5 -> SeoCheck(
                    "تراکم کلمه‌ی کلیدی", SeoLevel.IMPROVE,
                    "کلمه‌ی کلیدی $count بار اومده (حدود $pct درصد)؛ بین ۰٫۵ تا ۲٫۵ درصد بهتره."
                )
                density <= 2.5 -> SeoCheck(
                    "تراکم کلمه‌ی کلیدی", SeoLevel.GOOD,
                    "کلمه‌ی کلیدی $count بار اومده (حدود $pct درصد)؛ تراکم عالیه."
                )
                density <= 3.5 -> SeoCheck(
                    "تراکم کلمه‌ی کلیدی", SeoLevel.IMPROVE,
                    "کلمه‌ی کلیدی $count بار اومده (حدود $pct درصد)؛ یکم زیاده، چندتاش رو با مترادف عوض کن."
                )
                else -> SeoCheck(
                    "تراکم کلمه‌ی کلیدی", SeoLevel.PROBLEM,
                    "کلمه‌ی کلیدی $count بار اومده (حدود $pct درصد)؛ خیلی تکرار شده و گوگل اسپم حسابش می‌کنه."
                )
            }
        }
        out += when {
            wordCount < 200 -> SeoCheck(
                "طول متن", SeoLevel.PROBLEM,
                "متن فقط $wordCount کلمه‌ست؛ برای سئو حداقل ۳۰۰ کلمه بنویس."
            )
            wordCount < 300 -> SeoCheck(
                "طول متن", SeoLevel.IMPROVE,
                "متن $wordCount کلمه‌ست؛ ۳۰۰ کلمه یا بیشتر بهتره."
            )
            else -> SeoCheck("طول متن", SeoLevel.GOOD, "متن $wordCount کلمه‌ست؛ طولش مناسبه.")
        }

        // ───── لینک‌ها ─────
        val urls = ArrayList<String>()
        input.blocks.forEach { b ->
            b.spans.forEach { if (it.kind == SpanKind.LINK && it.url.isNotBlank()) urls.add(it.url.trim()) }
            if (b.type == BlockType.BUTTON && b.extra.isNotBlank()) urls.add(b.extra.trim())
        }
        val internalCount = urls.count { isInternal(it) }
        val outboundCount = urls.count { isOutbound(it) }
        out += if (internalCount == 0) {
            SeoCheck(
                "لینک‌های داخلی", SeoLevel.PROBLEM,
                "هیچ لینک داخلی توی این مطلب نیست؛ چند لینک به مطالب مرتبط اپدونی اضافه کن.",
                suggestLinks = true
            )
        } else {
            SeoCheck("لینک‌های داخلی", SeoLevel.GOOD, "$internalCount لینک داخلی داری.")
        }
        out += if (outboundCount == 0) {
            SeoCheck(
                "لینک‌های خروجی", SeoLevel.PROBLEM,
                "هیچ لینک خروجی نیست؛ به یه منبع معتبر لینک بده (آدرسش رو خودت بذار، هوش مصنوعی لینک نمی‌سازه)."
            )
        } else {
            SeoCheck("لینک‌های خروجی", SeoLevel.GOOD, "$outboundCount لینک خروجی داری.")
        }

        // ───── تصاویر ─────
        if (images.isEmpty()) {
            out += SeoCheck("تصاویر", SeoLevel.IMPROVE, "هیچ تصویری توی متن نیست؛ تصویر مرتبط، متن رو جذاب‌تر می‌کنه.")
        } else {
            val noAlt = images.count { it.extra.isBlank() }
            out += when {
                noAlt > 0 -> SeoCheck(
                    "تصاویر", SeoLevel.PROBLEM,
                    "$noAlt تصویر متن جایگزین (alt) ندارن.", SeoField.ALT
                )
                hasKeyword && images.none { SeoText.containsKeyword(it.extra, keyword) } -> SeoCheck(
                    "تصاویر", SeoLevel.IMPROVE,
                    "متن جایگزین هیچ تصویری کلمه‌ی کلیدی رو نداره.", SeoField.ALT
                )
                else -> SeoCheck("تصاویر", SeoLevel.GOOD, "همه‌ی تصاویر متن جایگزین دارن.")
            }
        }

        // ───── نامک ─────
        val slug = input.slug.trim()
        if (hasKeyword && slug.isNotEmpty()) {
            val decoded = try { URLDecoder.decode(slug, "UTF-8") } catch (e: Exception) { slug }
            val readable = decoded.replace('-', ' ').replace('_', ' ')
            out += if (SeoText.containsKeyword(readable, keyword)) {
                SeoCheck("نامک", SeoLevel.GOOD, "نامک کلمه‌ی کلیدی رو داره.")
            } else {
                SeoCheck(
                    "نامک", SeoLevel.IMPROVE,
                    "نامک (آدرس مطلب) کلمه‌ی کلیدی رو نداره. اگه نامک انگلیسیه، معادل انگلیسی کلمه رو توش بیار."
                )
            }
        }

        // ───── تصویر شاخص ─────
        out += if (input.hasFeatured) {
            SeoCheck("تصویر شاخص", SeoLevel.GOOD, "تصویر شاخص گذاشتی.")
        } else {
            SeoCheck("تصویر شاخص", SeoLevel.IMPROVE, "تصویر شاخص نداری؛ برای نتیجه‌ی گوگل و اشتراک‌گذاری مهمه.")
        }

        return out
    }
}

/** خلاصه‌ی نوشته‌ی توی ادیتور برای چت هوش مصنوعی (شناور و بیرون برنامه) */
object SeoContext {

    fun build(isBlog: Boolean, input: SeoInput, checks: List<SeoCheck>): String {
        val textBlocks = input.blocks.filter { it.type.supportsInline && it.text.isNotBlank() }
        if (input.title.isBlank() && textBlocks.isEmpty()) return ""

        val body = textBlocks.filter { it.type != BlockType.HEADING }.joinToString("\n") { it.text }
        val headings = textBlocks.filter { it.type == BlockType.HEADING }.map { it.text.trim() }
        val wordCount = SeoText.words(textBlocks.joinToString("\n") { it.text }).size
        val issues = checks.filter { it.level != SeoLevel.GOOD }

        return buildString {
            appendLine("نوع: ${if (isBlog) "مطلب وبلاگ" else "نوشته"}")
            appendLine("عنوان: ${input.title.ifBlank { "(ننوشته)" }}")
            appendLine("کلمه‌ی کلیدی کانونی: ${input.keyword.ifBlank { "(تعیین نشده)" }}")
            appendLine("عنوان سئو (${input.seoTitle.length} نویسه): ${input.seoTitle.ifBlank { "(خالی)" }}")
            appendLine("توضیحات متا (${input.metaDescription.length} نویسه): ${input.metaDescription.ifBlank { "(خالی)" }}")
            appendLine("تعداد کلمه‌ی متن: $wordCount")
            if (headings.isNotEmpty()) {
                appendLine("زیرعنوان‌ها:")
                headings.take(10).forEach { appendLine("- $it") }
            }
            if (body.isNotBlank()) {
                appendLine("ابتدای متن:")
                appendLine(body.take(1200))
            }
            if (issues.isNotEmpty()) {
                appendLine("مشکلات و بهبودهای سئو (طبق آنالیزور اپ):")
                issues.take(10).forEach { appendLine("- ${it.title}: ${it.message}") }
            } else {
                appendLine("آنالیزور سئوی اپ مشکلی پیدا نکرده.")
            }
        }.trim()
    }
}
