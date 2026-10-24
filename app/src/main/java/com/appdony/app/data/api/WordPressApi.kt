package com.appdony.app.data.api

import com.appdony.app.data.model.Post
import retrofit2.http.GET
import retrofit2.http.Query

interface WordPressApi {
    // اضافه شدن پارامترهای page و search برای اسکرول بی‌نهایت و تفکیک دسته‌ها
    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = 10,
        @Query("search") search: String? = null,
        @Query("_embed") embed: Boolean = true
    ): List<Post>
}
