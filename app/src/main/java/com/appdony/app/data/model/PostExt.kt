package com.appdony.app.data.model

import android.text.Html

fun Post.featuredImageUrl(): String? {
    val media = embedded?.featuredMedia?.firstOrNull()

    // ۱. تلاش برای گرفتن سایز متوسط (برای سرعت بالا در لیست‌ها)
    media?.mediaDetails?.sizes?.let { sizes ->
        sizes["medium"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
        sizes["medium_large"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
        sizes["thumbnail"]?.sourceUrl?.let { if (it.isNotBlank()) return it }
    }

    // ۲. اگر سایزها نبود، عکس اصلی
    media?.sourceUrl?.let { if (it.isNotBlank()) return it }

    // ۳. اگر هیچی نبود، از HTML محتوا اولین عکس رو دربیار
    val regex = Regex("""<img[^>]+src=["']([^"']+)["']""")
    regex.find(content.rendered)?.groupValues?.getOrNull(1)?.let {
        if (it.isNotBlank()) return it
    }

    return null
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
