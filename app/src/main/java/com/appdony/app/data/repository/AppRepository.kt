package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.HomeData
import com.appdony.app.data.model.HomeSection
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

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 20): Result<List<Post>> = try {
        Result.success(api.getPostsByCategory(categoryId = categoryId, perPage = perPage))
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * فقط ۳ درخواست موازی → سریع مثل برق!
     * ۱. ۵۰ پست آخر
     * ۲. ۵۰ پست قدیمی‌تر (page=2)
     * ۳. لیست دسته‌بندی‌ها
     */
    suspend fun loadHome(type: String = "all"): Result<HomeData> = try {
        coroutineScope {
            val postsPage1 = async { api.getPosts(perPage = 50, page = 1) }
            val postsPage2 = async { api.getPosts(perPage = 50, page = 2) }
            val categoriesDeferred = async { api.getCategories(perPage = 100) }

            val allPosts = (postsPage1.await() + postsPage2.await()).distinctBy { it.id }
            val allCategories = categoriesDeferred.await()

            // پیدا کردن ID والدها
            val gamesParentId = allCategories.firstOrNull {
                it.parent == 0 && (it.name.contains("بازی") || it.slug.contains("game"))
            }?.id
            val appsParentId = allCategories.firstOrNull {
                it.parent == 0 && (it.name.contains("برنامه") || it.name.contains("اپ") || it.slug.contains("app"))
            }?.id
            val blogParentId = allCategories.firstOrNull {
                it.parent == 0 && (it.name.contains("وبلاگ") || it.name.contains("مقال") || it.slug.contains("blog"))
            }?.id

            val gamesChildIds = allCategories.filter { it.parent == gamesParentId }.map { it.id }.toSet()
            val appsChildIds = allCategories.filter { it.parent == appsParentId }.map { it.id }.toSet()
            val blogChildIds = allCategories.filter { it.parent == blogParentId }.map { it.id }.toSet()

            // فیلتر پست‌ها بر اساس نوع
            val filteredPosts = when (type) {
                "games" -> allPosts.filter { p -> p.categories.any { it in gamesChildIds || it == gamesParentId } }
                "apps" -> allPosts.filter { p -> p.categories.any { it in appsChildIds || it == appsParentId } }
                "blog" -> allPosts.filter { p -> p.categories.any { it in blogChildIds || it == blogParentId } }
                else -> allPosts
            }

            // گروه‌بندی بر اساس دسته اصلی
            val groupKey: (Post) -> Pair<Int, String>? = { post ->
                val term = post.embedded?.terms?.firstOrNull()
                    ?.firstOrNull { it.id != gamesParentId && it.id != appsParentId && it.id != blogParentId }
                    ?: post.embedded?.terms?.firstOrNull()?.firstOrNull()
                term?.let { it.id to it.name }
            }

            val grouped = filteredPosts
                .mapNotNull { p -> groupKey(p)?.let { it to p } }
                .groupBy({ it.first }, { it.second })
                .filter { it.value.size >= 2 }

            val sections = grouped.entries
                .sortedByDescending { it.value.size }
                .take(25)
                .map { (key, posts) ->
                    HomeSection(
                        categoryId = key.first,
                        title = key.second,
                        posts = posts
                    )
                }

            // بنر و ویژگی تکی
            val banner = filteredPosts.getOrNull(0)
            val single = filteredPosts.getOrNull(1)

            Result.success(
                HomeData(
                    banner = banner,
                    singleFeature = single,
                    sections = sections
                )
            )
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
