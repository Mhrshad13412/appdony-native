package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.api.CommentRequest
import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.InitialData
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AppRepository {

    private val api = ApiClient.api

    // ══════════════════ لاگین ══════════════════

    suspend fun login(username: String, password: String): Result<UserData> = try {
        // ۱. ارسال درخواست لاگین به wp-login.php
        val response = api.login(username = username, password = password)

        // ۲. کد 302 یعنی ریدایرکت به پنل = موفق
        // کد 200 یعنی فرم دوباره نمایش داده شده = خطا
        if (response.code() == 302 || response.code() == 200 && response.isSuccessful) {
            // ۳. حالا با کوکی، اطلاعات کاربر رو بگیر
            val user = api.getCurrentUser()
            if (user.id > 0 && user.username.isNotBlank()) {
                Result.success(user)
            } else {
                Result.failure(Exception("لاگین ناموفق — اطلاعات کاربر دریافت نشد"))
            }
        } else {
            Result.failure(Exception("نام کاربری یا رمز عبور اشتباه است"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun checkAuth(): Boolean = try {
        val response = api.checkAuth()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }

    // ══════════════════ پست‌ها ══════════════════

    suspend fun getPost(id: Int): Result<Post> = try {
        Result.success(api.getPost(id = id))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPostsByIds(ids: List<Int>): Result<List<Post>> = try {
        if (ids.isEmpty()) Result.success(emptyList())
        else Result.success(api.getPostsByIds(ids = ids.joinToString(",")))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 10): Result<List<Post>> = try {
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

    // ══════════════════ کامنت‌ها ══════════════════

    suspend fun getComments(postId: Int): Result<List<Comment>> = try {
        Result.success(api.getComments(postId = postId, perPage = 50))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun postComment(
        postId: Int,
        content: String,
        nonce: String
    ): Result<Comment> = try {
        val comment = api.postComment(
            nonce = nonce,
            body = CommentRequest(post = postId, content = content)
        )
        Result.success(comment)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ══════════════════ اپدونی پلاس ══════════════════

    suspend fun loadPlusPosts(): Result<List<Post>> = try {
        val allCategories = api.getCategories(perPage = 100)

        val plusParent = allCategories.firstOrNull {
            it.parent == 0 && (
                it.name.contains("پلاس") ||
                it.slug.contains("plus", ignoreCase = true) ||
                it.name.contains("ویژه") ||
                it.name.contains("کیفیت بالا") ||
                it.slug.contains("premium", ignoreCase = true)
            )
        }

        val posts = if (plusParent != null) {
            val childIds = allCategories.filter { it.parent == plusParent.id }.map { it.id }
            val allIds = (listOf(plusParent.id) + childIds).joinToString(",")
            api.getPostsByMultipleCategories(categories = allIds, perPage = 12)
        } else {
            api.getPosts(perPage = 6)
        }

        Result.success(posts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ══════════════════ لود اولیه ══════════════════

    suspend fun loadInitial(feedType: String): Result<InitialData> = try {
        coroutineScope {
            val postsDeferred = async { api.getPosts(perPage = 10, page = 1) }
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
