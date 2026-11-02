package com.appdony.app.data.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * امتیاز شهرت بازی‌ها از فایل popularity.json که هر روز توسط GitHub Action (با دیتای RAWG) ساخته می‌شه.
 * اپ مستقیم با RAWG حرف نمی‌زنه؛ فقط همین فایل کوچیک رو از گیت‌هاب می‌خونه و کش می‌کنه.
 * اگه فایل نبود یا دانلود نشد، فقط لیست دستی (Popularity.kt) استفاده می‌شه.
 */
object PopularityRemote {

    private val URLS = listOf(
        "https://raw.githubusercontent.com/Mhrshad13412/appdony-native/main/popularity.json",
        "https://cdn.jsdelivr.net/gh/Mhrshad13412/appdony-native@main/popularity.json"
    )
    private const val MAX_AGE_MS = 12L * 60 * 60 * 1000
    private const val RETRY_GAP_MS = 10L * 60 * 1000

    @Volatile private var scores: Map<Int, Int> = emptyMap()
    @Volatile private var file: File? = null
    @Volatile private var lastTry = 0L

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    /** یک بار موقع بالا اومدن اپ صدا زده می‌شه؛ کش دیسکی رو فوراً می‌خونه */
    fun init(context: Context) {
        val f = File(context.applicationContext.cacheDir, "popularity.json")
        file = f
        try {
            if (f.exists()) parse(f.readText())?.let { scores = it }
        } catch (e: Exception) {
            // فایل خراب: نادیده بگیر
        }
    }

    fun score(postId: Int): Int? = scores[postId]

    /** اگه هنوز دیتایی نداریم صبر می‌کنه (حداکثر ۴ ثانیه)؛ اگه دیتای قدیمی داریم پشت صحنه تازه می‌کنه */
    suspend fun ensureFresh() {
        val f = file ?: return
        val age = System.currentTimeMillis() - f.lastModified()
        if (scores.isNotEmpty() && age < MAX_AGE_MS) return
        val now = System.currentTimeMillis()
        if (now - lastTry < RETRY_GAP_MS) return
        lastTry = now
        if (scores.isEmpty()) {
            withTimeoutOrNull(4000) { withContext(Dispatchers.IO) { download(f) } }
        } else {
            Thread { download(f) }.start()
        }
    }

    private fun download(f: File) {
        for (url in URLS) {
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use
                    val parsed = parse(body) ?: return@use
                    scores = parsed
                    try {
                        val tmp = File(f.parentFile, "popularity.json.tmp")
                        tmp.writeText(body)
                        tmp.renameTo(f)
                    } catch (e: Exception) {
                        // ننوشتن روی دیسک مهم نیست؛ توی حافظه هست
                    }
                    return
                }
            } catch (e: Exception) {
                // آدرس بعدی
            }
        }
    }

    private fun parse(text: String): Map<Int, Int>? {
        return try {
            val obj = JSONObject(text).getJSONObject("scores")
            val map = HashMap<Int, Int>(obj.length() * 2)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val id = k.toIntOrNull() ?: continue
                map[id] = obj.optInt(k, 0)
            }
            map
        } catch (e: Exception) {
            null
        }
    }
}

/** مرتب‌سازی پایدار بر اساس شهرت: بیشترِ (امتیاز RAWG، امتیاز لیست دستی)؛ بقیه با ترتیب قبلی */
fun List<Post>.sortedByFame(): List<Post> =
    sortedByDescending { maxOf(Popularity.score(it.cleanTitle()), PopularityRemote.score(it.id) ?: 0) }
