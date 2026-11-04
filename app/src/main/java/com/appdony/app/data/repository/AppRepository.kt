package com.appdony.app.data.repository

import android.content.Context
import android.text.Html
import android.webkit.CookieManager
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.api.EMBED_LIGHT
import com.appdony.app.data.api.LIST_FIELDS
import com.appdony.app.data.api.WordPressLogin
import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.appdony.app.data.session.CredentialsManager
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.lang.reflect.Type
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** خطای ارسال نظر همراه با کد HTTP و پیام خود وردپرس */
class CommentException(
    val httpCode: Int,
    val wpCode: String?,
    val serverMessage: String?
) : Exception(serverMessage?.takeIf { it.isNotBlank() } ?: "HTTP $httpCode")

class AppRepository(private val context: Context? = null) {
    // کلاینت مشترک با کش دیسکی و «اول کش، بعد تازه‌سازی»؛ صف شبکه‌ی جدا از تصاویر
    private val client: OkHttpClient by lazy {
        ApiClient.lightClient(30).newBuilder().protocols(listOf(Protocol.HTTP_1_1)).build()
    }

    private val baseUrl = "https://appdony.ir/wp-json/wp/v2/"
    private val gson = Gson()

    // درخواست قابل‌لغو: وقتی کاربر چیز دیگه‌ای جستجو می‌کنه یا صفحه رو می‌بنده، درخواست قبلی همون لحظه قطع می‌شه
    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { try { cancel() } catch (_: Throwable) {} }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { if (cont.isActive) cont.resumeWithException(e) }
            override fun onResponse(call: Call, response: Response) { cont.resume(response) }
        })
    }

    private suspend fun <T> getJson(url: String, type: Type): Result<T> = withContext(Dispatchers.IO) {
        try {
            client.newCall(Request.Builder().url(url).build()).await().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                Result.success(gson.fromJson<T>(body, type))
            }
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }

    private val postListType: Type = object : TypeToken<List<Post>>() {}.type
    private fun enc(v: String) = java.net.URLEncoder.encode(v, "UTF-8")
    private val light = "&_embed=$EMBED_LIGHT&_fields=$LIST_FIELDS"

    private suspend fun getCategoryId(slug: String): Int? =
        getJson<List<Category>>("${baseUrl}categories?slug=${enc(slug)}&_fields=id,name,slug,count,parent", object : TypeToken<List<Category>>() {}.type)
            .getOrNull()?.firstOrNull()?.id

    suspend fun loadInitial(page: String): Result<InitialData> = withContext(Dispatchers.IO) {
        try {
            val catId = getCategoryId(if (page == "games") "game" else "app")
            val plusCatId = getCategoryId("appdony-plus")
            val aiCatId = if (page == "games") getCategoryId("ai") else null
            val excl = listOfNotNull(plusCatId, aiCatId).joinToString(",")
            val url = StringBuilder("${baseUrl}posts?per_page=10$light")
            if (catId != null) url.append("&categories=$catId")
            if (excl.isNotEmpty()) url.append("&categories_exclude=$excl")
            val posts = getJson<List<Post>>(url.toString(), postListType).getOrThrow()
            val categories = getJson<List<Category>>("${baseUrl}categories?per_page=20&_fields=id,name,slug,count,parent", object : TypeToken<List<Category>>() {}.type).getOrThrow()
            Result.success(
                InitialData(
                    banner = posts.firstOrNull(),
                    singleFeature = posts.getOrNull(1),
                    featured = posts.drop(2),
                    categories = categories.filter { it.slug != "ai" || page == "apps" }
                )
            )
        } catch (e: kotlinx.coroutines.CancellationException) { throw e
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getBlogPosts(page: Int, perPage: Int): Result<List<Post>> =
        getJson("${baseUrl}blog?page=$page&per_page=$perPage&_embed=$EMBED_LIGHT", postListType)

    suspend fun getBlogPost(postId: Int): Result<Post> =
        getJson("${baseUrl}blog/$postId?_embed=$EMBED_LIGHT", Post::class.java)

    suspend fun searchPosts(query: String): Result<List<Post>> =
        getJson("${baseUrl}posts?search=${enc(query)}&per_page=15$light", postListType)

    suspend fun getPostsByIds(ids: List<Int>): Result<List<Post>> {
        if (ids.isEmpty()) return Result.success(emptyList())
        return getJson("${baseUrl}posts?include=${ids.joinToString(",")}&per_page=${ids.size.coerceIn(1, 100)}$light", postListType)
    }

    fun clearCache() { try { client.cache?.evictAll() } catch (e: Exception) {} }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int): Result<List<Post>> =
        getJson("${baseUrl}posts?categories=$categoryId&per_page=$perPage$light", postListType)

    suspend fun getPostsByCategoryPaged(categoryId: Int, page: Int, perPage: Int, orderBy: String = "date", order: String = "desc"): Result<List<Post>> =
        getJson("${baseUrl}posts?categories=$categoryId&page=$page&per_page=$perPage&orderby=${enc(orderBy)}&order=${enc(order)}$light", postListType)

    suspend fun getPost(postId: Int): Result<Post> =
        getJson("${baseUrl}posts/$postId?_embed=$EMBED_LIGHT", Post::class.java)

    suspend fun getComments(postId: Int): Result<List<Comment>> =
        getJson("${baseUrl}comments?post=$postId&per_page=50", object : TypeToken<List<Comment>>() {}.type)

    // کوکی‌های ورود: اول از کوکی‌جار اصلی اپ، اگه خالی بود از CookieManager اندروید
    private fun cookieHeader(): String {
        val fromJar = try {
            ApiClient.cookieJarInstance
                ?.loadForRequest("https://appdony.ir/".toHttpUrl())
                ?.joinToString("; ") { "${it.name}=${it.value}" }
                .orEmpty()
        } catch (e: Exception) { "" }
        return fromJar.ifBlank { CookieManager.getInstance().getCookie("https://appdony.ir") ?: "" }
    }

    private fun parseWpError(raw: String): Pair<String?, String?> = try {
        val o = JsonParser.parseString(raw).asJsonObject
        val code = o.get("code")?.takeIf { !it.isJsonNull }?.asString
        val msg = o.get("message")?.takeIf { !it.isJsonNull }?.asString
            ?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() }
        code to msg
    } catch (e: Exception) { null to null }

    /** نانس تازه‌ی REST با کوکی‌های فعلی (نانس قدیمی = خطای ۴۰۳) */
    suspend fun fetchFreshNonce(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://appdony.ir/wp-admin/admin-ajax.php?action=rest-nonce")
                .addHeader("Cookie", cookieHeader())
                .addHeader("Cache-Control", "no-cache")
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string()?.trim().orEmpty()
                if (response.isSuccessful && Regex("^[A-Za-z0-9]{6,20}$").matches(text)) text else null
            }
        } catch (e: Exception) { null }
    }

    suspend fun postComment(postId: Int, content: String, nonce: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder().add("post", postId.toString()).add("content", content).build()
            val request = Request.Builder()
                .url("${baseUrl}comments")
                .addHeader("X-WP-Nonce", nonce)
                .addHeader("Cookie", cookieHeader())
                .post(formBody)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val (wpCode, wpMessage) = parseWpError(raw)
                    return@withContext Result.failure(CommentException(response.code, wpCode, wpMessage))
                }
                if (raw.isBlank()) return@withContext Result.failure(Exception("Empty body"))
                Result.success(gson.fromJson(raw, Comment::class.java))
            }
        } catch (e: Exception) { Result.failure(e) }
    }

    /** ورود دوباره با اطلاعات ذخیره‌شده؛ کاربرِ برگشتی نانس تازه رو داره */
    suspend fun autoLogin(): Result<UserData> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(Exception("Context is null"))
        val credentials = CredentialsManager(ctx)
        val username = credentials.getUsername() ?: return@withContext Result.failure(Exception("No saved credentials"))
        val password = credentials.getPassword() ?: return@withContext Result.failure(Exception("No saved credentials"))
        try {
            WordPressLogin.login(username, password)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(cookies: String = "", nonce: String = ""): Result<UserData> = withContext(Dispatchers.IO) {
        try {
            val cookieHeader = cookies.ifBlank { CookieManager.getInstance().getCookie("https://appdony.ir") ?: "" }
            val requestBuilder = Request.Builder().url("${baseUrl}users/me?context=edit").addHeader("Cookie", cookieHeader)
            if (nonce.isNotBlank()) requestBuilder.addHeader("X-WP-Nonce", nonce)
            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("HTTP ${response.code}"))
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val user: UserData = gson.fromJson(body, UserData::class.java)
                Result.success(user.copy(nonce = nonce))
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}
