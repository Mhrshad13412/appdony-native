package com.appdony.app.data.model

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Int,
    val date: String,
    val title: RenderedText,
    val content: RenderedText,
    val excerpt: RenderedText,
    @SerializedName("featured_media") val featuredMedia: Int? = null,
    val categories: List<Int> = emptyList(),
    @SerializedName("_embedded") val embedded: Embedded? = null
)

data class RenderedText(val rendered: String)

data class Embedded(
    @SerializedName("wp:featuredmedia") val featuredMedia: List<Media>? = null,
    @SerializedName("wp:term") val terms: List<List<Term>>? = null
)

data class Media(
    @SerializedName("source_url") val sourceUrl: String? = null
)

data class Term(
    val id: Int,
    val name: String,
    val slug: String
)
