package com.appdony.app.data.api

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.text.HtmlCompat
import com.appdony.app.data.model.WpCategory
import com.appdony.app.data.session.CredentialsManager
import com.appdony.app.data.session.SessionManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

data class SavedPost(val id: Long, val link: String, val status: String, val note: String? = null)

data class UploadedMedia(val id: Long, val url: String)

/** یک ردیف از لیست «نوشته‌ها / بلاگ‌ها» */
data class PostRow(
    val id: Long,
    val title: String,
    val status: String,
    val date: String,
    val thumb: String?
)

/** یک صفحه از لیست. [all] = true یعنی همه‌ی مطالب (مدیر/ویرایشگر)، false یعنی فقط مطالب خود کاربر */
data class PostPage(val rows: List<PostRow>, val hasMore: Boolean, val all: Boolean)

/** اطلاعات کامل یک مطلب برای باز شدن توی ادیتور */
data class RemotePost(
    val id: Long,
    val title: String,
    val content: String,
    val status: String,
    val link: String,
    val featuredId: Long?,
    val featuredUrl: String?,
    val featuredAlt: String = "",
    val slug: String,
    val date: String,
    val excerpt: String,
    val commentsOpen: Boolean,
    val categoryIds: Set<Int>
)

/** خطایی که پیامش فارسی و آماده‌ی نمایش به کاربره */
class EditorException(message: String) : Exception(message)

/**
 * ذخیره‌ی نوشته/وبلاگ، لیست و خوندن مطالب، دسته‌ها و آپلود تصویر با REST وردپرس.
 * احراز هویت با کوکی‌های نشست + هدر X-WP-Nonce؛ اگه نانس منقضی شده بود یه بار تازه می‌گیره
 * و اگه باز نشد با اطلاعات ذخیره‌شده دوباره وارد می‌شه.
 */
object EditorApi {

    private const val SITE = "https://appdony.ir"
    private const val NONCE_URL = "$SITE/wp-admin/admin-ajax.php?action=rest-nonce"
    private const val PAGE_SIZE = 20
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    private val NONCE_REGEX = Regex("^[A-Za-z0-9]{6,20}$")
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    private class Reply(val code: Int, val body: String)

    @Volatile
    private var blogRoute: String? = null

    // ───────────── ذخیره ─────────────

    /**
     * [postId] خالی = ساخت تازه، پر = به‌روزرسانی. [status]: draft / publish / pending.
     * [extra] فیلدهای اضافه‌ی REST (نامک، تاریخ، چکیده، دیدگاه‌ها، دسته‌ها).
     * اگه کاربر اجازه‌ی انتشار نداشت، خودکار «pending» ذخیره می‌شه.
     */
    suspend fun save(
        context: Context,
        kind: String,
        postId: Long?,
        title: String,
        content: String,
        status: String,
        featuredId: Long?,
        extra: JsonObject? = null
    ): Result<SavedPost> = withContext(Dispatchers.IO) {
        try {
            val base = route(context, kind)
            val url = if (postId == null) base else "$base/$postId"
            var wanted = status
            var note: String? = null

            fun request(nonce: String, st: String): Request =
                authed(Request.Builder().url(url), nonce)
                    .post(body(title, content, st, featuredId, postId != null, extra).toRequestBody(JSON_TYPE))
                    .build()

            var reply = send(context) { request(it, wanted) }
            if (wanted == "publish" && reply.code == 403 && reply.body.contains("rest_cannot_publish")) {
                wanted = "pending"
                note = "اجازه‌ی انتشار مستقیم نداری؛ برای بازبینی ذخیره شد"
                reply = send(context) { request(it, wanted) }
            }
            if (reply.code !in 200..299) throw EditorException(explain(kind, reply, upload = false))

            val obj = JsonParser.parseString(reply.body).asJsonObject
            Result.success(
                SavedPost(
                    id = obj.get("id").asLong,
                    link = obj.get("link")?.asString.orEmpty(),
                    status = obj.get("status")?.asString ?: wanted,
                    note = note
                )
            )
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("ذخیره نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    private fun body(
        title: String,
        content: String,
        status: String,
        featured: Long?,
        isUpdate: Boolean,
        extra: JsonObject?
    ): String {
        val o = JsonObject()
        extra?.entrySet()?.forEach { o.add(it.key, it.value) }
        o.addProperty("title", title)
        o.addProperty("content", content)
        o.addProperty("status", status)
        if (featured != null) o.addProperty("featured_media", featured)
        else if (isUpdate) o.addProperty("featured_media", 0)
        return o.toString()
    }

    /** آدرس REST هر نوع: نوشته ثابته؛ وبلاگ از types/blog خونده می‌شه (rest_base واقعی) */
    private suspend fun route(context: Context, kind: String): String {
        if (kind != "blog") return "$SITE/wp-json/wp/v2/posts"
        blogRoute?.let { return it }
        var found = "$SITE/wp-json/wp/v2/blog"
        val reply = send(context) { nonce ->
            authed(Request.Builder().url("$SITE/wp-json/wp/v2/types/blog?context=edit"), nonce).get().build()
        }
        if (reply.code in 200..299) {
            runCatching {
                val o = JsonParser.parseString(reply.body).asJsonObject
                val base = o.get("rest_base")?.asString ?: "blog"
                val ns = o.get("rest_namespace")?.asString ?: "wp/v2"
                found = "$SITE/wp-json/$ns/$base"
            }
            blogRoute = found
        }
        return found
    }

    // ───────────── لیست مطالب ─────────────

    /**
     * یک صفحه از مطالب. اگه کاربر اجازه‌ی ویرایش مطالب دیگران رو داشته باشه (مدیر، ویرایشگر) همه رو می‌گیره،
     * وگرنه فقط مطالب خودش رو. هیچ چیزی روی گوشی ذخیره نمی‌شه.
     */
    suspend fun list(context: Context, kind: String, page: Int): Result<PostPage> = withContext(Dispatchers.IO) {
        try {
            val base = route(context, kind)
            val (me, all) = access(context, kind)
            val url = buildString {
                append(base)
                append("?context=edit&per_page=").append(PAGE_SIZE).append("&page=").append(page)
                append("&orderby=date&order=desc&status=publish,future,draft,pending,private")
                append("&_fields=id,title,status,date,featured_media")
                if (!all) append("&author=").append(me)
            }
            val reply = send(context) { nonce -> authed(Request.Builder().url(url), nonce).get().build() }
            if (reply.code == 400 && reply.body.contains("rest_post_invalid_page_number")) {
                return@withContext Result.success(PostPage(emptyList(), false, all))
            }
            if (reply.code !in 200..299) throw EditorException(explain(kind, reply, upload = false))

            val items = JsonParser.parseString(reply.body).asJsonArray.map { it.asJsonObject }
            val featuredIds = items.mapNotNull { featuredOf(it) }
            val thumbs = thumbnails(context, featuredIds)
            val rows = items.map { o ->
                val title = HtmlCompat.fromHtml(field(o, "title"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                PostRow(
                    id = o.get("id").asLong,
                    title = title.ifBlank { "(بدون عنوان)" },
                    status = o.get("status")?.asString.orEmpty(),
                    date = o.get("date")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    thumb = featuredOf(o)?.let { thumbs[it] }
                )
            }
            Result.success(PostPage(rows, rows.size >= PAGE_SIZE, all))
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("لیست گرفته نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    /** شناسه‌ی کاربر و اینکه اجازه‌ی ویرایش مطالب دیگران رو داره یا نه */
    private suspend fun access(context: Context, kind: String): Pair<Long, Boolean> {
        val reply = send(context) { nonce ->
            authed(
                Request.Builder().url("$SITE/wp-json/wp/v2/users/me?context=edit&_fields=id,capabilities"),
                nonce
            ).get().build()
        }
        if (reply.code !in 200..299) throw EditorException(explain(kind, reply, upload = false))
        val o = JsonParser.parseString(reply.body).asJsonObject
        val id = o.get("id").asLong
        val caps = o.get("capabilities")?.takeIf { it.isJsonObject }?.asJsonObject
        fun has(key: String): Boolean {
            val e = caps?.get(key) ?: return false
            return e.isJsonPrimitive && runCatching { e.asBoolean }.getOrDefault(false)
        }
        val all = if (kind == "blog") has("edit_others_blogs") || has("edit_others_posts") else has("edit_others_posts")
        return id to all
    }

    private fun featuredOf(o: JsonObject): Long? =
        o.get("featured_media")?.takeIf { it.isJsonPrimitive }?.asLong?.takeIf { it > 0 }

    /** مقدار یه فیلد REST که یا رشته‌ست یا {raw, rendered} */
    private fun field(o: JsonObject, key: String): String {
        val e = o.get(key) ?: return ""
        if (e.isJsonPrimitive) return e.asString
        if (!e.isJsonObject) return ""
        val j = e.asJsonObject
        val v = j.get("raw")?.takeIf { it.isJsonPrimitive } ?: j.get("rendered")?.takeIf { it.isJsonPrimitive }
        return v?.asString.orEmpty()
    }

    /** آدرس تصویرک‌ها؛ اگه نشد لیست بدون تصویر نشون داده می‌شه */
    private suspend fun thumbnails(context: Context, ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        return try {
            val include = ids.distinct().joinToString(",")
            val reply = send(context) { nonce ->
                authed(
                    Request.Builder()
                        .url("$SITE/wp-json/wp/v2/media?include=$include&per_page=$PAGE_SIZE&_fields=id,source_url,media_details"),
                    nonce
                ).get().build()
            }
            if (reply.code !in 200..299) return emptyMap()
            val out = HashMap<Long, String>()
            JsonParser.parseString(reply.body).asJsonArray.forEach { e ->
                val o = e.asJsonObject
                val id = o.get("id")?.asLong ?: return@forEach
                val thumb = runCatching {
                    o.getAsJsonObject("media_details").getAsJsonObject("sizes")
                        .getAsJsonObject("thumbnail").get("source_url").asString
                }.getOrNull()
                val url = thumb ?: o.get("source_url")?.takeIf { it.isJsonPrimitive }?.asString
                if (!url.isNullOrBlank()) out[id] = url
            }
            out
        } catch (e: Exception) {
            emptyMap()
        }
    }

    // ───────────── خوندن یک مطلب ─────────────

    /** عنوان، محتوا، وضعیت، تصویر شاخص و تنظیمات یک مطلب (context=edit) */
    suspend fun fetch(context: Context, kind: String, id: Long): Result<RemotePost> = withContext(Dispatchers.IO) {
        try {
            val base = route(context, kind)
            val reply = send(context) { nonce ->
                authed(Request.Builder().url("$base/$id?context=edit"), nonce).get().build()
            }
            if (reply.code !in 200..299) throw EditorException(explain(kind, reply, upload = false))

            val o = JsonParser.parseString(reply.body).asJsonObject
            val status = o.get("status")?.takeIf { it.isJsonPrimitive }?.asString ?: "draft"
            val featuredId = featuredOf(o)
            val featuredMedia = if (featuredId != null) mediaInfo(context, featuredId) else null
            val featuredUrl = featuredMedia?.first
            // تاریخ فقط برای مطلب‌های منتشرشده/زمان‌بندی‌شده/خصوصی پر می‌شه؛ پیش‌نویس «همین حالا» می‌مونه
            val dated = status == "publish" || status == "future" || status == "private"
            val date = if (dated) {
                o.get("date")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty().replace('T', ' ').take(16)
            } else ""
            val categories = o.get("categories")?.takeIf { it.isJsonArray }?.asJsonArray
                ?.mapNotNull { runCatching { it.asInt }.getOrNull() }?.toSet() ?: emptySet()

            Result.success(
                RemotePost(
                    id = o.get("id").asLong,
                    title = field(o, "title"),
                    content = field(o, "content"),
                    status = status,
                    link = o.get("link")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    featuredId = featuredId,
                    featuredUrl = featuredUrl,
                    featuredAlt = featuredMedia?.second.orEmpty(),
                    slug = o.get("slug")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty(),
                    date = date,
                    excerpt = field(o, "excerpt"),
                    commentsOpen = (o.get("comment_status")?.takeIf { it.isJsonPrimitive }?.asString ?: "open") == "open",
                    categoryIds = categories
                )
            )
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("مطلب گرفته نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    /** آدرس و متن جایگزین (alt) یک رسانه؛ اگه نشد null */
    private suspend fun mediaInfo(context: Context, id: Long): Pair<String?, String>? = try {
        val reply = send(context) { nonce ->
            authed(Request.Builder().url("$SITE/wp-json/wp/v2/media/$id?_fields=source_url,alt_text"), nonce).get().build()
        }
        if (reply.code in 200..299) {
            val o = JsonParser.parseString(reply.body).asJsonObject
            val url = o.get("source_url")?.takeIf { it.isJsonPrimitive }?.asString?.ifBlank { null }
            val alt = o.get("alt_text")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
            url to alt
        } else null
    } catch (e: Exception) {
        null
    }

    /** متن جایگزین (alt) رسانه‌ی تصویر شاخص رو روی سایت به‌روز می‌کنه */
    suspend fun updateMediaAlt(context: Context, id: Long, alt: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val payload = JsonObject().apply { addProperty("alt_text", alt) }.toString()
            val reply = send(context) { nonce ->
                authed(Request.Builder().url("$SITE/wp-json/wp/v2/media/$id"), nonce)
                    .post(payload.toRequestBody(JSON_TYPE))
                    .build()
            }
            if (reply.code !in 200..299) throw EditorException("متن جایگزین (alt) تصویر شاخص ذخیره نشد")
            Result.success(Unit)
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("متن جایگزین (alt) تصویر شاخص ذخیره نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    // ───────────── دسته‌ها ─────────────

    suspend fun categories(context: Context): Result<List<WpCategory>> = withContext(Dispatchers.IO) {
        try {
            val reply = send(context) { nonce ->
                authed(
                    Request.Builder().url("$SITE/wp-json/wp/v2/categories?per_page=100&orderby=name&order=asc&_fields=id,name,parent"),
                    nonce
                ).get().build()
            }
            if (reply.code !in 200..299) throw EditorException(explain("categories", reply, upload = false))
            val list = JsonParser.parseString(reply.body).asJsonArray.map {
                val o = it.asJsonObject
                WpCategory(
                    id = o.get("id").asInt,
                    name = HtmlCompat.fromHtml(o.get("name").asString, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                    parent = o.get("parent")?.asInt ?: 0
                )
            }
            Result.success(list)
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("دسته‌ها گرفته نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    // ───────────── آپلود تصویر ─────────────

    suspend fun uploadImage(context: Context, uri: Uri): Result<UploadedMedia> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "image/jpeg"
            if (!mime.startsWith("image/")) throw EditorException("فقط تصویر می‌تونی انتخاب کنی.")
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw EditorException("فایل خونده نشد.")
            if (bytes.size > 30 * 1024 * 1024) throw EditorException("حجم تصویر بیشتر از ۳۰ مگابایته.")
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
            val name = "appdony-${System.currentTimeMillis()}.$ext"

            val reply = send(context) { nonce ->
                authed(Request.Builder().url("$SITE/wp-json/wp/v2/media"), nonce)
                    .header("Content-Disposition", "attachment; filename=\"$name\"")
                    .post(bytes.toRequestBody(mime.toMediaType()))
                    .build()
            }
            if (reply.code !in 200..299) throw EditorException(explain("media", reply, upload = true))

            val obj = JsonParser.parseString(reply.body).asJsonObject
            Result.success(UploadedMedia(obj.get("id").asLong, obj.get("source_url")?.asString.orEmpty()))
        } catch (e: EditorException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(EditorException("آپلود نشد: ${e.message ?: "خطای ناشناخته"}"))
        }
    }

    // ───────────── شبکه و نانس ─────────────

    private fun authed(builder: Request.Builder, nonce: String): Request.Builder {
        builder.header("User-Agent", UA).header("Accept", "application/json")
        if (nonce.isNotBlank()) builder.header("X-WP-Nonce", nonce)
        return builder
    }

    private fun http(): OkHttpClient {
        val jar = ApiClient.cookieJarInstance
            ?: throw EditorException("اپ هنوز آماده نیست. دوباره امتحان کن.")
        return OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    private fun exec(client: OkHttpClient, request: Request): Reply = try {
        client.newCall(request).execute().use { Reply(it.code, it.body?.string().orEmpty()) }
    } catch (e: IOException) {
        throw EditorException("اینترنت قطعه یا سایت جواب نداد. دوباره امتحان کن.")
    }

    private fun stale(reply: Reply): Boolean =
        reply.code == 401 || reply.body.contains("rest_cookie_invalid_nonce")

    private fun fetchNonce(client: OkHttpClient): String = try {
        client.newCall(Request.Builder().url(NONCE_URL).header("User-Agent", UA).get().build())
            .execute().use { resp ->
                val text = resp.body?.string()?.trim().orEmpty()
                if (resp.isSuccessful && NONCE_REGEX.matches(text)) text else ""
            }
    } catch (e: Exception) {
        ""
    }

    private suspend fun send(context: Context, build: (String) -> Request): Reply {
        val session = SessionManager(context)
        val client = http()
        val original = session.nonce.first()
        var nonce = original.ifBlank { fetchNonce(client) }
        var reply = exec(client, build(nonce))

        if (stale(reply)) {
            val fresh = fetchNonce(client)
            if (fresh.isNotBlank()) {
                nonce = fresh
                reply = exec(client, build(nonce))
            }
        }
        if (stale(reply)) {
            val credentials = CredentialsManager(context)
            val username = credentials.getUsername()
            val password = credentials.getPassword()
            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                val relogged = WordPressLogin.login(username, password).getOrNull()
                if (relogged != null) {
                    nonce = relogged.nonce
                    reply = exec(client, build(nonce))
                }
            }
        }
        if (nonce.isNotBlank() && nonce != original) session.saveNonce(nonce)
        return reply
    }

    // ───────────── پیام خطا ─────────────

    private fun explain(kind: String, reply: Reply, upload: Boolean): String {
        val obj = runCatching { JsonParser.parseString(reply.body).asJsonObject }.getOrNull()
        val code = obj?.get("code")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        val message = obj?.get("message")?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
        return when {
            stale(reply) -> "نشستت منقضی شده؛ یه بار از حساب خارج شو و دوباره وارد شو."
            code == "rest_no_route" && kind == "blog" ->
                "نوع «وبلاگ» روی سایت برای REST فعال نیست (show_in_rest). باید توی قالب playclone فعالش کنیم."
            code == "rest_no_route" -> "مسیر ذخیره روی سایت پیدا نشد."
            code == "rest_post_invalid_id" -> "این مطلب روی سایت پیدا نشد (شاید پاک شده)."
            reply.code == 403 && upload -> "اجازه‌ی آپلود تصویر رو نداری."
            reply.code == 403 -> "اجازه‌ی این کار رو نداری."
            reply.code == 413 -> "حجم درخواست زیاده."
            reply.code in 500..599 -> "سایت خطا داد (${reply.code}). کمی بعد دوباره امتحان کن."
            message.isNotBlank() -> message
            else -> "انجام نشد (کد ${reply.code})."
        }
    }
}
