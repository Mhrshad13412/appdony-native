package com.appdony.app.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.preferences.SettingsManager
import kotlinx.coroutines.flow.first

class BlogUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // 👇 چک کردن تنظیمات کاربر
            val settingsManager = SettingsManager(applicationContext)
            val isNotifyEnabled = settingsManager.notifyBlog.first()
            if (!isNotifyEnabled) return Result.success() // اگه خاموش بود، کاری نکن

            val posts = ApiClient.api.getCustomBlogPosts(perPage = 1)
            
            if (posts.isNotEmpty()) {
                val latestPost = posts.first()
                val prefs = applicationContext.getSharedPreferences("blog_prefs", Context.MODE_PRIVATE)
                val lastSeenId = prefs.getInt("last_blog_id", 0)

                if (lastSeenId != 0 && latestPost.id != lastSeenId) {
                    NotificationHelper.showBlogNotification(applicationContext, latestPost.cleanTitle(), latestPost.id)
                }
                prefs.edit().putInt("last_blog_id", latestPost.id).apply()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
