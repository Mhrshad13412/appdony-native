package com.appdony.app.data.api

import com.appdony.app.data.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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
                val jar = ApiClient.cookieJarInstance
                    ?: return@withContext Result.failure(Exception("ApiClient.init() صدا زده نشده!"))

                jar.clear()

                val client = OkHttpClient.Builder()
                    .cookieJar(jar)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build()

                // ۱. اول یه درخواست GET بزنیم تا کوکی‌های اولیه ست بشن
                client.newCall(
                    Request.Builder()
                        .url(LOGIN_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .get()
                        .build()
                ).execute().use { it.body?.close() }

                // ۲. حالا فرم لاگین رو با اطلاعات کاربر پر می‌کنیم
                val form = FormBody.Builder()
                    .add("log", username)
                    .add("pwd", password)
                    .add("wp-submit", "ورود")
                    .add("redirect_to", ADMIN_URL)
                    .add("testcookie", "1")
                    .build()

                val postResponse = client.newCall(
                    Request.Builder()
                        .url(LOGIN_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .header("Referer", LOGIN_URL)
                        .header("Origin", "https://appdony.ir")
                        .post(form)
                        .build()
                ).execute()

                val cookies = jar.loadForRequest(LOGIN_URL.toHttpUrl())
                val loggedIn = cookies.any { it.name.startsWith("wordpress_logged_in") }

                if (!loggedIn) {
                    return@withContext Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است. (توجه: وردپرس به طور پیش‌فرض ورود با ایمیل را قبول نمی‌کند، لطفاً نام کاربری خود را وارد کنید)"))
                }

                // ۳. حالا اطلاعات کاربر رو از API بگیریم
                val userJson = client.newCall(
                    Request.Builder()
                        .url(API_USER_URL)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .get()
                        .build()
                ).execute().use { resp ->
                    if (resp.isSuccessful) resp.body?.string() else null
                }

                if (userJson.isNullOrBlank()) {
                    return@withContext Result.success(UserData(username = username, name = username))
                }

                val user = parseUser(userJson)
                if (user != null) Result.success(user)
                else Result.success(UserData(username = username, name = username))

            } catch (e: Exception) {
                Result.failure(Exception("خطا در اتصال: ${e.message}"))
            }
        }

    private fun parseUser(json: String): UserData? {
        val id = Regex(""""id"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val username = Regex(""""username"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""
        val name = Regex(""""name"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""
        val email = Regex(""""email"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""
        val nonce = Regex(""""nonce"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""
        val avatar = Regex(""""96"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.get(1) ?: ""

        return if (id > 0 && username.isNotBlank())
            UserData(
                id = id, username = username, name = name, email = email,
                nonce = nonce,
                avatarUrls = if (avatar.isNotBlank()) mapOf("96" to avatar) else null
            )
        else null
    }
                           }
