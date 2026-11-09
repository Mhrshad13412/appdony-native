package com.appdony.app.data.api

import com.appdony.app.data.model.UserData
import com.appdony.app.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AccountException(message: String) : Exception(message)

/**
 * تنظیمات حساب وردپرس (نام، ایمیل، درباره‌ی من) از داخل برنامه.
 * با همون کوکی‌ها و نانسِ ورود کار می‌کنه؛ رمز عبور و عکس پروفایل از خود سایت عوض می‌شن.
 */
object WordPressAccount {

    private const val SITE = "https://appdony.ir"
    private const val NONCE_URL = "$SITE/wp-admin/admin-ajax.php?action=rest-nonce"
    private const val USER_URL = "$SITE/wp-json/wp/v2/users/me?context=edit"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    private val NONCE_REGEX = Regex("^[A-Za-z0-9]{6,20}$")

    private fun buildClient(): OkHttpClient? {
        val jar = ApiClient.cookieJarInstance ?: return null
        return OkHttpClient.Builder()
            .cookieJar(jar)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    private fun fetchNonce(client: OkHttpClient): String {
        return try {
            client.newCall(Request.Builder().url(NONCE_URL).header("User-Agent", UA).get().build())
                .execute().use { resp ->
                    val text = resp.body?.string()?.trim().orEmpty()
                    if (resp.isSuccessful && NONCE_REGEX.matches(text)) text else ""
                }
        } catch (e: Exception) {
            ""
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        val message = try {
            JSONObject(body).optString("message")
        } catch (e: Exception) {
            ""
        }
        return message.ifBlank { "خطا در ارتباط با سایت (کد $code)" }
    }

    /** اطلاعات حساب رو از وردپرس می‌گیره و نام، ایمیل و عکس پروفایل رو توی نشست تازه می‌کنه. */
    suspend fun fetch(session: SessionManager): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            if (!session.isLoggedIn.first()) {
                return@withContext Result.failure(AccountException("وارد حساب نشدی."))
            }
            val client = buildClient()
                ?: return@withContext Result.failure(AccountException("اپ هنوز آماده نیست. دوباره امتحان کن."))
            val nonce = fetchNonce(client).ifBlank { session.nonce.first() }

            val request = Request.Builder()
                .url(USER_URL)
                .header("User-Agent", UA)
                .header("Cache-Control", "no-cache")
                .apply { if (nonce.isNotBlank()) header("X-WP-Nonce", nonce) }
                .get()
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(AccountException(errorMessage(body, resp.code)))
                }
                val user = Gson().fromJson(body, UserData::class.java)
                    ?: return@withContext Result.failure(AccountException("اطلاعات حساب خونده نشد."))
                if (nonce.isNotBlank()) session.saveNonce(nonce)
                session.saveProfile(user.displayName(), user.email, user.avatarUrl().orEmpty())
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(AccountException("اتصال به سایت برقرار نشد."))
        }
    }

    /** نام نمایشی، نام، نام خانوادگی، ایمیل و درباره‌ی من رو توی وردپرس ذخیره می‌کنه. */
    suspend fun update(
        session: SessionManager,
        name: String,
        firstName: String,
        lastName: String,
        email: String,
        description: String
    ): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            if (!session.isLoggedIn.first()) {
                return@withContext Result.failure(AccountException("وارد حساب نشدی."))
            }
            val client = buildClient()
                ?: return@withContext Result.failure(AccountException("اپ هنوز آماده نیست. دوباره امتحان کن."))
            val nonce = fetchNonce(client).ifBlank { session.nonce.first() }
            if (nonce.isBlank()) {
                return@withContext Result.failure(AccountException("نشست منقضی شده؛ یک بار خارج و دوباره وارد شو."))
            }

            val json = JSONObject()
                .put("name", name)
                .put("first_name", firstName)
                .put("last_name", lastName)
                .put("email", email)
                .put("description", description)

            val request = Request.Builder()
                .url(USER_URL)
                .header("User-Agent", UA)
                .header("X-WP-Nonce", nonce)
                .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(AccountException(errorMessage(body, resp.code)))
                }
                val user = Gson().fromJson(body, UserData::class.java)
                    ?: return@withContext Result.failure(AccountException("پاسخ سایت خونده نشد."))
                session.saveNonce(nonce)
                session.saveProfile(user.displayName(), user.email, user.avatarUrl().orEmpty())
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(AccountException("اتصال به سایت برقرار نشد."))
        }
    }
}
