# ==========================================
# قوانین ProGuard/R8 اپلیکیشن اپدونی (نسخه‌ی سبک)
# ==========================================

# --- ۱. Attributeهای لازم ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# --- ۲. رفع خطای Missing class (علت اصلی فیل شدن بیلد) ---
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.crypto.tink.**
-dontwarn androidx.security.crypto.**

# --- ۳. Tink (فقط کلاس‌های پروتوبافِ لازم، نه کل کتابخانه) ---
-keepclassmembers class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite {
    <fields>;
}

# --- ۴. WorkManager (فقط Workerهای خودت) ---
-keepclassmembers class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# --- ۵. Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- ۶. Retrofit / Gson / OkHttp ---
-keep class com.appdony.app.data.model.** { *; }
-keep class com.appdony.app.data.api.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- ۷. Jsoup / Coil / Compose ---
-dontwarn org.jsoup.**
-dontwarn coil.**
-dontwarn androidx.compose.**

# --- ۸. Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# --- ۹. کلاس‌های اصلی اپ ---
-keep class com.appdony.app.MainActivity { *; }
-keep class com.appdony.app.data.session.** { *; }
-keep class com.appdony.app.data.preferences.** { *; }
