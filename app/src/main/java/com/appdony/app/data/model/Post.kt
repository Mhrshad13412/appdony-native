package com.appdony.app.data.model

import com.google.gson.annotations.SerializedName

data class Post(
    val id: Int,
    val date: String? = null,
    val title: RenderedText,
    val content: RenderedText,
    val excerpt: RenderedText? = null, // 👈 اینجا اختیاری شد
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
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("media_details") val mediaDetails: MediaDetails? = null
)

data class MediaDetails(
    val width: Int? = null,
    val height: Int? = null,
    val sizes: Map<String, MediaSize>? = null
)

data class MediaSize(
    @SerializedName("source_url") val sourceUrl: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

data class Term(
    val id: Int,
    val name: String,
    val slug: String,
    val taxonomy: String? = null
)
