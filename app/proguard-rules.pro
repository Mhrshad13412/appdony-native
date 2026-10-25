# ==========================================
# قوانین ProGuard برای اپلیکیشن اپدونی
# ==========================================

# ۱. حفظ کردن WorkManager (دلیل اصلی کرش در اسپلش اسکرین)
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.impl.WorkManagerInitializer { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ۲. حفظ کردن کتابخانه jsoup (برای پارس کردن HTML)
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# ۳. حفظ کردن مدل‌های API و دیتابیس (برای Retrofit، Gson و Room)
-keep class com.appdony.app.data.model.** { *; }
-keep class com.appdony.app.data.api.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepattributes Signature
-keepattributes *Annotation*

# ۴. حفظ کردن کدهای Reflection برای مدیریت دانلود
-keep class android.app.DownloadManager { *; }
-keepclassmembers class android.app.DownloadManager {
    public void pauseDownload(long);
    public void resumeDownload(long);
}
