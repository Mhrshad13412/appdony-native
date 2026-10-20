package com.appdony.app.data.repository

import android.content.Context
import android.webkit.CookieManager
import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.appdony.app.data.session.CredentialsManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AppRepository(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1)) // رفع خطای Required SETTINGS preface
        .build()

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"
    private val gson = Gson()

    // پیدا کردن ID دسته‌بندی با استفاده از اسلاگ (مثلاً "game" یا "app")
    private suspend fun getCategoryId(slug: String): Int? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}categories?slug=$slug").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val type = object : TypeToken<List<Category>>() {}.type
                val categories: List<Category> = gson.fromJson(body, type)
                categories.firstOrNull()?.id
            }
        } catch (e: Exception) {
            null
        }
    }

    // لود اولیه صفحه اصلی (بازی‌ها یا برنامه‌ها) با فیلتر دسته‌بندی دقیق
    suspend fun loadInitial(page: String): Result<InitialData> = withContext(Dispatchers.IO) {
        try {
            // اسلاگ دسته‌بندی رو بر اساس نوع صفحه انتخاب می‌کنیم
            val categorySlug = if (page == "games") "game" else "app"
            val catId = getCategoryId(categorySlug)

            // اگر ID دسته‌بندی پیدا شد، فقط پست‌های همون دسته رو می‌گیریم
            val url = if (catId != null) {
                "${baseUrl}posts?categories=$catId&per_page=10&_embed"
            } else {
                // اگر پیدا نشد، همه پست‌ها رو می‌گیریم (برای اطمینان)
                "${baseUrl}posts?per_page=10&_embed"
            }

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val type = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, type)

                // همچنین لیست دسته‌بندی‌ها رو برای نمایش در صفحه اصلی می‌گیریم
                val catsReq = Request.Builder().url("${baseUrl}categories?per_page=20").build()
                val catsBody = client.newCall(catsReq).execute().use { resp -> resp.body?.string() ?: "" }
                val catsType = object : TypeToken<List<Category>>() {}.type
                val categories: List<Category> = gson.fromJson(catsBody, catsType)

                Result.success(
                    InitialData(
                        banner = posts.firstOrNull(),
                        singleFeature = posts.getOrNull(1),
                        featured = posts.drop(2),
                        categories = categories
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // دریافت پست‌های وبلاگ از نوع پست سفارشی (Custom Post Type) "blog"
    suspend fun getBlogPosts(page: Int, perPage: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            // آدرس اختصاصی API وبلاگ
            val request = Request.Builder()
                .url("${baseUrl}blog?page=$page&per_page=$perPage&_embed")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val type = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, type)
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // دریافت یک پست وبلاگ خاص با ID
    suspend fun getBlogPost(postId: Int): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}blog/$postId?_embed").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val post: Post = gson.fromJson(body, Post::class.java)
                Result.success(post)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // بقیه متدها (getPostsByCategory, getPost, getComments, postComment و ...) بدون تغییر باقی می‌مونن
    // ...

    // لاگین خودکار (اصلاح شده برای برگرداندن Nonce)
    suspend fun autoLogin(): Result<String> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(Exception("Context is null"))
        val credentials = CredentialsManager(ctx)
        val username = credentials.getUsername() ?: return@withContext Result.failure(Exception("No saved credentials"))
        val password = credentials.getPassword() ?: return@withContext Result.failure(Exception("No saved credentials"))

        try {
            val formBody = FormBody.Builder()
                .add("log", username)
                .add("pwd", password)
                .add("wp-submit", "Log In")
                .add("redirect_to", "https://appdony.ir/wp-admin/")
                .add("testcookie", "1")
                .build()
            val request = Request.Builder()
                .url("https://appdony.ir/wp-login.php")
                .post(formBody)
                .build()
            client.newCall(request).execute().use { response ->
                val setCookies = response.headers("Set-Cookie")
                if (setCookies.isNotEmpty()) {
                    val cookieManager = CookieManager.getInstance()
                    setCookies.forEach { cookie ->
                        cookieManager.setCookie("https://appdony.ir", cookie)
                    }
                    cookieManager.flush()
                    // یه مقدار موقت برمی‌گردونیم، چون Nonce واقعی باید از صفحه لاگین شده استخراج بشه
                    Result.success("logged_in")
                } else {
                    Result.failure(Exception("No cookies received from login"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(cookies: String = "", nonce: String = ""): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            val cookieHeader = cookies.ifBlank {
                CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
            }
            val requestBuilder = Request.Builder()
                .url("${baseUrl}users/me")
                .addHeader("Cookie", cookieHeader)
            if (nonce.isNotBlank()) {
                requestBuilder.addHeader("X-WP-Nonce", nonce)
            }
            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val user: UserData = gson.fromJson(body, UserData::class.java)
                Result.success(user.copy(nonce = nonce))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
