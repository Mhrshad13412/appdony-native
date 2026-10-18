package com.appdony.app.data.api

import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class CommentRequest(
    val post: Int,
    val content: String,
    @SerializedName("author_name") val authorName: String? = null
)

// 👇 اینجا بیرون از اینترفیس تعریفش کردیم که خطا نده
private const val LIST_FIELDS = "id,title,excerpt,date,categories,featured_media,_embedded"

interface AppdonyApi {

    // ══════════════════ کاربر ══════════════════
    @GET("wp-json/wp/v2/users/me?context=edit")
    suspend fun getCurrentUser(): UserData

    @GET("wp-json/wp/v2/users/me")
    suspend fun checkAuth(): Response<ResponseBody>

    // ══════════════════ پست‌ها (بهینه‌شده برای سرعت) ══════════════════

    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Int = 1,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts/{id}")
    suspend fun getPost(
        @Path("id") id: Int,
        @Query("_embed") embed: Int = 1
    ): Post

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByCategory(
        @Query("categories") categoryId: Int,
        @Query("per_page") perPage: Int = 6,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Int = 1,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByMultipleCategories(
        @Query("categories") categories: String,
        @Query("per_page") perPage: Int = 6,
        @Query("_embed") embed: Int = 1,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByIds(
        @Query("include") ids: String,
        @Query("per_page") perPage: Int = 50,
        @Query("_embed") embed: Int = 1,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun searchPosts(
        @Query("search") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("_embed") embed: Int = 1,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    // ══════════════════ دسته‌بندی‌ها ══════════════════
    @GET("wp-json/wp/v2/categories")
    suspend fun getCategories(
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc",
        @Query("_fields") fields: String = "id,name,slug,count,parent"
    ): List<Category>

    // ══════════════════ کامنت‌ها ══════════════════
    @GET("wp-json/wp/v2/comments")
    suspend fun getComments(
        @Query("post") postId: Int,
        @Query("per_page") perPage: Int = 50,
        @Query("orderby") orderBy: String = "date",
        @Query("order") order: String = "desc"
    ): List<Comment>

    @POST("wp-json/wp/v2/comments")
    suspend fun postComment(
        @Header("X-WP-Nonce") nonce: String,
        @Body body: CommentRequest
    ): Comment
}
