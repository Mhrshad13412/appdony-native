package com.appdony.app.data.api

import com.appdony.app.data.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * لاگین مستقیم به وردپرس با OkHttp
 * بدون Retrofit — کنترل کامل روی کوکی‌ها و ریدایرکت‌ها
 */
object WordPressLogin {

    private const val LOGIN_URL = "https://appdony.ir/wp-login.php"
    private const val ADMIN_URL = "https://appdony.ir/wp-admin/"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * ساخت یه OkHttpClient اختصاصی برای لاگین
     * (بدون redirect خودکار، خودمون مدیریت می‌کنیم)
     */
    private fun buildLoginClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(ApiClient.cookieJarInstance
                ?: throw IllegalStateException("ApiClient.init() رو صدا بزن!"))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            // ❌ followRedirects(false) → خودمون مدیریت می‌کنیم
            .followRedirects(false)
            .followSslRedirects(false)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * لاگین به وردپرس
     * 1. GET صفحه لاگین → ست شدن wordpress_test_cookie
     * 2. POST با log, pwd, wp-submit, redirect_to, testcookie
     * 3. اگه پاسخ 302 بود → کوکی چک کن → موفق
     * 4. اگه 200 بود → خطا
     */
    suspend fun login(username: String, password: String): Result<UserData> =
        withContext(Dispatchers.IO) {
            try {
                val client = buildLoginClient()

                // ═══ مرحله ۱: GET صفحه لاگین برای ست کردن کوکی اولیه ═══
                val getRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")
                    .get()
                    .build()

                client.newCall(getRequest).execute().use { response ->
                    response.body?.close()
                }

                // ═══ مرحله ۲: POST با اطلاعات کاربر ═══
                val formBody = FormBody.Builder()
                    .add("log", username)
                    .add("pwd", password)
                    .add("wp-submit", "ورود")
                    .add("redirect_to", ADMIN_URL)
                    .add("testcookie", "1")
                    .build()

                val postRequest = Request.Builder()
                    .url(LOGIN_URL)
                    .header("User-Agent", USER_AGENT)
                    .header("Referer", LOGIN_URL)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "fa-IR,fa;q=0.9,en;q=0.8")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Origin", "https://appdony.ir")
                    .post(formBody)
                    .build()

                val postResponse = client.newCall(postRequest).execute()

                val responseCode = postResponse.code
                val locationHeader = postResponse.header("Location")
                postResponse.body?.close()

                // ═══ مرحله ۳: بررسی نتیجه ═══

                // اگه 302 بود و Location داشت = موفق
                val isSuccess = responseCode == 302 ||
                        (responseCode in 300..399 && locationHeader != null)

                if (isSuccess) {
                    // ═══ مرحله ۴: دریافت اطلاعات کاربر ═══
                    val userData = fetchUserData(client)
                    if (userData != null) {
                        Result.success(userData)
                    } else {
                        Result.failure(Exception("لاگین موفق بود ولی اطلاعات کاربر دریافت نشد"))
                    }
                } else {
                    Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است"))
                }

            } catch (e: Exception) {
                Result.failure(Exception("خطا در اتصال: ${e.message}"))
            }
        }

    /**
     * گرفتن اطلاعات کاربر از REST API (بعد از لاگین)
     */
    private fun fetchUserData(client: OkHttpClient): UserData? {
        return try {
            val request = Request.Builder()
                .url("https://appdony.ir/wp-json/wp/v2/users/me?context=edit")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: return null
                    parseUserFromJson(body)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * پارس ساده JSON بدون Gson (چون می‌خوایم مستقل باشه)
     */
    private fun parseUserFromJson(json: String): UserData? {
        return try {
            val id = Regex(""""id"\s*:\s*(\d+)""")
                .find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val username = Regex(""""username"\s*:\s*"([^"]*)"""")
                .find(json)?.groupValues?.get(1) ?: ""
            val name = Regex(""""name"\s*:\s*"([^"]*)"""")
                .find(json)?.groupValues?.get(1) ?: ""
            val email = Regex(""""email"\s*:\s*"([^"]*)"""")
                .find(json)?.groupValues?.get(1) ?: ""
            val nonce = Regex(""""nonce"\s*:\s*"([^"]*)"""")
                .find(json)?.groupValues?.get(1) ?: ""
            val avatar = Regex(""""96"\s*:\s*"([^"]*)"""")
                .find(json)?.groupValues?.get(1) ?: ""

            if (id > 0 && username.isNotBlank()) {
                UserData(
                    id = id,
                    username = username,
                    name = name,
                    email = email,
                    nonce = nonce,
                    avatarUrls = if (avatar.isNotBlank()) mapOf("96" to avatar) else null
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
                               }
