package com.appdony.app.data.api

import com.appdony.app.data.model.Category
import com.appdony.app.data.model.Comment
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.google.gson.annotations.SerializedName
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
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

interface AppdonyApi {

    // ══════════════════ لاگین ══════════════════

    /** مرحله ۱: گرفتن کوکی‌های اولیه */
    @GET("wp-login.php")
    suspend fun getLoginPage(): Response<ResponseBody>

    /** مرحله ۲: ارسال یوزرنیم و پسورد */
    @FormUrlEncoded
    @POST("wp-login.php")
    suspend fun login(
        @Field("log") username: String,
        @Field("pwd") password: String,
        @Field("rememberme") remember: String = "forever",
        @Field("redirect_to") redirect: String = "https://appdony.ir/wp-admin/",
        @Field("testcookie") testCookie: String = "1"
    ): Response<ResponseBody>

    /** گرفتن اطلاعات کاربر لاگین‌شده (شامل nonce) */
    @GET("wp-json/wp/v2/users/me?context=edit")
    suspend fun getCurrentUser(): UserData

    /** چک کردن معتبر بودن کوکی لاگین */
    @GET("wp-json/wp/v2/users/me")
    suspend fun checkAuth(): Response<ResponseBody>

    // ══════════════════ پست‌ها ══════════════════

    @GET("wp-json/wp/v2/posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Int = 1
    ): List<Post>

    /** گرفتن پست‌های وبلاگ با فیلتر دسته‌بندی */
    @GET("wp-json/wp/v2/posts")
    suspend fun getBlogPosts(
        @Query("categories") categoryId: Int,
        @Query("per_page") perPage: Int = 10,
        @Query("_embed") embed: Int = 1
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
        @Query("_embed") embed: Int = 1
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByMultipleCategories(
        @Query("categories") categories: String,
        @Query("per_page") perPage: Int = 6,
        @Query("_embed") embed: Int = 1
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun getPostsByIds(
        @Query("include") ids: String,
        @Query("per_page") perPage: Int = 50,
        @Query("_embed") embed: Int = 1
    ): List<Post>

    @GET("wp-json/wp/v2/posts")
    suspend fun searchPosts(
        @Query("search") query: String,
        @Query("per_page") perPage: Int = 20,
        @Query("_embed") embed: Int = 1
    ): List<Post>

    // ══════════════════ دسته‌بندی‌ها ══════════════════

    @GET("wp-json/wp/v2/categories")
    suspend fun getCategories(
        @Query("per_page") perPage: Int = 100,
        @Query("orderby") orderBy: String = "count",
        @Query("order") order: String = "desc"
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
