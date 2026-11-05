package com.appdony.app.data.api

import com.appdony.app.data.model.AppInfo
import com.appdony.app.data.model.CustomBox
import com.appdony.app.data.model.DlLink
import com.appdony.app.data.model.SeoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * خوندن و ذخیره‌ی متاباکس‌های playclone و Yoast. این‌ها توی REST باز نیستن، پس دقیقاً مثل خود گوتنبرگ عمل می‌کنیم:
 * صفحه‌ی ویرایش رو می‌خونیم (نانس‌ها و فیلدهای مخفی)، فرم متاباکس رو با آدرس meta-box-loader می‌فرستیم
 * و آخرش مقدار رو دوباره از سایت می‌خونیم تا ذخیره شدنش تأیید بشه.
 */
object MetaApi {

    private const val SITE = "https://appdony.ir"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val BASE_NAMES = setOf(
        "_wpnonce", "_wp_http_referer", "user_ID", "action", "originalaction", "post_type",
        "original_post_status", "referredby", "_wp_original_http_referer", "post_ID",
        "meta-box-order-nonce", "closedpostboxesnonce", "samplepermalinknonce"
    )

    private val INPUT = Regex("<input\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val TEXTAREA = Regex("<textarea\\b([^>]*)>(.*?)</textarea>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val CHECKED = Regex("(?<![\\w-])checked\\b", RegexOption.IGNORE_CASE)
    private val SELECTED = Regex("(?<![\\w-])selected\\b", RegexOption.IGNORE_CASE)
    private val OPTION = Regex("<option\\b([^>]*)>", RegexOption.IGNORE_CASE)

    private class Page(val html: String, val fields: Map<String, String>)

    // ───────────── خوندن (برای باز کردن مطلب موجود) ─────────────

    /** مقدار فعلی اطلاعات برنامه/بازی و Yoast یک مطلب، دقیقاً از صفحه‌ی ویرایش سایت */
    suspend fun load(postId: Long): Result<Pair<AppInfo, SeoInfo>> = withContext(Dispatchers.IO) {
        try {
            val page = fetchPage(http(), postId)
            Result.success(parse(page))
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("اطلاعات برنامه گرفته نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    private fun parse(page: Page): Pair<AppInfo, SeoInfo> {
        val f = page.fields
        fun v(key: String): String = f[key].orEmpty()

        val links = rows(f, "playclone_dl_links")
            .map { DlLink(texto = it["texto"].orEmpty(), link = it["link"].orEmpty(), page = it["page"].orEmpty()) }
            .ifEmpty { listOf(DlLink()) }
        val boxes = rows(f, "playclone_boxes")
            .map { CustomBox(title = it["title"].orEmpty(), content = it["content"].orEmpty()) }
            .ifEmpty { listOf(CustomBox()) }

        val info = AppInfo(
            descripcion = v("playclone_info[descripcion]"),
            version = v("playclone_info[version]"),
            tamano = v("playclone_info[tamano]"),
            fechaActualizacion = v("playclone_info[fecha_actualizacion]"),
            requerimientos = v("playclone_info[requerimientos]"),
            consiguelo = v("playclone_info[consiguelo]"),
            novedades = v("playclone_info[novedades]"),
            dlOption = f["playclone_dl_option"] ?: "direct-download",
            directDownload = v("playclone_direct_download"),
            dlLinks = links,
            screenshots = v("playclone_screenshots"),
            videoId = v("playclone_video_id"),
            boxes = boxes,
            ratingAverage = v("playclone_rating_average"),
            ratingCount = v("playclone_rating_count"),
            appdoniPlus = f.containsKey("playclone_appdoni_plus"),
            contentSource = selectValue(page.html, "playclone_content_source") ?: "own",
            contentSourceCustom = v("playclone_content_source_custom")
        )
        val seo = SeoInfo(
            focusKeyword = v("yoast_wpseo_focuskw"),
            title = v("yoast_wpseo_title"),
            description = v("yoast_wpseo_metadesc")
        )
        return info to seo
    }

    /** فیلدهای ردیفی مثل playclone_dl_links[0][texto] → لیستی از نقشه‌ها به ترتیب اندیس */
    private fun rows(fields: Map<String, String>, prefix: String): List<Map<String, String>> {
        val re = Regex("^" + Regex.escape(prefix) + "\\[(\\d+)\\]\\[(\\w+)\\]$")
        val byIndex = sortedMapOf<Int, MutableMap<String, String>>()
        for ((key, value) in fields) {
            val m = re.find(key) ?: continue
            byIndex.getOrPut(m.groupValues[1].toInt()) { mutableMapOf() }[m.groupValues[2]] = value
        }
        return byIndex.values.toList()
    }

    /** مقدار گزینه‌ی انتخاب‌شده‌ی یک select (اگه هیچ‌کدوم انتخاب نبود، اولی) */
    private fun selectValue(html: String, name: String): String? {
        val select = Regex(
            "<select\\b[^>]*name=\"" + Regex.escape(name) + "\"[^>]*>(.*?)</select>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html) ?: return null
        val options = OPTION.findAll(select.groupValues[1]).toList()
        val chosen = options.firstOrNull { SELECTED.containsMatchIn(it.groupValues[1]) } ?: options.firstOrNull()
        return chosen?.let { attr(it.groupValues[1], "value") }
    }

    // ───────────── ذخیره ─────────────

    suspend fun save(postId: Long, info: AppInfo, seo: SeoInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val client = http()
            val page = fetchPage(client, postId)
            val url = metaBoxUrl(page.html, postId)
                ?: throw EditorException("فرم اطلاعات برنامه روی سایت پیدا نشد (متاباکس‌ها).")
            val form = buildForm(page.fields, info, seo)

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Referer", "$SITE/wp-admin/post.php?post=$postId&action=edit")
                .post(form)
                .build()
            val code = try {
                client.newCall(request).execute().use { it.code }
            } catch (e: IOException) {
                throw EditorException("اینترنت قطعه یا سایت جواب نداد.")
            }
            if (code !in 200..399) throw EditorException("سایت اطلاعات برنامه رو نپذیرفت (کد $code).")

            verify(client, postId, info, seo)
            Result.success(Unit)
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("اطلاعات برنامه ذخیره نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    // ───────────── ساخت فرم ─────────────

    private fun buildForm(scraped: Map<String, String>, info: AppInfo, seo: SeoInfo): FormBody {
        val map = LinkedHashMap<String, String>()
        for ((name, value) in scraped) {
            if (name in BASE_NAMES || name.startsWith("yoast_") || name == "playclone_app_meta_nonce") {
                map[name] = value
            }
        }

        if (seo != SeoInfo()) {
            map["yoast_wpseo_focuskw"] = seo.focusKeyword
            map["yoast_wpseo_title"] = seo.title
            map["yoast_wpseo_metadesc"] = seo.description
        }

        map["playclone_info[descripcion]"] = info.descripcion
        map["playclone_info[version]"] = info.version
        map["playclone_info[tamano]"] = info.tamano
        map["playclone_info[fecha_actualizacion]"] = info.fechaActualizacion
        map["playclone_info[requerimientos]"] = info.requerimientos
        map["playclone_info[consiguelo]"] = info.consiguelo
        map["playclone_info[novedades]"] = info.novedades
        map["playclone_dl_option"] = info.dlOption
        map["playclone_direct_download"] = info.directDownload
        info.dlLinks
            .filter { it.texto.isNotBlank() || it.link.isNotBlank() || it.page.isNotBlank() }
            .forEachIndexed { i, row ->
                map["playclone_dl_links[$i][texto]"] = row.texto
                map["playclone_dl_links[$i][link]"] = row.link
                map["playclone_dl_links[$i][page]"] = row.page
            }
        map["playclone_screenshots"] = info.screenshots
        map["playclone_video_id"] = info.videoId
        info.boxes
            .filter { it.title.isNotBlank() || it.content.isNotBlank() }
            .forEachIndexed { i, box ->
                map["playclone_boxes[$i][title]"] = box.title
                map["playclone_boxes[$i][content]"] = box.content
            }
        map["playclone_rating_average"] = info.ratingAverage
        map["playclone_rating_count"] = info.ratingCount
        if (info.appdoniPlus) map["playclone_appdoni_plus"] = "1"
        map["playclone_content_source"] = info.contentSource
        map["playclone_content_source_custom"] = info.contentSourceCustom

        val builder = FormBody.Builder()
        map.forEach { (k, v) -> builder.add(k, v) }
        return builder.build()
    }

    private suspend fun verify(client: OkHttpClient, postId: Long, info: AppInfo, seo: SeoInfo) {
        val (name, expected) = when {
            info.version.isNotBlank() -> "playclone_info[version]" to info.version
            seo.focusKeyword.isNotBlank() -> "yoast_wpseo_focuskw" to seo.focusKeyword
            info.descripcion.isNotBlank() -> "playclone_info[descripcion]" to info.descripcion
            else -> return
        }
        val after = fetchPage(client, postId).fields[name].orEmpty()
        if (after.trim() != expected.trim()) {
            throw EditorException("ذخیره‌ی اطلاعات برنامه روی سایت تأیید نشد؛ سایت مقدار رو عوض نکرد.")
        }
    }

    // ───────────── خواندن صفحه‌ی ویرایش ─────────────

    private fun http(): OkHttpClient {
        val jar = ApiClient.cookieJarInstance
            ?: throw EditorException("اپ هنوز آماده نیست. دوباره امتحان کن.")
        return OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun fetchPage(client: OkHttpClient, postId: Long): Page {
        val request = Request.Builder()
            .url("$SITE/wp-admin/post.php?post=$postId&action=edit")
            .header("User-Agent", UA)
            .get()
            .build()
        val html = try {
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) throw EditorException("صفحه‌ی ویرایش باز نشد (کد ${resp.code}).")
                resp.body?.string().orEmpty()
            }
        } catch (e: IOException) {
            throw EditorException("اینترنت قطعه یا سایت جواب نداد.")
        }
        if (!html.contains("name=\"post_ID\"")) {
            throw EditorException("صفحه‌ی ویرایش سایت پیدا نشد؛ شاید نشستت منقضی شده.")
        }
        return Page(html, scrape(html))
    }

    private fun scrape(html: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in INPUT.findAll(html)) {
            val tag = m.value
            val name = attr(tag, "name") ?: continue
            val type = (attr(tag, "type") ?: "text").lowercase()
            if (type == "button" || type == "submit" || type == "file" || type == "image" || type == "reset") continue
            if ((type == "checkbox" || type == "radio") && !CHECKED.containsMatchIn(tag)) continue
            out.putIfAbsent(name, unescape(attr(tag, "value") ?: ""))
        }
        for (m in TEXTAREA.findAll(html)) {
            val name = attr(m.groupValues[1], "name") ?: continue
            out.putIfAbsent(name, unescape(m.groupValues[2]))
        }
        return out
    }

    /** آدرس ذخیره‌ی متاباکس (با نانس meta-box-loader) که گوتنبرگ توی صفحه می‌ذاره */
    private fun metaBoxUrl(html: String, postId: Long): String? {
        Regex("_wpMetaBoxUrl\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").find(html)?.let {
            val url = jsonUnescape(it.groupValues[1])
            if (url.startsWith("http")) return url
            if (url.startsWith("/")) return SITE + url
        }
        Regex("meta-box-loader-nonce=([A-Za-z0-9]+)").find(html)?.let {
            return "$SITE/wp-admin/post.php?post=$postId&action=edit&meta-box-loader=1&meta-box-loader-nonce=${it.groupValues[1]}"
        }
        return null
    }

    private fun attr(tag: String, name: String): String? {
        val m = Regex("(?<![\\w-])$name\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')", RegexOption.IGNORE_CASE).find(tag) ?: return null
        return m.groups[1]?.value ?: m.groups[2]?.value
    }

    private fun unescape(s: String): String = s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#039;", "'")
        .replace("&#39;", "'")
        .replace("&amp;", "&")

    private fun jsonUnescape(s: String): String =
        Regex("\\\\u([0-9a-fA-F]{4})").replace(s) { it.groupValues[1].toInt(16).toChar().toString() }
            .replace("\\/", "/")
}
