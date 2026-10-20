package com.appdony.app.data.repository

import android.content.Context
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HomeData(
    val banner: Post? = null,
    val singleFeature: Post? = null,
    val featured: List<Post> = emptyList()
)

class AppRepository(private val context: Context? = null) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1)) // رفع خطای شبکه وبلاگ
        .build()

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"

    suspend fun loadInitial(page: String): Result<HomeData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?per_page=10").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonArray = JSONArray(body)
                val posts = mutableListOf<Post>()
                for (i in 0 until jsonArray.length()) {
                    posts.add(Post.fromJson(jsonArray.getJSONObject(i)))
                }
                Result.success(HomeData(posts.getOrNull(0), posts.getOrNull(1), posts.drop(2)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBlogPosts(page: Int, perPage: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?page=$page&per_page=$perPage").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonArray = JSONArray(body)
                val posts = mutableListOf<Post>()
                for (i in 0 until jsonArray.length()) {
                    posts.add(Post.fromJson(jsonArray.getJSONObject(i)))
                }
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategoryPosts(categoryId: Int, page: Int = 1): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?categories=$categoryId&page=$page&per_page=20").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonArray = JSONArray(body)
                val posts = mutableListOf<Post>()
                for (i in 0 until jsonArray.length()) {
                    posts.add(Post.fromJson(jsonArray.getJSONObject(i)))
                }
                Result.success(posts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostDetails(postId: Int): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts/$postId").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(Post.fromJson(JSONObject(body)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: Int): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}comments?post=$postId&per_page=50").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val jsonArray = JSONArray(body)
                val comments = mutableListOf<JSONObject>()
                for (i in 0 until jsonArray.length()) {
                    comments.add(jsonArray.getJSONObject(i))
                }
                Result.success(comments)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: Int, content: String, nonce: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("post", postId)
                put("content", content)
            }
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${baseUrl}comments")
                .addHeader("X-WP-Nonce", nonce)
                .post(requestBody)
                .build()
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(cookies: String = "", nonce: String = ""): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder()
                .url("${baseUrl}users/me")
                .addHeader("Cookie", cookies)
            if (nonce.isNotBlank()) requestBuilder.addHeader("X-WP-Nonce", nonce)
            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val obj = JSONObject(body)
                val user = UserData(
                    id = obj.optInt("id"),
                    username = obj.optString("slug"),
                    name = obj.optString("name"),
                    email = obj.optString("email"),
                    avatarUrl = obj.optJSONObject("avatar_urls")?.optString("96") ?: "",
                    nonce = nonce
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
