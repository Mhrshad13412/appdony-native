package com.appdony.app.data.api

import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.google.gson.annotations.SerializedName
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

const val LIST_FIELDS = "id,title,excerpt,date,modified,content,categories,featured_media,_links,_embedded"
// فقط تصویر شاخص و دسته؛ نویسنده/پاسخ‌ها و ... دیگه دانلود نمی‌شن
const val EMBED_LIGHT = "wp:featuredmedia,wp:term"

interface AppdonyApi {
    @GET("wp-json/wp/v2/users/me?context=edit")
    suspend fun getCurrentUser(): UserData

    @GET("wp-json/wp/v2/blog")
    suspend fun getCustomBlogPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: String = EMBED_LIGHT
    ): List<Post>

    @GET("wp-json/wp/v2/blog/{id}")
    suspend fun getBlogPost(
        @Path("id") id: Int,
        @Query("_embed") embed: String = EMBED_LIGHT
    ): Post

    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: String = EMBED_LIGHT,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts/{id}")
    suspend fun getPost(
        @Path("id") id: Int,
        @Query("_embed") embed: String = EMBED_LIGHT
    ): Post

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByCategory(
        @Query("categories") categoryId: Int,
        @Query("per_page") perPage: Int = 6,
        @Query("page") page: Int = 1,
        @Query("orderby") orderBy: String = "date",
        @Query("order") order: String = "desc",
        @Query("_embed") embed: String = EMBED_LIGHT,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByIds(
        @Query("include") ids: String,
        @Query("per_page") perPage: Int = 50,
        @Query("_embed") embed: String = EMBED_LIGHT,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun searchPosts(
        @Query("search") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("_embed") embed: String = EMBED_LIGHT,
        @Query("_fields") fields: String = LIST_FIELDS
    ): List<Post>

    @GET("wp-json/wp/v2/categories")
    suspend fun getCategories(
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc",
        @Query("_fields") fields: String = "id,name,slug,count,parent"
    ): List<Category>

    @GET("wp-json/wp/v2/comments")
    suspend fun getComments(
        @Query("post") postId: Int,
        @Query("per_page") perPage: Int = 50
    ): List<Comment>

    @POST("wp-json/wp/v2/comments")
    suspend fun postComment(
        @Header("X-WP-Nonce") nonce: String,
        @Body body: CommentRequest
    ): Comment
}
