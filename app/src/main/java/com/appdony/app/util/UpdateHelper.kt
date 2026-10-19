package com.appdony.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object UpdateHelper {
    // آدرس API گیت‌هاب برای گرفتن آخرین ریلیز
    private const val GITHUB_API = "https://api.github.com/repos/Mhrshad13412/appdony-native/releases/latest"

    data class UpdateInfo(val latestVersion: String, val downloadUrl: String, val changelog: String)

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val request = Request.Builder().url(GITHUB_API).get().build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val tagName = json.getString("tag_name").replace("v", "") // مثلا v2.9.36 میشه 2.9.36
                val body = json.optString("body", "نسخه جدید با بهبود عملکرد و رفع باگ‌ها.")
                
                val assets = json.getJSONArray("assets")
                if (assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    return@withContext UpdateInfo(tagName, downloadUrl, body)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
