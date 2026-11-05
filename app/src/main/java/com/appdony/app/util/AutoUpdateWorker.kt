package com.appdony.app.util

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appdony.app.R
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.preferences.DownloadRecord
import com.appdony.app.data.preferences.DownloadStore
import com.appdony.app.data.preferences.SettingsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * آپدیت خودکار (هر ۶ ساعت، و یک‌بار بلافاصله وقتی کاربر سوئیچ رو روشن می‌کنه):
 * ۱) خود اپدونی: نسخه‌ی جدید توی پس‌زمینه دانلود می‌شه و اعلان «آماده‌ی نصب» میاد.
 * ۲) برنامه‌هایی که کاربر دانلود کرده: اگه پستشون تو سایت آپدیت شده باشه، نسخه‌ی جدید دانلود می‌شه.
 * هر دو پیش‌فرض خاموشن و از تنظیمات روشن می‌شن. نصب نهایی همیشه با تأیید کاربره (قانون اندروید).
 */
class AutoUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ctx = applicationContext
            NetworkMonitor.init(ctx)
            val settings = SettingsManager(ctx)
            val appOn = settings.autoUpdateApp.first()
            val appsOn = settings.autoUpdateApps.first()
            if (!appOn && !appsOn) return Result.success()

            val wifiOnly = settings.wifiOnly.first()
            if (!NetworkMonitor.usable(wifiOnly)) return Result.success()

            if (appOn) updateSelf(ctx)
            if (appsOn) updateDownloadedApps(ctx, wifiOnly)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }

    // ---------- خود اپدونی ----------

    private suspend fun updateSelf(ctx: Context) {
        val info = UpdateHelper.checkForUpdate(ctx) ?: return
        val apk = File(File(ctx.cacheDir, "updates"), "appdony-${info.latestVersion}.apk")
        val ready = apk.exists() && apk.length() > 100_000L &&
            (info.sizeBytes <= 0L || apk.length() == info.sizeBytes)
        val file = if (ready) apk else (UpdateHelper.download(ctx, info) { } ?: return)

        val prefs = ctx.getSharedPreferences("auto_update", Context.MODE_PRIVATE)
        if (prefs.getString("notified", null) == info.latestVersion) return
        notifyReady(ctx, file, info.latestVersion)
        prefs.edit().putString("notified", info.latestVersion).apply()
    }

    private fun notifyReady(ctx: Context, file: File, version: String) {
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(CH_UPDATE, "آپدیت اپدونی", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !ctx.packageManager.canRequestPackageInstalls()
            ) {
                // اول اجازه‌ی «نصب از منابع ناشناس»
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
            } else {
                val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pi = PendingIntent.getActivity(
                ctx, 7001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, CH_UPDATE)
                .setSmallIcon(R.drawable.ic_notification_box)
                .setContentTitle("نسخه‌ی جدید اپدونی آماده‌ی نصبه")
                .setContentText("نسخه‌ی $version دانلود شد؛ بزن تا نصب بشه.")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(NOTIF_UPDATE_ID, n)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ---------- برنامه‌هایی که کاربر دانلود کرده ----------

    private suspend fun updateDownloadedApps(ctx: Context, wifiOnly: Boolean) {
        val store = DownloadStore(ctx)
        val latest = store.getAll()
            .filter { it.completed && it.postId > 0 }
            .groupBy { it.postId }
            .values
            .mapNotNull { list -> list.maxByOrNull { it.createdAt } }
            .take(20)
        val deadline = System.currentTimeMillis() + 9 * 60_000L
        for (rec in latest) {
            currentCoroutineContext().ensureActive()
            if (System.currentTimeMillis() > deadline) break
            try {
                updateOne(ctx, store, rec, wifiOnly, deadline)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // یکی خراب شد؛ بقیه ادامه پیدا کنن
            }
        }
    }

    private suspend fun updateOne(
        ctx: Context, store: DownloadStore, rec: DownloadRecord, wifiOnly: Boolean, deadline: Long
    ) {
        val post = ApiClient.api.getPost(rec.postId)
        val newMod = post.modified ?: return
        val oldMod = rec.postModified
        if (oldMod == null) {
            // اولین بار فقط نقطه‌ی شروع ثبت می‌شه؛ چیزی دانلود نمی‌شه
            store.update(rec.copy(postModified = newMod))
            return
        }
        if (newMod <= oldMod) return

        val found = InstallLinkFinder.find(post.id, post.link).getOrNull() ?: return
        val url = found.links
            .firstOrNull { it.url.isNotBlank() && !TelegramLinks.isTelegram(it.url) }?.url ?: return

        val resolved = withContext(Dispatchers.IO) {
            try {
                DownloadHelper.resolve(url)
            } catch (e: IOException) {
                null
            }
        } ?: return

        // فقط متن پست ویرایش شده؛ فایل همونه (هم‌حجم)، پس دوباره دانلود نمی‌کنیم
        if (resolved.size > 0 && resolved.size == rec.totalBytes) {
            store.update(rec.copy(postModified = newMod))
            return
        }

        val id = System.currentTimeMillis()
        store.add(
            DownloadRecord(
                id = id,
                title = rec.title,
                iconUrl = rec.iconUrl,
                fileName = DownloadHelper.buildFileName(rec.title, resolved),
                sourceUrl = url,
                createdAt = id,
                totalBytes = if (resolved.size > 0) resolved.size else 0L,
                status = DownloadManager.STATUS_PENDING,
                message = "در صف دانلود...",
                wifiOnly = wifiOnly,
                postId = rec.postId,
                postModified = newMod
            )
        )
        DownloadEngine.start(ctx, id, resolved)

        while (DownloadEngine.isActive(id) && System.currentTimeMillis() < deadline) {
            delay(2000L)
        }
        val done = store.getAll().firstOrNull { it.id == id }
        if (done?.completed == true) {
            // نسخه‌ی جدید کامل شد؛ نسخه‌ی قدیمی از لیست و حافظه پاک بشه
            DownloadEngine.cancelAndDelete(ctx, rec.id)
        }
    }

    private companion object {
        const val CH_UPDATE = "app_update_ready"
        const val NOTIF_UPDATE_ID = 4001
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
