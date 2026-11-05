package com.appdony.app.data.model

import java.util.concurrent.ConcurrentHashMap

/**
 * ⚠️ مهم — تحت هیچ عنوان حذف نشه ⚠️
 * پست‌هایی که قبلاً تو لیست‌ها (خانه، دسته‌ها و ...) دیده شدن؛ وقتی کاربر یکی رو باز می‌کنه
 * عکس، اسم و دسته‌ش همون لحظه از اینجا نشون داده می‌شه و فقط بقیه‌ی صفحه (مشخصات، لینک، توضیحات) لود می‌شه.
 */
object PostMemory {
    private val map = ConcurrentHashMap<Int, Post>()

    fun put(post: Post) {
        if (map.size > 600) map.clear()
        map[post.id] = post
    }

    fun get(id: Int): Post? = map[id]

    /** پیدا کردن پست از روی اسم تمیزش (همون اسمی که موقع شروع دانلود داده می‌شه) */
    fun findByTitle(title: String): Post? = map.values.firstOrNull { it.cleanTitle() == title }
}
