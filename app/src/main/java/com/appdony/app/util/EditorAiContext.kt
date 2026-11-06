package com.appdony.app.util

/**
 * خلاصه‌ی نوشته‌ای که کاربر الان توی ادیتور (نوشته یا وبلاگ) داره می‌نویسه.
 * ادیتور هر چند لحظه به‌روزش می‌کنه و چت هوش مصنوعی (داخل اپ، شناور و بیرون برنامه)
 * خودکار توی پرامپتش می‌ذارتش. بعد از خروج از ادیتور ۵ دقیقه می‌مونه (مثلاً برای پیش‌نمایش)، بعدش پاک می‌شه.
 */
object EditorAiContext {

    private const val GRACE_MS = 5L * 60 * 1000

    private var text: String = ""
    private var active = false
    private var graceUntil = 0L

    @Synchronized
    fun update(summary: String) {
        text = summary
        active = summary.isNotBlank()
        graceUntil = 0L
    }

    @Synchronized
    fun pause() {
        if (active) {
            active = false
            graceUntil = System.currentTimeMillis() + GRACE_MS
        }
    }

    @Synchronized
    fun promptBlock(): String {
        if (text.isBlank()) return ""
        if (active) return text
        if (System.currentTimeMillis() < graceUntil) return text
        text = ""
        return ""
    }
}
