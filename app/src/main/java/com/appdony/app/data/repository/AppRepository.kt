package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.HomeData
import com.appdony.app.data.model.HomeSection
import com.appdony.app.data.model.Post

class AppRepository {

    private val api = ApiClient.api

    suspend fun getLatestPosts(perPage: Int = 20): Result<List<Post>> = try {
        Result.success(api.getPosts(perPage = perPage))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPostsByCategory(categoryId: Int, perPage: Int = 20): Result<List<Post>> = try {
        Result.success(api.getPostsByCategory(categoryId = categoryId, perPage = perPage))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun loadHome(): Result<HomeData> = try {
        // 1. آخرین پست‌ها برای بنر
        val latest = api.getPosts(perPage = 5)

        // 2. دسته‌بندی‌های اصلی (فقط اونایی که پست دارن)
        val categories = api.getCategories(perPage = 12)
            .filter { it.count > 0 && it.parent == 0 }
            .take(8)

        // 3. برای هر دسته، پست‌هاش رو بگیر
        val sections = categories.mapNotNull { cat ->
            try {
                val posts = api.getPostsByCategory(categoryId = cat.id, perPage = 10)
                if (posts.isEmpty()) null
                else HomeSection(
                    categoryId = cat.id,
                    title = cat.name,
                    posts = posts
                )
            } catch (e: Exception) {
                null
            }
        }

        Result.success(
            HomeData(
                banner = latest.getOrNull(0),
                singleFeature = latest.getOrNull(1),
                sections = sections
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}
