package com.appdony.app.data.api

import android.webkit.CookieManager
import com.appdony.app.data.model.UserData
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object WordPressLogin {
    private const val LOGIN_URL = "https://appdony.ir/wp-login.php"
    private const val ADMIN_URL = "https://appdony.ir/wp-admin/"
    private const val API_USER_URL = "https://appdony.ir/wp-json/wp/v2/users/me?context=edit"

    suspend fun login(username: String, password: String): Result<UserData> =
        withContext(Dispatchers.IO) {
            try {
                // 👈 همگام‌سازی مستقیم با CookieManager اندروید
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie("https://appdony.ir") ?: ""

                val client = OkHttpClient.Builder()
                    .cookieJar(object : okhttp3.CookieJar {
                        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                            return cookies.split("; ").mapNotNull { pair ->
                                val parts = pair.split("=", limit = 2)
                                if (parts.size == 2) {
                                    okhttp3.Cookie.Builder()
                                        .domain(url.host)
                                        .path("/")
                                        .name(parts[0])
                                        .value(parts[1])
                                        .build()
                                } else null
                            }
                        }
                        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) {}
                    })
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build()

                // ۱. درخواست GET اولیه برای ست کردن کوکی‌ها
                client.newCall(
                    Request.Builder()
                        .url(LOGIN_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .get()
                        .build()
                ).execute().use { it.body?.close() }

                // ۲. ارسال فرم لاگین
                val form = FormBody.Builder()
                    .add("log", username)
                    .add("pwd", password)
                    .add("wp-submit", "ورود")
                    .add("redirect_to", ADMIN_URL)
                    .add("testcookie", "1")
                    .build()

                client.newCall(
                    Request.Builder()
                        .url(LOGIN_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .header("Referer", LOGIN_URL)
                        .header("Origin", "https://appdony.ir")
                        .post(form)
                        .build()
                ).execute().use { it.body?.close() }

                // ۳. بررسی موفقیت‌آمیز بودن لاگین با کوکی جدید
                val newCookies = cookieManager.getCookie("https://appdony.ir") ?: ""
                val loggedIn = newCookies.contains("wordpress_logged_in")

                if (!loggedIn) {
                    return@withContext Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است. (توجه: وردپرس به طور پیش‌فرض ورود با ایمیل را قبول نمی‌کند، لطفاً نام کاربری خود را وارد کنید)"))
                }

                // ۴. گرفتن اطلاعات کاربر
                val userJson = client.newCall(
                    Request.Builder()
                        .url(API_USER_URL)
                        .header("Cookie", newCookies)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .get()
                        .build()
                ).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }

                if (userJson.isNullOrBlank()) {
                    return@withContext Result.success(UserData(username = username, name = username))
                }

                val user = Gson().fromJson(userJson, UserData::class.java)
                if (user != null) Result.success(user) else Result.success(UserData(username = username, name = username))
            } catch (e: Exception) {
                Result.failure(Exception("خطا در اتصال: ${e.message}"))
            }
        }
}
