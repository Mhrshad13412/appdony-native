package com.appdony.app.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.appdony.app.BuildConfig
import com.appdony.app.data.api.SupportApi
import com.appdony.app.data.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {

    private const val FILE_NAME = "last_crash.txt"

    // کرشی که هنوز به پشتیبانی نرسیده؛ بعد از ارسال موفق پاک می‌شه (جدا از last_crash.txt که فقط برای دیالوگ داخل اپه)
    private const val PENDING_NAME = "pending_crash.json"
    private const val PREFS = "crash_reporter"
    private const val KEY_LAST_SIG = "last_sig"
    private const val KEY_LAST_TIME = "last_time"

    // گزارش‌ها توی یه گفتگوی ثابت توی پنل پشتیبانی می‌شینن (جدا از چت کاربرها) و فقط پشتیبان می‌بینه
    private const val CRASH_THREAD = "crashlog"
    private const val CRASH_NAME = "🛑 گزارش خطا"

    // یه خطای تکراری (مثلاً کرشِ پشت‌سرهم) توی این مدت فقط یک بار فرستاده می‌شه
    private const val MIN_GAP_MS = 60L * 60L * 1000L

    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = buildString {
                    append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) - ${Build.MODEL}\n")
                    append("Thread: ${thread.name}\n\n")
                    append(Log.getStackTraceString(throwable))
                }
                File(appContext.filesDir, FILE_NAME).writeText(text)
            } catch (_: Throwable) {
            }
            try {
                // توی لحظه‌ی کرش شبکه قابل اعتماد نیست؛ فقط روی دیسک می‌نویسیم و دفعه‌ی بعدِ باز شدن اپ فرستاده می‌شه
                val data = buildData("crash", "", thread.name, throwable)
                File(appContext.filesDir, PENDING_NAME).writeText(data.toString())
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** اگه کرشِ ارسال‌نشده‌ای مونده باشه، همین حالا برای پشتیبانی می‌فرسته (از AppdonyApp صدا زده می‌شه) */
    fun uploadPending(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val file = File(appContext.filesDir, PENDING_NAME)
            try {
                if (!file.exists()) return@launch
                val data = JSONObject(file.readText())
                if (send(appContext, data)) file.delete()
            } catch (_: org.json.JSONException) {
                // فایل خراب بود؛ پاکش می‌کنیم که دفعه‌ی بعد دوباره گیر نکنه
                try { file.delete() } catch (_: Throwable) { }
            } catch (_: Throwable) {
                // شبکه یا سرور مشکل داشت؛ فایل می‌مونه تا دفعه‌ی بعد
            }
        }
    }

    /** برای خطاهایی که اپ رو نمی‌بندن ولی باید پشتیبان خبردار بشه؛ tag محل خطاست (مثلاً "DownloadEngine") */
    fun reportError(context: Context, tag: String, throwable: Throwable) {
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                send(appContext, buildData("error", tag, Thread.currentThread().name, throwable))
            } catch (_: Throwable) {
            }
        }
    }

    fun read(context: Context): String? {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.readText().ifBlank { null } else null
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(context: Context) {
        try {
            File(context.filesDir, FILE_NAME).delete()
        } catch (_: Throwable) {
        }
    }

    private fun rootOf(throwable: Throwable): Throwable {
        var current = throwable
        var depth = 0
        while (depth < 10) {
            val next = current.cause ?: break
            if (next === current) break
            current = next
            depth++
        }
        return current
    }

    /** اولین خط از کد خود اپدونی توی استک (کلاس.تابع:خط) تا معلوم باشه خطا دقیقاً از کجاست */
    private fun appFrame(throwable: Throwable): String {
        var current: Throwable? = throwable
        var depth = 0
        while (current != null && depth < 10) {
            val frame = current.stackTrace.firstOrNull { it.className.startsWith("com.appdony") }
            if (frame != null) {
                return "${frame.className.substringAfterLast('.')}.${frame.methodName}:${frame.lineNumber}"
            }
            val next = current.cause
            current = if (next === current) null else next
            depth++
        }
        return ""
    }

    private fun buildData(type: String, tag: String, threadName: String, throwable: Throwable): JSONObject {
        val root = rootOf(throwable)
        return JSONObject()
            .put("type", type)
            .put("tag", tag)
            .put("thread", threadName)
            .put("error", "${root.javaClass.name}: ${root.message.orEmpty().take(200)}")
            .put("where", appFrame(throwable))
            .put("version", BuildConfig.VERSION_NAME)
            .put("device", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) | ${Build.MANUFACTURER} ${Build.MODEL}")
            .put("time", System.currentTimeMillis())
            .put("stack", Log.getStackTraceString(throwable).take(1000))
    }

    /** کاربری که الان وارد شده؛ اگه وارد نشده بود یا خوندنش نشد، یه برچسب ساده برمی‌گردونه */
    private suspend fun whoIsUser(context: Context): String {
        return try {
            val session = SessionManager(context)
            if (!session.isLoggedIn.first()) {
                "مهمان (بدون ورود)"
            } else {
                val id = session.userId.first()
                val name = session.displayName.first().ifBlank { session.username.first() }
                "$name (${SupportApi.threadOf(id)})"
            }
        } catch (_: Throwable) {
            "نامشخص"
        }
    }

    /** true یعنی کار این گزارش تمومه (فرستاده شد یا تکراری بود)؛ false یعنی بعداً دوباره تلاش بشه */
    private suspend fun send(context: Context, data: JSONObject): Boolean {
        val signature = data.optString("error") + "|" + data.optString("where")
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (prefs.getString(KEY_LAST_SIG, "") == signature && now - prefs.getLong(KEY_LAST_TIME, 0L) < MIN_GAP_MS) {
            return true
        }

        val isCrash = data.optString("type") == "crash"
        val tag = data.optString("tag")
        val whereLine = data.optString("where")
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(data.optLong("time", now)))
        val body = buildString {
            append(if (isCrash) "🛑 کرش اپ\n" else "⚠️ ایرور (اپ نبسته شد)\n")
            append("👤 ${whoIsUser(context)}\n")
            append("❗ ${data.optString("error")}\n")
            if (tag.isNotBlank()) append("🏷 $tag\n")
            if (whereLine.isNotBlank()) append("📍 $whereLine\n")
            append("📱 v${data.optString("version")} | ${data.optString("device")}\n")
            append("🕒 $time\n\n")
            append(data.optString("stack"))
        }.take(1900)

        val sent = SupportApi.send(CRASH_THREAD, "user", CRASH_NAME, body).isSuccess
        if (sent) {
            prefs.edit().putString(KEY_LAST_SIG, signature).putLong(KEY_LAST_TIME, now).apply()
        }
        return sent
    }
}
