package com.appdony.app.data.api

import android.content.Context
import com.appdony.app.data.model.UserData
import com.appdony.app.data.session.CredentialsManager
import com.appdony.app.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * سطح دسترسی نوشتنِ کاربر واردشده («ایجاد نوشته» و «ایجاد بلاگ»).
 * از قابلیت‌های حساب توی وردپرس (users/me?context=edit) خونده و توی نشست ذخیره می‌شه؛
 * پس کاربرِ عادی هیچ‌وقت این دکمه‌ها رو نمی‌بینه. سرور هم خودش دسترسی رو چک می‌کنه.
 */
object UserAccess {

    private const val SITE = "https://appdony.ir"
    private const val NONCE_URL = "$SITE/wp-admin/admin-ajax.php?action=rest-nonce"
    private const val USER_URL = "$SITE/wp-json/wp/v2/users/me?context=edit"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"
    private val NONCE_REGEX = Regex("^[A-Za-z0-9]{6,20}$")

    private class UserResult(val user: UserData?, val code: Int)

    /** دسترسی رو از سرور می‌گیره و ذخیره می‌کنه. اگه اینترنت نبود هیچ چیزی تغییر نمی‌کنه. */
    suspend fun refresh(context: Context, session: SessionManager) = withContext(Dispatchers.IO) {
        try {
            if (!session.isLoggedIn.first()) return@withContext
            val jar = ApiClient.cookieJarInstance ?: return@withContext
            val client = OkHttpClient.Builder()
                .cookieJar(jar)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .build()

            var nonce = fetchNonce(client).ifBlank { session.nonce.first() }
            var result = fetchUser(client, nonce)

            // فقط وقتی سرور صریحاً «وارد نشدی» گفت (۴۰۱/۴۰۳) ورود خودکار؛ با قطعی اینترنت کوکی‌ها دست نمی‌خورن
            if (result.user == null && (result.code == 401 || result.code == 403)) {
                val credentials = CredentialsManager(context)
                val username = credentials.getUsername()
                val password = credentials.getPassword()
                if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                    val relogged = WordPressLogin.login(username, password).getOrNull()
                    if (relogged != null) {
                        nonce = relogged.nonce
                        result = UserResult(relogged, 200)
                    }
                }
            }

            val user = result.user ?: return@withContext
            if (nonce.isNotBlank()) session.saveNonce(nonce)
            session.saveWriteAccess(user.canWritePosts(), user.canWriteBlog())
            // نام، ایمیل و عکس پروفایل هم از وردپرس تازه می‌شن
            session.saveProfile(user.displayName(), user.email, user.avatarUrl().orEmpty())
        } catch (e: Exception) {
            // بی‌صدا؛ دفعه‌ی بعد دوباره تلاش می‌شه
        }
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

    private fun fetchUser(client: OkHttpClient, nonce: String): UserResult {
        return try {
            val request = Request.Builder()
                .url(USER_URL)
                .header("User-Agent", UA)
                .header("Cache-Control", "no-cache")
                .apply { if (nonce.isNotBlank()) header("X-WP-Nonce", nonce) }
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return UserResult(null, resp.code)
                val body = resp.body?.string().orEmpty()
                val user = if (body.isBlank()) null else Gson().fromJson(body, UserData::class.java)
                UserResult(user, resp.code)
            }
        } catch (e: Exception) {
            UserResult(null, 0)
        }
    }
}
