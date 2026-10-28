package com.appdony.app.data.api

import android.text.Html
import android.webkit.CookieManager
import com.appdony.app.data.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

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

    suspend fun login(username: String, password: String): Result<UserData> =
        withContext(Dispatchers.IO) {
            try {
                // از همان کوکی‌جار اصلی اپ استفاده می‌کنیم تا بقیه‌ی درخواست‌ها هم لاگین بمانند
                val jar = ApiClient.cookieJarInstance
                    ?: return@withContext Result.failure(Exception("اپ هنوز آماده نیست. دوباره امتحان کن."))

                jar.clear()

                val client = OkHttpClient.Builder()
                    .cookieJar(jar)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()
                val noRedirectClient = client.newBuilder().followRedirects(false).build()

                // ۱) گرفتن کوکی تستی وردپرس
                client.newCall(
                    Request.Builder().url(LOGIN_URL).header("User-Agent", UA).get().build()
                ).execute().use { it.body?.string() }

                // ۲) ارسال فرم ورود (ریدایرکت دنبال نمی‌شود)
                val form = FormBody.Builder()
                    .add("log", username)
                    .add("pwd", password)
                    .add("wp-submit", "Log In")
                    .add("redirect_to", ADMIN_URL)
                    .add("testcookie", "1")
                    .build()

                val responseText = noRedirectClient.newCall(
                    Request.Builder()
                        .url(LOGIN_URL)
                        .header("User-Agent", UA)
                        .header("Referer", LOGIN_URL)
                        .header("Origin", SITE)
                        .post(form)
                        .build()
                ).execute().use { it.body?.string().orEmpty() }

                // ۳) ورود موفق یعنی کوکی wordpress_logged_in ساخته شده
                val loggedIn = jar.loadForRequest(LOGIN_URL.toHttpUrl())
                    .any { it.name.startsWith("wordpress_logged_in") }

                if (!loggedIn) {
                    return@withContext Result.failure(Exception(extractLoginError(responseText)))
                }

                // ۴) توکن REST (برای ارسال کامنت و اطلاعات کاربر)
                val nonce = fetchNonce(client)

                // ۵) اطلاعات کاربر
                val userRequest = Request.Builder()
                    .url(API_USER_URL)
                    .header("User-Agent", UA)
                    .apply { if (nonce.isNotBlank()) header("X-WP-Nonce", nonce) }
                    .get()
                    .build()

                val userJson = try {
                    client.newCall(userRequest).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string() else null
                    }
                } catch (e: Exception) {
                    null
                }

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
                Result.failure(Exception("اتصال به اینترنت برقرار نیست."))
            } catch (e: SocketTimeoutException) {
                Result.failure(Exception("پاسخ سایت دیر رسید. دوباره امتحان کن."))
            } catch (e: Exception) {
                Result.failure(Exception("خطا در اتصال: ${e.message}"))
            }
        }

    private fun fetchNonce(client: OkHttpClient): String {
        try {
            client.newCall(
                Request.Builder().url(NONCE_URL).header("User-Agent", UA).get().build()
            ).execute().use { resp ->
                val text = resp.body?.string()?.trim().orEmpty()
                if (resp.isSuccessful && NONCE_REGEX.matches(text)) return text
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
                if (match != null) return match.groupValues[1]
            }
        } catch (e: Exception) {
            // بدون توکن ادامه می‌دهیم
        }
        return ""
    }

    private fun syncCookiesToWebView(cookies: List<okhttp3.Cookie>) {
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

    private fun extractLoginError(html: String): String {
        val raw = ERROR_REGEX.find(html)?.groupValues?.getOrNull(1)
        if (!raw.isNullOrBlank()) {
            val clean = Html.fromHtml(raw, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (clean.isNotBlank()) return clean
        }
        return "ورود انجام نشد. نام کاربری (یا ایمیل) و رمز عبور را بررسی کن."
    }
}
