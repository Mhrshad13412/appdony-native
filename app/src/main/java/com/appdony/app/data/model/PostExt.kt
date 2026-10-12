package com.appdony.app.data.model

fun Post.featuredImageUrl(): String? {
    return embedded?.featuredMedia?.firstOrNull()?.sourceUrl
}

fun Post.cleanTitle(): String {
    return title.rendered
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&#8211;", "-")
        .replace("&#8217;", "'")
        .trim()
}
