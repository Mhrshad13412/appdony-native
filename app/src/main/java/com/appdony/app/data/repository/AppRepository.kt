package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.api.CommentRequest
import com.appdony.app.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AppRepository {

    private val api = ApiClient.api

    private val cacheInitial = mutableMapOf<String, InitialData>()
    private val cacheSections = mutableMapOf<Int, List<Post>>()

    suspend fun getCurrentUser(): Result<UserData> = try {
        Result.success(api.getCurrentUser())
    } catch (e: Exception) { Result.failure(e) }

    // گرفتن لیست پست‌های وبلاگ از آدرس اختصاصی
    suspend fun getBlogPosts(page: Int, perPage: Int = 10): Result<List<Post>> = try {
        val posts = api.getCustomBlogPosts(perPage = perPage, page = page)
        Result.success(posts)
    } catch (e: Exception) { 
        Result.failure(e) 
    }

    // 👇 تشخیص هوشمند: اگه پست عادی نبود، از وبلاگ بگیر
    suspend fun getPost(id: Int): Result<Post> = try {
        try {
            Result.success(api.getPost(id = id))
        } catch (e: Exception) {
            // اگه خطای 404 داد (یعنی پست وبلاگه)، از این آدرس بگیر
            Result.success(api.getBlogPost(id = id))
        }
    } catch (e: Exception) { 
        Result.failure(e) 
    }

    suspend fun getPostsByIds(ids: List<Int>): Result<List<Post>> = try {
        if (ids.isEmpty()) Result.success(emptyList())
        else Result.success(api.getPostsByIds(ids = ids.joinToString(",")))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 6): Result<List<Post>> = try {
        cacheSections[categoryId]?.let { return Result.success(it) }
        val posts = api.getPostsByCategory(categoryId = categoryId, perPage = perPage)
        cacheSections[categoryId] = posts
        Result.success(posts)
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

    suspend fun loadInitial(feedType: String): Result<InitialData> = try {
        cacheInitial[feedType]?.let { return Result.success(it) }

        coroutineScope {
            val postsDeferred = async { api.getPosts(perPage = 10, page = 1) }
            val categoriesDeferred = async { api.getCategories(perPage = 100) }

            val posts = postsDeferred.await()
            val allCategories = categoriesDeferred.await()

            val gamesParent = allCategories.firstOrNull { it.parent == 0 && matchesAny(it, listOf("game", "بازی", "games")) }
            val appsParent = allCategories.firstOrNull { it.parent == 0 && matchesAny(it, listOf("app", "application", "برنامه", "اپلیکیشن")) }

            val relevantParent = when (feedType) {
                "games" -> gamesParent
                "apps" -> appsParent
                else -> null
            }

            val sections = if (relevantParent != null) {
                val children = allCategories.filter { it.parent == relevantParent.id && it.count > 0 }
                if (children.isNotEmpty()) children.sortedByDescending { it.count }.take(30)
                else listOf(relevantParent)
            } else {
                allCategories.filter { it.parent == 0 && it.count > 0 }.sortedByDescending { it.count }.take(25)
            }

            val data = InitialData(
                banner = posts.getOrNull(0),
                singleFeature = posts.getOrNull(1),
                featured = posts.take(6),
                categories = sections
            )
            
            cacheInitial[feedType] = data
            Result.success(data)
        }
    } catch (e: Exception) { Result.failure(e) }
}

private fun matchesAny(cat: Category, keywords: List<String>): Boolean {
    val name = cat.name.lowercase()
    val slug = cat.slug.lowercase()
    return keywords.any { name.contains(it.lowercase()) || slug.contains(it.lowercase()) }
}
