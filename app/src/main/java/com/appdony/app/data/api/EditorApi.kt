package com.appdony.app.data.api

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
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

/** خطایی که پیامش فارسی و آماده‌ی نمایش به کاربره */
class EditorException(message: String) : Exception(message)

/**
 * ذخیره‌ی نوشته/وبلاگ و آپلود تصویر با REST وردپرس. احراز هویت با کوکی‌های نشست + هدر X-WP-Nonce؛
 * اگه نانس منقضی شده بود یه بار تازه می‌گیره و اگه باز نشد با اطلاعات ذخیره‌شده دوباره وارد می‌شه.
 */
object EditorApi {

    private const val SITE = "https://appdony.ir"
    private const val NONCE_URL = "$SITE/wp-admin/admin-ajax.php?action=rest-nonce"
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
     * اگه کاربر اجازه‌ی انتشار نداشت، خودکار «pending» ذخیره می‌شه.
     */
    suspend fun save(
        context: Context,
        kind: String,
        postId: Long?,
        title: String,
        content: String,
        status: String,
        featuredId: Long?
    ): Result<SavedPost> = withContext(Dispatchers.IO) {
        try {
            val base = route(context, kind)
            val url = if (postId == null) base else "$base/$postId"
            var wanted = status
            var note: String? = null

            fun request(nonce: String, st: String): Request =
                authed(Request.Builder().url(url), nonce)
                    .post(body(title, content, st, featuredId, postId != null).toRequestBody(JSON_TYPE))
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

    private fun body(title: String, content: String, status: String, featured: Long?, isUpdate: Boolean): String {
        val o = JsonObject()
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
