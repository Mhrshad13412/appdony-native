# ═══════════════════════════════════════
# APPDONY PROGUARD RULES
# ═══════════════════════════════════════

# حفظ اطلاعات عمومی
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes SourceFile
-keepattributes LineNumberTable

# ─── Retrofit + OkHttp + Gson ───
-keep class com.appdony.app.data.model.** { *; }
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ─── Gson مدل‌ها ───
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ─── Kotlin Coroutines ───
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ─── Compose ───
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ─── Coil ───
-dontwarn coil.**

# ─── DataStore ───
-keep class androidx.datastore.** { *; }

# ─── Appdony Classes ───
-keep class com.appdony.app.** { *; }

# ─── رفع مشکلات Reflection ───
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keep @androidx.annotation.Keep class * { *; }
