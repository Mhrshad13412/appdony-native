package com.appdony.app.data.api

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** ثبت‌نام با فرم استاندارد وردپرس (wp-login.php?action=register) */
object WordPressRegister {

    private const val SITE = "https://appdony.ir"
    private const val REGISTER_URL = "$SITE/wp-login.php?action=register"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val FORM_REGEX = Regex("<form[\\s\\S]*?</form>", RegexOption.IGNORE_CASE)
    private val INPUT_REGEX = Regex("<input\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ACTION_REGEX =
        Regex("<form[^>]*\\baction\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
    private val ERROR_REGEX =
        Regex("<div id=\"login_error\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)

    // کوکی‌جار جدا (به سشن ورود اپ دست نمی‌زنه)
    private class MemoryJar : CookieJar {
        private val store = mutableListOf<Cookie>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            synchronized(store) {
                store.removeAll { old -> cookies.any { it.name == old.name } }
                store.addAll(cookies)
            }
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(store) { store.toList() }
    }

    private fun fail(message: String, trace: StringBuilder): Result<Unit> =
        Result.failure(LoginException(message, trace.toString()))

    private fun attr(tag: String, name: String): String? =
        Regex("\\b$name\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.getOrNull(1)

    private fun decode(text: String): String =
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()

    suspend fun register(username: String, email: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val trace = StringBuilder()
            fun t(line: String) { trace.appendLine(line) }

            try {
                val client = OkHttpClient.Builder()
                    .cookieJar(MemoryJar())
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val noRedirect = client.newBuilder().followRedirects(false).build()

                // ۱) گرفتن فرم ثبت‌نام
                var pageUrl = REGISTER_URL
                var html = ""
                var getCode = 0
                client.newCall(
                    Request.Builder().url(REGISTER_URL).header("User-Agent", UA).get().build()
                ).execute().use { resp ->
                    getCode = resp.code
                    pageUrl = resp.request.url.toString()
                    html = resp.body?.string().orEmpty()
                }
                t("1) GET فرم ثبت‌نام -> کد $getCode | آدرس نهایی: $pageUrl | حجم: ${html.length}")

                if (pageUrl.contains("registration=disabled")) {
                    return@withContext fail("ثبت‌نام روی سایت بسته است.", trace)
                }

                val formHtml = FORM_REGEX.findAll(html).map { it.value }.firstOrNull {
                    it.contains("name=\"user_login\"") || it.contains("name='user_login'")
                }
                if (formHtml == null) {
                    val lower = html.lowercase()
                    val blocked = (getCode == 403 || getCode == 503 || getCode == 429) &&
                        (lower.contains("just a moment") || lower.contains("cloudflare") || lower.contains("captcha"))
                    val msg = if (blocked) "سایت درخواست را مسدود کرد (محافظ ضدربات). بعداً امتحان کن."
                    else "فرم ثبت‌نام سایت پیدا نشد."
                    return@withContext fail(msg, trace)
                }

                // ۲) خواندن فیلدهای فرم
                val rawAction = ACTION_REGEX.find(formHtml)?.groupValues?.getOrNull(1)
                val action = try {
                    if (rawAction.isNullOrBlank()) pageUrl
                    else pageUrl.toHttpUrl().resolve(decode(rawAction))?.toString() ?: pageUrl
                } catch (e: Exception) {
                    pageUrl
                }

                val hidden = linkedMapOf<String, String>()
                val extras = mutableListOf<String>()
                var submitName = "wp-submit"
                var submitValue = "Register"

                INPUT_REGEX.findAll(formHtml).forEach { match ->
                    val tag = match.value
                    val type = attr(tag, "type")?.lowercase() ?: "text"
                    val name = attr(tag, "name").orEmpty()
                    val value = decode(attr(tag, "value").orEmpty())
                    if (name.isBlank() || name == "user_login" || name == "user_email") return@forEach

                    when (type) {
                        "hidden" -> hidden[name] = value
                        "submit" -> {
                            submitName = name
                            if (value.isNotBlank()) submitValue = value
                        }
                        "checkbox", "radio", "button", "image", "reset" -> Unit
                        else -> extras.add(name)
                    }
                }
                t("   آدرس ارسال=$action | مخفی=${hidden.keys} | فیلد اضافه=$extras | دکمه=$submitName")

                // ۳) ارسال فرم
                val body = FormBody.Builder()
                    .add("user_login", username)
                    .add("user_email", email)
                hidden.forEach { (k, v) -> body.add(k, v) }
                extras.forEach { body.add(it, "") }
                body.add(submitName, submitValue)

                val actionUrl = action.toHttpUrl()
                var postCode = 0
                var location: String? = null
                var postHtml = ""
                noRedirect.newCall(
                    Request.Builder()
                        .url(actionUrl)
                        .header("User-Agent", UA)
                        .header("Referer", pageUrl)
                        .header("Origin", "${actionUrl.scheme}://${actionUrl.host}")
                        .post(body.build())
                        .build()
                ).execute().use { resp ->
                    postCode = resp.code
                    location = resp.header("Location")
                    postHtml = resp.body?.string().orEmpty()
                }
                t("2) POST فرم -> کد $postCode | Location: ${location ?: "-"} | حجم: ${postHtml.length}")

                val loc = location.orEmpty()
                val redirected = postCode in 300..399
                val success = loc.contains("checkemail=registered") ||
                    (redirected && !loc.contains("action=register") && !loc.contains("registration=disabled"))
                if (success) {
                    t("   نتیجه: ثبت‌نام موفق")
                    return@withContext Result.success(Unit)
                }

                val serverError = ERROR_REGEX.find(postHtml)?.groupValues?.getOrNull(1)
                    ?.let { decode(it).replace(Regex("\\s+"), " ").trim() }
                    ?.ifBlank { null }
                t("   نتیجه: ناموفق | پیام سایت: ${serverError ?: "-"}")
                fail(serverError ?: "ثبت‌نام انجام نشد. نام کاربری و ایمیل را بررسی کن.", trace)
            } catch (e: UnknownHostException) {
                fail("اتصال به اینترنت برقرار نیست.", trace.append("UnknownHostException"))
            } catch (e: SocketTimeoutException) {
                fail("پاسخ سایت دیر رسید. دوباره امتحان کن.", trace.append("Timeout"))
            } catch (e: Exception) {
                fail("خطا در اتصال: ${e.message}", trace.append("Exception: ${e::class.java.simpleName} ${e.message}"))
            }
        }
}
