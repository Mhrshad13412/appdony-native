package com.appdony.app.data.model

import android.text.Html

fun Post.featuredImageUrl(): String? {
    return embedded?.featuredMedia?.firstOrNull()?.sourceUrl
}

fun Post.cleanTitle(): String {
    return Html.fromHtml(title.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .trim()
}

fun Post.cleanExcerpt(): String {
    return Html.fromHtml(excerpt.rendered, Html.FROM_HTML_MODE_LEGACY)
        .toString()
        .replace("\n", " ")
        .trim()
}
