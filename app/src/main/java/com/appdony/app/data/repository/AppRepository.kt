package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AppRepository {

    private val api = ApiClient.api

    suspend fun getPost(id: Int): Result<Post> = try {
        Result.success(api.getPost(id = id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 15): Result<List<Post>> = try {
        Result.success(api.getPostsByCategory(categoryId = categoryId, perPage = perPage))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPostsByCategoryPaged(
        categoryId: Int,
        page: Int,
        perPage: Int = 24
    ): Result<List<Post>> = try {
        Result.success(
            api.getPostsByCategory(categoryId = categoryId, perPage = perPage, page = page)
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun searchPosts(query: String): Result<List<Post>> = try {
        Result.success(api.searchPosts(query = query, perPage = 30))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getComments(postId: Int): Result<List<Comment>> = try {
        Result.success(api.getComments(postId = postId, perPage = 50))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loadInitial(feedType: String): Result<InitialData> = try {
        coroutineScope {
            val postsDeferred = async { api.getPosts(perPage = 15, page = 1) }
            val categoriesDeferred = async { api.getCategories(perPage = 100) }

            val posts = postsDeferred.await()
            val allCategories = categoriesDeferred.await()

            val gamesParent = allCategories.firstOrNull {
                it.parent == 0 && matchesAny(it, listOf("game", "بازی", "games"))
            }
            val appsParent = allCategories.firstOrNull {
                it.parent == 0 && matchesAny(it, listOf("app", "application", "برنامه", "اپلیکیشن"))
            }
            val blogParent = allCategories.firstOrNull {
                it.parent == 0 && matchesAny(
                    it,
                    listOf("blog", "وبلاگ", "article", "مقاله", "news", "خبر", "اخبار")
                )
            }

            val relevantParent = when (feedType) {
                "games" -> gamesParent
                "apps" -> appsParent
                "blog" -> blogParent
                else -> null
            }

            val sections = if (relevantParent != null) {
                val children = allCategories.filter {
                    it.parent == relevantParent.id && it.count > 0
                }
                if (children.isNotEmpty()) {
                    children.sortedByDescending { it.count }.take(30)
                } else {
                    listOf(relevantParent)
                }
            } else {
                // برای بلاگ: اگه parent پیدا نشد، همه دسته‌ها رو نشون بده
                if (feedType == "blog") {
                    allCategories.filter { it.count > 0 }
                        .sortedByDescending { it.count }
                        .take(20)
                } else {
                    allCategories.filter { it.parent == 0 && it.count > 0 }
                        .sortedByDescending { it.count }
                        .take(25)
                }
            }

            Result.success(
                InitialData(
                    banner = posts.getOrNull(0),
                    singleFeature = posts.getOrNull(1),
                    featured = posts.take(6),
                    categories = sections
                )
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun matchesAny(cat: Category, keywords: List<String>): Boolean {
    val name = cat.name.lowercase()
    val slug = cat.slug.lowercase()
    return keywords.any { name.contains(it.lowercase()) || slug.contains(it.lowercase()) }
}
