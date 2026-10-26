# ==========================================
# قوانین کامل ProGuard برای اپلیکیشن اپدونی
# ==========================================

# --- ۰. رفع خطای Missing class در R8 (فقط dontwarn، بدون تأثیر روی عملکرد) ---
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.crypto.tink.**

# --- ۱. حفظ کردن اطلاعات اصلی کاتلین ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# --- ۲. قوانین WorkManager ---
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

# --- ۳. قوانین Room Database ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.paging.**

# --- ۴. قوانین Retrofit و Gson ---
-keep class com.appdony.app.data.model.** { *; }
-keep class com.appdony.app.data.api.** { *; }
-keep class com.appdony.app.data.local.** { *; }
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.franmontiel.persistentcookiejar.**
-keep class com.franmontiel.persistentcookiejar.** { *; }

# --- ۵. قوانین Jsoup ---
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**

# --- ۶. قوانین Coil ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- ۷. قوانین Security-Crypto و DataStore ---
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-keep class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { *; }
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

# --- ۱۰. ViewModelها ---
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# --- ۱۱. جلوگیری از حذف کلاس‌های اصلی برنامه ---
-keep class com.appdony.app.AppdonyApp { *; }
-keep class com.appdony.app.MainActivity { *; }
-keep class com.appdony.app.util.** { *; }
-keep class com.appdony.app.data.session.** { *; }
-keep class com.appdony.app.data.preferences.** { *; }
