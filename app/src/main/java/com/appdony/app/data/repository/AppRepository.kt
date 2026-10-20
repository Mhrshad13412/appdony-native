package com.appdony.app.data.repository

import android.content.Context
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HomeData(
    val banner: Post? = null,
    val singleFeature: Post? = null,
    val featured: List<Post> = emptyList()
)

class AppRepository(private val context: Context? = null) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1)) // 👈 رفع خطای Required SETTINGS preface در وبلاگ
            .build()
    }

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"
    private val gson = Gson()

    suspend fun loadInitial(page: String): Result<HomeData> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("${baseUrl}posts?per_page=10").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val type = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, type)
                
                val homeData = HomeData(
                    banner = posts.firstOrNull(),
                    singleFeature = posts.getOrNull(1),
                    featured = posts.drop(2)
                )
                Result.success(homeData)
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
                val type = object : TypeToken<List<Post>>() {}.type
                val posts: List<Post> = gson.fromJson(body, type)
                Result.success(posts)
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
                
            if (nonce.isNotBlank()) {
                requestBuilder.addHeader("X-WP-Nonce", nonce)
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }
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
