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
    /** نقش‌های حساب در وردپرس (از users/me?context=edit) */
    val roles: List<String>? = null,
    /** مدیر کل شبکه (فقط روی چندسایتی وردپرس پر می‌شه) */
    @SerializedName("is_super_admin") val isSuperAdmin: Boolean? = null,
    @SerializedName("avatar_urls") val avatarUrls: Map<String, String>? = null
) {
    companion object {
        /**
         * فقط این چهار نقش اجازه‌ی استفاده از ادیتور رو دارن:
         * مدیر کل (super admin)، مدیر (administrator)، ویرایشگر (editor) و نویسنده (author).
         * بقیه‌ی نقش‌ها (مشارکت‌کننده، مشترک، مشتری و ...) هیچ دکمه‌ی نوشتنی نمی‌بینن.
         */
        private val ALLOWED_ROLES = setOf("administrator", "editor", "author")
    }

    fun displayName(): String {
        if (name.isNotBlank()) return name
        if (firstName.isNotBlank() || lastName.isNotBlank()) {
            return "$firstName $lastName".trim()
        }
        return username
    }

    /** آیا حساب یکی از چهار نقش مجاز (مدیر کل، مدیر، ویرایشگر، نویسنده) رو داره؟ */
    fun hasEditorRole(): Boolean {
        if (isSuperAdmin == true) return true
        val list = roles ?: return false
        return list.any { it.trim().lowercase() in ALLOWED_ROLES }
    }

    /** اجازه‌ی ساخت «نوشته» (برنامه/بازی): فقط چهار نقش مجاز، و با قابلیت نوشتن واقعی */
    fun canWritePosts(): Boolean = hasEditorRole() && capabilities?.get("edit_posts") == true

    /** اجازه‌ی ساخت مطلب «وبلاگ»: فقط چهار نقش مجاز، و با یکی از قابلیت‌های نوشتن */
    fun canWriteBlog(): Boolean {
        if (!hasEditorRole()) return false
        val caps = capabilities ?: return false
        return caps["edit_blogs"] == true || caps["edit_blog"] == true || caps["edit_posts"] == true
    }

    fun avatarUrl(): String? {
        return avatarUrls?.get("96") ?: avatarUrls?.get("48")
    }
}
