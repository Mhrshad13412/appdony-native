package com.appdony.app.data.model

/**
 * بلوک تصویر: [EditorBlock.text] = «شناسه|آدرس» (شناسه‌ی رسانه‌ی وردپرس؛ صفر = ناشناخته)
 * و [EditorBlock.extra] = متن جایگزین (alt).
 */
object ImageData {

    fun encode(mediaId: Long, url: String): String =
        if (url.isBlank()) "" else "${mediaId.coerceAtLeast(0L)}|${url.trim()}"

    fun mediaId(text: String): Long {
        val cut = text.indexOf('|')
        if (cut <= 0) return 0L
        val head = text.substring(0, cut)
        return if (head.all { it.isDigit() }) head.toLongOrNull() ?: 0L else 0L
    }

    fun url(text: String): String {
        val cut = text.indexOf('|')
        if (cut > 0 && text.substring(0, cut).all { it.isDigit() }) return text.substring(cut + 1).trim()
        return text.trim()
    }
}
