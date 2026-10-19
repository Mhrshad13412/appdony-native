package com.appdony.app.data.repository

import android.content.Context
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.api.CommentRequest
import com.appdony.app.data.api.WordPressLogin
import com.appdony.app.data.local.AppDatabase
import com.appdony.app.data.local.CachedPost
import com.appdony.app.data.model.*
import com.appdony.app.data.session.CredentialsManager
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AppRepository(context: Context? = null) {

    private val api = ApiClient.api
    private val db = context?.let { AppDatabase.getDatabase(it) }
    private val gson = Gson()
    private val credentialsManager = context?.let { CredentialsManager(it) }

    private val cacheInitial = mutableMapOf<String, InitialData>()

    private fun postToJson(post: Post): String = gson.toJson(post)
    private fun jsonToPost(json: String): Post = gson.fromJson(json, Post::class.java)

    suspend fun clearCache() {
        cacheInitial.clear()
        db?.postDao()?.clearAll()
    }
    
    // 👇 تابع لاگین خودکار با اطلاعات ذخیره شده
    suspend fun autoLogin(): Result<UserData> {
        val user = credentialsManager?.getUsername()
        val pass = credentialsManager?.getPassword()
        if (user.isNullOrBlank() || pass.isNullOrBlank()) return Result.failure(Exception("no saved credentials"))
        return WordPressLogin.login(user, pass)
    }

    suspend fun getCurrentUser(): Result<UserData> = try {
        Result.success(api.getCurrentUser())
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getBlogPosts(page: Int, perPage: Int = 10): Result<List<Post>> = try {
        val posts = api.getCustomBlogPosts(perPage = perPage, page = page)
        Result.success(posts)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPost(id: Int): Result<Post> = try {
        try {
            Result.success(api.getPost(id = id))
        } catch (e: Exception) {
            Result.success(api.getBlogPost(id = id))
        }
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 6, forceRefresh: Boolean = false): Result<List<Post>> {
        return try {
            if (!forceRefresh) {
                val cachedList = db?.postDao()?.getPostsByCategory(categoryId) ?: emptyList()
                if (cachedList.isNotEmpty()) {
                    return Result.success(cachedList.map { jsonToPost(it.jsonData) })
                }
            }
            val posts = api.getPostsByCategory(categoryId = categoryId, perPage = perPage)
            val entities = posts.map { CachedPost(it.id, postToJson(it), categoryId) }
            db?.postDao()?.insertPosts(entities)
            Result.success(posts)
        } catch (e: Exception) {
            val cachedList = db?.postDao()?.getPostsByCategory(categoryId) ?: emptyList()
            if (cachedList.isNotEmpty()) Result.success(cachedList.map { jsonToPost(it.jsonData) })
            else Result.failure(e)
        }
    }

    suspend fun getPostsByCategoryPaged(categoryId: Int, page: Int, perPage: Int = 24, orderBy: String = "date", order: String = "desc"): Result<List<Post>> {
        return try {
            Result.success(api.getPostsByCategory(categoryId = categoryId, perPage = perPage, page = page, orderBy = orderBy, order = order))
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getPostsByIds(ids: List<Int>): Result<List<Post>> = try {
        if (ids.isEmpty()) Result.success(emptyList())
        else Result.success(api.getPostsByIds(ids = ids.joinToString(",")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun searchPosts(query: String): Result<List<Post>> = try {
        Result.success(api.searchPosts(query = query, perPage = 30))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getComments(postId: Int): Result<List<Comment>> = try {
        Result.success(api.getComments(postId = postId, perPage = 50))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun postComment(postId: Int, content: String, nonce: String): Result<Comment> = try {
        val comment = api.postComment(nonce = nonce, body = CommentRequest(post = postId, content = content))
        Result.success(comment)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun loadPlusPosts(): Result<List<Post>> = try {
        val allCategories = api.getCategories(perPage = 100)
        val plusParent = allCategories.firstOrNull {
            it.parent == 0 && (it.name.contains("پلاس") || it.slug.contains("plus", true) || it.name.contains("ویژه") || it.slug.contains("premium", true))
        }
        val posts = if (plusParent != null) {
            val childIds = allCategories.filter { it.parent == plusParent.id }.map { it.id }
            val allIds = (listOf(plusParent.id) + childIds).joinToString(",")
            api.getPostsByMultipleCategories(categories = allIds, perPage = 12)
        } else {
            api.getPosts(perPage = 6)
        }
        Result.success(posts)
    } catch (e: Exception) { Result.failure(e) }

    suspend fun loadInitial(feedType: String, forceRefresh: Boolean = false): Result<InitialData> {
        return try {
            if (!forceRefresh) {
                cacheInitial[feedType]?.let { return Result.success(it) }
            }
            coroutineScope {
                val postsDeferred = async { api.getPosts(perPage = 10, page = 1) }
                val categoriesDeferred = async { api.getCategories(perPage = 100) }
                val posts = postsDeferred.await()
                val allCategories = categoriesDeferred.await()
                val gamesParent = allCategories.firstOrNull { it.parent == 0 && matchesAny(it, listOf("game", "بازی", "games")) }
                val appsParent = allCategories.firstOrNull { it.parent == 0 && matchesAny(it, listOf("app", "application", "برنامه", "اپلیکیشن")) }
                val relevantParent = when (feedType) { "games" -> gamesParent; "apps" -> appsParent; else -> null }
                val sections = if (relevantParent != null) {
                    val children = allCategories.filter { it.parent == relevantParent.id && it.count > 0 }
                    if (children.isNotEmpty()) children.sortedByDescending { it.count }.take(30)
                    else listOf(relevantParent)
                } else {
                    allCategories.filter { it.parent == 0 && it.count > 0 }.sortedByDescending { it.count }.take(25)
                }
                val data = InitialData(
                    banner = posts.getOrNull(0), singleFeature = posts.getOrNull(1),
                    featured = posts.take(6), categories = sections
                )
                cacheInitial[feedType] = data
                Result.success(data)
            }
        } catch (e: Exception) { Result.failure(e) }
    }
}

private fun matchesAny(cat: Category, keywords: List<String>): Boolean {
    val name = cat.name.lowercase()
    val slug = cat.slug.lowercase()
    return keywords.any { name.contains(it.lowercase()) || slug.contains(it.lowercase()) }
}
