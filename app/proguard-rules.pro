# ==========================================
# قوانین کامل ProGuard برای اپلیکیشن اپدونی
# ==========================================

# --- ۱. حفظ کردن اطلاعات اصلی کاتلین ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# --- ۲. قوانین WorkManager (بسیار مهم برای جلوگیری از کرش در اسپلش) ---
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class androidx.work.impl.WorkManagerInitializer { *; }
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
-keepclassmembers class * extends androidx.work.Worker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ۳. قوانین Room Database (دیتابیس) ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# --- ۴. قوانین Retrofit و Gson ---
-keep class com.appdony.app.data.model.** { *; }
-keep class com.appdony.app.data.api.** { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**

# --- ۵. قوانین Jsoup (برای پارس کردن HTML) ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- ۶. قوانین Coil (برای لود عکس‌ها) ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- ۷. قوانین Security-Crypto و DataStore ---
-keep class androidx.security.crypto.** { *; }
-keep class androidx.datastore.** { *; }
-dontwarn androidx.security.crypto.**

# --- ۸. قوانین Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# --- ۹. قوانین Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.platform.** { *; }
-keepclassmembers class androidx.compose.runtime.SnapshotStateKt {
    public static ** mutableStateOf(...);
}

# --- ۱۰. حفظ کردن متدهای Reflection برای مدیریت دانلود ---
-keep class android.app.DownloadManager { *; }
-keepclassmembers class android.app.DownloadManager {
    public void pauseDownload(long);
    public void resumeDownload(long);
}

# --- ۱۱. جلوگیری از حذف کلاس‌های اصلی برنامه ---
-keep class com.appdony.app.MainActivity { *; }
-keep class com.appdony.app.data.session.** { *; }
-keep class com.appdony.app.data.preferences.** { *; }
