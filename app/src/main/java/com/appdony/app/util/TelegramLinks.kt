package com.appdony.app.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** لینک‌های تلگرام رو مستقیم با اپ تلگرام باز می‌کنه (نه با وب) */
object TelegramLinks {

    private val HOSTS = setOf("t.me", "telegram.me", "telegram.dog")

    // تلگرام رسمی، نسخه‌ی وب‌سایت تلگرام، تلگرام X و پلاس
    private val PACKAGES = listOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "org.telegram.plus"
    )

    fun isTelegram(url: String): Boolean {
        val u = url.trim()
        if (u.startsWith("tg:", ignoreCase = true)) return true
        val host = u.toHttpUrlOrNull()?.host?.lowercase() ?: return false
        return host in HOSTS
    }

    /** اول اپ تلگرام؛ اگه نصب نبود آخرش مرورگر (صفحه‌ی دانلود تلگرام) */
    fun open(context: Context, url: String): Boolean {
        val uri = try { Uri.parse(url.trim()) } catch (e: Exception) { return false }
        for (pkg in PACKAGES) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, uri).setPackage(pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return true
            } catch (e: ActivityNotFoundException) {
                // این نسخه از تلگرام نصب نیست؛ بعدی
            } catch (e: SecurityException) {
            }
        }
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            false
        }
    }
}
