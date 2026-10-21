package com.appdony.app.data.repository

import android.content.Context
import android.webkit.CookieManager
import com.appdony.app.data.api.WordPressLogin
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
        .protocols(listOf(Protocol.HTTP_1_1))
        .build()

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"
    private val gson = Gson()

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

    suspend fun loadInitial(page: String): Result<InitialData> = withContext(Dispatchers.IO) {
        try {
            val categorySlug = if (page == "games") "game" else "app"
            val catId = getCategoryId(categorySlug)
            val plusCatId = getCategoryId("appdony-plus")

            val urlBuilder = StringBuilder("${baseUrl}posts?per_page=10&_embed")
            if (catId != null) urlBuilder.append("&categories=$catId")
            if (plusCatId != null) urlBuilder.append("&categories_exclude=$plusCatId")

            val request = Request.Builder().url(urlBuilder.toString()).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val type = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, type)
                
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

    suspend fun loadPlusPosts(page: Int, perPage: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val plusCatId = getCategoryId("appdony-plus")
            if (plusCatId == null) {
                return@withContext Result.success(emptyList())
            }
            
            val request = Request.Builder()
                .url("${baseUrl}posts?categories=$plusCatId&page=$page&per_page=$perPage&_embed")
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

    suspend fun getBlogPosts(page: Int, perPage: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
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

    suspend fun searchPosts(query: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?search=$query&per_page=20&_embed").build()
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

    suspend fun getPostsByIds(ids: List<Int>): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            if (ids.isEmpty()) return@withContext Result.success(emptyList())
            val idsString = ids.joinToString(",")
            val request = Request.Builder().url("${baseUrl}posts?include=$idsString&_embed").build()
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

    fun clearCache() {
        try {
            client.cache?.evictAll()
        } catch (e: Exception) {
            // ignore
        }
    }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?categories=$categoryId&per_page=$perPage&_embed").build()
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

    suspend fun getPostsByCategoryPaged(
        categoryId: Int,
        page: Int,
        perPage: Int,
        orderBy: String = "date",
        order: String = "desc"
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${baseUrl}posts?categories=$categoryId&page=$page&per_page=$perPage&orderby=$orderBy&order=$order&_embed")
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

    suspend fun getPost(postId: Int): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts/$postId?_embed").build()
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

    suspend fun getComments(postId: Int): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}comments?post=$postId&per_page=50").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val type = object : TypeToken<List<Comment>>() {}.type
                val comments: List<Comment> = gson.fromJson(body, type)
                Result.success(comments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun postComment(postId: Int, content: String, nonce: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("post", postId.toString())
                .add("content", content)
                .build()
            val request = Request.Builder()
                .url("${baseUrl}comments")
                .addHeader("X-WP-Nonce", nonce)
                .post(formBody)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val comment: Comment = gson.fromJson(body, Comment::class.java)
                Result.success(comment)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autoLogin(): Result<UserData> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(Exception("Context is null"))
        val credentials = CredentialsManager(ctx)
        val username = credentials.getUsername() ?: return@withContext Result.failure(Exception("No saved credentials"))
        val password = credentials.getPassword() ?: return@withContext Result.failure(Exception("No saved credentials"))

        // استفاده از WordPressLogin برای همگام‌سازی درست کوکی‌ها
        val loginResult = WordPressLogin.login(username, password)
        loginResult.onSuccess { user ->
            // بعد از لاگین موفق، کوکی‌ها از قبل در CookieManager ذخیره شدن
            // حالا کاربر رو با همون کوکی‌ها می‌گیریم
            val cookies = CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
            getCurrentUser(cookies, user.nonce).getOrThrow()
        }
    }

    suspend fun getCurrentUser(cookies: String = "", nonce: String = ""): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            val cookieHeader = cookies.ifBlank {
                CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
            }
            val requestBuilder = Request.Builder()
                .url("${baseUrl}users/me?context=edit") // context=edit برای گرفتن nonce جدید
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
