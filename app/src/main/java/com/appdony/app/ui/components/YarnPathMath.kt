package com.appdony.app.ui.components

import kotlin.math.abs
import kotlin.math.sin

// ───────────────────────── مسیر کاموا‌ها موقع بررسی لینک ─────────────────────────

/** قل خوردن: ۱ دور و نیم تا وسط مسیر و بقیه موقع برگشتن */
internal fun dlSpin(q: Float): Float = q * 540f

/** کاموا تا وقتی از بالا نیومده نامرئیه و آخر مسیر پیش گربه محو می‌شه */
internal fun dlAlpha(q: Float): Float = when {
    q <= 0f -> 0f
    q < 2.6f -> 1f
    else -> ((3f - q) / 0.4f).coerceIn(0f, 1f)
}

/** پرش‌های کوچیک موقع افتادن از بالا: هر بار پایین می‌رسه یه کم بالا می‌پره */
internal fun dlBounce(q: Float): Float {
    if (q <= 0f || q >= 1f) return 0f
    return abs(sin(q * Math.PI * 3.0)).toFloat() * (1f - q) * 0.9f
}
