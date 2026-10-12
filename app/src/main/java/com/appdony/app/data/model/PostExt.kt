package com.appdony.app.data.model

import android.text.Html

fun Post.featuredImageUrl(): String? {
    // ۱. از _embedded
    embedded?.featuredMedia?.firstOrNull()?.sourceUrl?.let {
        if (it.isNotBlank()) return it
    }
    // ۲. از HTML محتوا - اولین تصویر
    val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
    regex.find(content.rendered)?.groupValues?.getOrNull(1)?.let {
        if (it.isNotBlank()) return it
    }
    // ۳. یه عکس از media_details
    return embedded?.featuredMedia?.firstOrNull()
        ?.mediaDetails?.sizes?.get("medium")?.sourceUrl
}

fun Post.cleanTitle(): String {
    return Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\u00A0", " ")
        .trim()
}

fun Post.cleanExcerpt(): String {
    return Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\n", " ")
        .replace("\u00A0", " ")
        .trim()
}

fun Post.cleanContent(): String {
    return Html.fromHtml(content.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\u00A0", " ")
        .trim()
}

fun Post.primaryCategoryName(): String {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.name ?: "دسته‌بندی"
}

fun Post.primaryCategoryId(): Int? {
    return embedded?.terms?.firstOrNull()?.firstOrNull()?.id
}
