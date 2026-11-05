package com.appdony.app.data.model

import com.google.gson.annotations.SerializedName

data class UserData(
    val id: Int = 0,
    val username: String = "",
    val name: String = "",
    @SerializedName("first_name") val firstName: String = "",
    @SerializedName("last_name") val lastName: String = "",
    val email: String = "",
    val description: String = "",
    val link: String = "",
    val slug: String = "",
    val nonce: String = "",
    val capabilities: Map<String, Boolean>? = null,
    @SerializedName("avatar_urls") val avatarUrls: Map<String, String>? = null
) {
    fun displayName(): String {
        if (name.isNotBlank()) return name
        if (firstName.isNotBlank() || lastName.isNotBlank()) {
            return "$firstName $lastName".trim()
        }
        return username
    }

    /** اجازه‌ی ساخت «نوشته» (برنامه/بازی): نقش‌های نویسنده، ویراستار، مدیر و ... */
    fun canWritePosts(): Boolean = capabilities?.get("edit_posts") == true

    /** اجازه‌ی ساخت مطلب «وبلاگ»: یکی از سطح‌های دسترسی نوشتن (قابلیت اختصاصی وبلاگ یا نوشتن عمومی) */
    fun canWriteBlog(): Boolean {
        val caps = capabilities ?: return false
        return caps["edit_blogs"] == true || caps["edit_blog"] == true || caps["edit_posts"] == true
    }

    fun avatarUrl(): String? {
        return avatarUrls?.get("96") ?: avatarUrls?.get("48")
    }
}
