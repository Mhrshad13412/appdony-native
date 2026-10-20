package com.appdony.app.data.repository

import android.content.Context
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AppRepository(private val context: Context? = null) {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"

    suspend fun getPosts(categoryId: Int? = null): List<Post> = withContext(Dispatchers.IO) {
        try {
            val url = if (categoryId != null) "${baseUrl}posts?categories=$categoryId&per_page=20" else "${baseUrl}posts?per_page=20"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val jsonArray = JSONArray(body)
                val posts = mutableListOf<Post>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    posts.add(Post.fromJson(obj)) // 👈 فرض بر این است که این متد در دیتاکلاس Post وجود دارد
                }
                posts
            }
        } catch (e: Exception) {
            emptyList()
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
                
                // 👈 سازگار با دیتاکلاس UserData شما
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
