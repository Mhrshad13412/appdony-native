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

    fun avatarUrl(): String? {
        return avatarUrls?.get("96") ?: avatarUrls?.get("48")
    }
}
