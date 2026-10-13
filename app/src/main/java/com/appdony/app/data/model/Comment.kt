package com.appdony.app.data.model

import android.text.Html
import com.google.gson.annotations.SerializedName

data class Comment(
    val id: Int,
    @SerializedName("post") val postId: Int,
    val author_name: String = "کاربر",
    @SerializedName("author_avatar_urls") val avatarUrls: Map<String, String>? = null,
    val date: String,
    val content: RenderedText,
    val parent: Int = 0
) {
    fun cleanContent(): String {
        return Html.fromHtml(content.rendered, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace("\u00A0", " ")
            .trim()
    }

    fun avatarUrl(): String? {
        return avatarUrls?.get("96") ?: avatarUrls?.get("48")
    }
}
