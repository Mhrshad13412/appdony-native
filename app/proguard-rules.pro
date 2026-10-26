# ==========================================
# قوانین ProGuard/R8 اپلیکیشن اپدونی (نسخه‌ی امن)
# ==========================================

# --- ۱. Attributeهای لازم ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# --- ۲. رفع خطای Missing class ---
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn org.checkerframework.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn com.google.j2objc.annotations.**

# --- ۳. Tink + Security-Crypto (EncryptedSharedPreferences) ---
-keep class com.google.crypto.tink.** { *; }
-keep class androidx.security.crypto.** { *; }
-keep class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn androidx.security.crypto.**

# --- ۴. WorkManager و Workerهای خودت ---
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.appdony.app.util.** { *; }

# --- ۵. Room ---
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep class com.appdony.app.data.local.** { *; }
-dontwarn androidx.room.paging.**

# --- ۶. Retrofit / Gson / OkHttp ---
-keep class com.appdony.app.data.model.** { *; }
-keep class com.appdony.app.data.api.** { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- ۷. Cookie / Jsoup / Coil / Compose ---
-dontwarn com.franmontiel.persistentcookiejar.**
-keep class com.franmontiel.persistentcookiejar.** { *; }
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
-keep class com.appdony.app.AppdonyApp { *; }
-keep class com.appdony.app.MainActivity { *; }
-keep class com.appdony.app.data.session.** { *; }
-keep class com.appdony.app.data.preferences.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# --- ۱۰. Parcelable / Serializable / Enum ---
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
