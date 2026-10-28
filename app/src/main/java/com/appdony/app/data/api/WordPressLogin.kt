package com.appdony.app.data.api

import android.text.Html
import android.webkit.CookieManager
import com.appdony.app.data.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

// خطای ورود همراه با گزارش مرحله‌به‌مرحله (بدون رمز عبور)
class LoginException(message: String, val trace: String) : Exception(message)

object WordPressLogin {

    private const val SITE = "https://appdony.ir"
    private const val LOGIN_URL = "$SITE/wp-login.php"
    private const val ADMIN_URL = "$SITE/wp-admin/"
    private const val NONCE_URL = "$SITE/wp-admin/admin-ajax.php?action=rest-nonce"
    private const val API_USER_URL = "$SITE/wp-json/wp/v2/users/me?context=edit"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val NONCE_REGEX = Regex("^[A-Za-z0-9]{6,20}$")
    private val HOME_NONCE_REGEX = Regex("\"nonce\"\\s*:\\s*\"([A-Za-z0-9]{6,20})\"")
    private val ERROR_REGEX = Regex("<div id=\"login_error\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
    private val FORM_REGEX = Regex("<form[\\s\\S]*?</form>", RegexOption.IGNORE_CASE)
    private val INPUT_REGEX = Regex("<input\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val ACTION_REGEX = Regex("<form[^>]*\\baction\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)

    private class LoginForm(
        val action: String,
        val hidden: Map<String, String>,
        val submitName: String,
        val submitValue: String
    )

    suspend fun login(username: String, password: String): Result<UserData> =
        withContext(Dispatchers.IO) {
            val trace = StringBuilder()
            fun t(line: String) { trace.appendLine(line) }

            try {
                // از همان کوکی‌جار اصلی اپ استفاده می‌کنیم تا بقیه‌ی درخواست‌ها هم لاگین بمانند
                val jar = ApiClient.cookieJarInstance
                    ?: return@withContext Result.failure(LoginException("اپ هنوز آماده نیست. دوباره امتحان کن.", "cookieJar=null"))

                jar.clear()

                val client = OkHttpClient.Builder()
                    .cookieJar(jar)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val noRedirectClient = client.newBuilder().followRedirects(false).build()

                fun cookieNames(): List<String> =
                    jar.loadForRequest(SITE.toHttpUrl()).map { it.name }

                // ۱) گرفتن صفحه‌ی ورود (کوکی تستی + خواندن فرم)
                var pageUrl = LOGIN_URL
                var pageHtml = ""
                var getCode = 0
                client.newCall(
                    Request.Builder().url(LOGIN_URL).header("User-Agent", UA).get().build()
                ).execute().use { resp ->
                    getCode = resp.code
                    pageUrl = resp.request.url.toString()
                    pageHtml = resp.body?.string().orEmpty()
                }
                t("1) GET صفحه ورود -> کد $getCode | آدرس نهایی: $pageUrl | حجم: ${pageHtml.length}")
                t("   کوکی‌ها: ${cookieNames()}")
                t("   نشانه‌ها: ${detectMarkers(pageHtml)}")

                val form = parseLoginForm(pageHtml, pageUrl)
                t("   فرم: آدرس ارسال=${form.action} | فیلدهای مخفی=${form.hidden.keys} | دکمه=${form.submitName}")

                // ۲) ارسال فرم ورود (ریدایرکت دنبال نمی‌شود)
                val body = FormBody.Builder()
                    .add("log", username)
                    .add("pwd", password)
                form.hidden.forEach { (k, v) -> body.add(k, v) }
                if (!form.hidden.containsKey("redirect_to")) body.add("redirect_to", ADMIN_URL)
                if (!form.hidden.containsKey("testcookie")) body.add("testcookie", "1")
                body.add("rememberme", "forever")
                body.add(form.submitName, form.submitValue)

                val actionUrl = form.action.toHttpUrl()
                var postCode = 0
                var location: String? = null
                var postHtml = ""
                noRedirectClient.newCall(
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
                val names = cookieNames()
                t("2) POST فرم -> کد $postCode | Location: ${location ?: "-"} | حجم: ${postHtml.length}")
                t("   کوکی‌ها: $names")

                // ۳) ورود موفق یعنی کوکی wordpress_logged_in ساخته شده
                val loggedIn = names.any { it.startsWith("wordpress_logged_in") }
                if (!loggedIn) {
                    val serverError = extractLoginError(postHtml)
                    val blocked = looksLikeBotBlock(postCode, postHtml)
                    t("   نتیجه: کوکی ورود ساخته نشد | پیام سایت: ${serverError ?: "-"} | نشانه‌ها: ${detectMarkers(postHtml)}")

                    val message = when {
                        blocked -> "سایت درخواست ورود را مسدود کرد (محافظ ضدربات). بعداً امتحان کن."
                        serverError != null -> serverError
                        else -> "ورود انجام نشد. نام کاربری (یا ایمیل) و رمز عبور را بررسی کن."
                    }
                    return@withContext Result.failure(LoginException(message, trace.toString()))
                }
                t("   نتیجه: ورود موفق (کوکی ساخته شد)")

                // ۴) توکن REST (برای ارسال کامنت و اطلاعات کاربر)
                val (nonce, nonceSource) = fetchNonce(client)
                t("3) توکن: طول=${nonce.length} | منبع=$nonceSource")

                // ۵) اطلاعات کاربر
                val userRequest = Request.Builder()
                    .url(API_USER_URL)
                    .header("User-Agent", UA)
                    .apply { if (nonce.isNotBlank()) header("X-WP-Nonce", nonce) }
                    .get()
                    .build()

                var userCode = 0
                val userJson = try {
                    client.newCall(userRequest).execute().use { resp ->
                        userCode = resp.code
                        if (resp.isSuccessful) resp.body?.string() else null
                    }
                } catch (e: Exception) {
                    null
                }
                t("4) اطلاعات کاربر -> کد $userCode")

                val parsed = try {
                    if (userJson.isNullOrBlank()) null else Gson().fromJson(userJson, UserData::class.java)
                } catch (e: Exception) {
                    null
                }

                val user = (parsed ?: UserData(username = username, name = username)).copy(nonce = nonce)

                // ۶) کوکی‌ها را برای بخش‌هایی که از CookieManager اندروید می‌خوانند هم کپی می‌کنیم
                syncCookiesToWebView(jar.loadForRequest(SITE.toHttpUrl()))

                Result.success(user)
            } catch (e: UnknownHostException) {
                Result.failure(LoginException("اتصال به اینترنت برقرار نیست.", trace.toString() + "UnknownHostException"))
            } catch (e: SocketTimeoutException) {
                Result.failure(LoginException("پاسخ سایت دیر رسید. دوباره امتحان کن.", trace.toString() + "Timeout"))
            } catch (e: Exception) {
                Result.failure(LoginException("خطا در اتصال: ${e.message}", trace.toString() + "Exception: ${e::class.java.simpleName} ${e.message}"))
            }
        }

    // ───────────── خواندن فرم ورود ─────────────

    private fun attr(tag: String, name: String): String? {
        val regex = Regex("\\b$name\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE)
        return regex.find(tag)?.groupValues?.getOrNull(1)
    }

    private fun decode(text: String): String =
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()

    private fun parseLoginForm(html: String, pageUrl: String): LoginForm {
        val formHtml = FORM_REGEX.findAll(html)
            .map { it.value }
            .firstOrNull { it.contains("name=\"pwd\"") || it.contains("name='pwd'") }
            .orEmpty()

        val rawAction = ACTION_REGEX.find(formHtml)?.groupValues?.getOrNull(1)
        val action = try {
            if (rawAction.isNullOrBlank()) {
                pageUrl
            } else {
                pageUrl.toHttpUrl().resolve(decode(rawAction))?.toString() ?: pageUrl
            }
        } catch (e: Exception) {
            pageUrl
        }

        val hidden = linkedMapOf<String, String>()
        var submitName = "wp-submit"
        var submitValue = "Log In"

        INPUT_REGEX.findAll(formHtml).forEach { match ->
            val tag = match.value
            val type = attr(tag, "type")?.lowercase()
            val name = attr(tag, "name").orEmpty()
            val value = decode(attr(tag, "value").orEmpty())
            if (name.isBlank()) return@forEach

            when (type) {
                "hidden" -> if (name != "log" && name != "pwd") hidden[name] = value
                "submit" -> {
                    submitName = name
                    if (value.isNotBlank()) submitValue = value
                }
            }
        }

        return LoginForm(action, hidden, submitName, submitValue)
    }

    // ───────────── توکن، کوکی، خطاها ─────────────

    private fun fetchNonce(client: OkHttpClient): Pair<String, String> {
        try {
            client.newCall(
                Request.Builder().url(NONCE_URL).header("User-Agent", UA).get().build()
            ).execute().use { resp ->
                val text = resp.body?.string()?.trim().orEmpty()
                if (resp.isSuccessful && NONCE_REGEX.matches(text)) return Pair(text, "ajax")
            }
        } catch (e: Exception) {
            // ادامه با روش جایگزین
        }

        try {
            client.newCall(
                Request.Builder().url("$SITE/").header("User-Agent", UA).get().build()
            ).execute().use { resp ->
                val html = resp.body?.string().orEmpty()
                val match = HOME_NONCE_REGEX.find(html)
                if (match != null) return Pair(match.groupValues[1], "home")
            }
        } catch (e: Exception) {
            // بدون توکن ادامه می‌دهیم
        }
        return Pair("", "none")
    }

    private fun syncCookiesToWebView(cookies: List<Cookie>) {
        try {
            val manager = CookieManager.getInstance()
            manager.setAcceptCookie(true)
            cookies.forEach { cookie ->
                manager.setCookie(SITE, "${cookie.name}=${cookie.value}; Path=/")
            }
            manager.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractLoginError(html: String): String? {
        val raw = ERROR_REGEX.find(html)?.groupValues?.getOrNull(1) ?: return null
        val clean = decode(raw).replace(Regex("\\s+"), " ").trim()
        return clean.ifBlank { null }
    }

    private fun looksLikeBotBlock(code: Int, html: String): Boolean {
        val lower = html.lowercase()
        return (code == 403 || code == 503 || code == 429) &&
            (lower.contains("just a moment") || lower.contains("cloudflare") ||
                lower.contains("attention required") || lower.contains("captcha"))
    }

    private fun detectMarkers(html: String): List<String> {
        val lower = html.lowercase()
        val keys = listOf(
            "recaptcha", "turnstile", "hcaptcha", "captcha", "just a moment", "cloudflare",
            "wordfence", "login lockdown", "limit login", "otp", "digits", "sms", "telegram", "honeypot"
        )
        return keys.filter { lower.contains(it) }
    }
}
