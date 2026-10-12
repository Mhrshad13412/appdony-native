package com.appdony.app.data.repository

import com.appdony.app.data.api.ApiClient
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
}
