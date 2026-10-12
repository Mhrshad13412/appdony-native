package com.appdony.app.data.api

import com.appdony.app.data.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface AppdonyApi {

    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Int = 1
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByCategory(
        @Query("categories") categoryId: Int,
        @Query("per_page") perPage: Int = 10,
        @Query("_embed") embed: Int = 1
    ): List<Post>
}
