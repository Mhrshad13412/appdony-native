# ══════════════════ مدل‌های اپدونی (بسیار مهم) ══════════════════
-keep class com.appdony.app.data.model.** { *; }

# ══════════════════ Retrofit & OkHttp ══════════════════
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ══════════════════ Gson ══════════════════
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ══════════════════ Jetpack Compose ══════════════════
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-dontwarn androidx.compose.**

# ══════════════════ Coil (عکس‌ها) ══════════════════
-keep class coil.** { *; }
-dontwarn coil.**

# ══════════════════ DataStore & Security ══════════════════
-keep class androidx.datastore.** { *; }
-keep class androidx.security.crypto.** { *; }

# ══════════════════ Coroutines ══════════════════
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ══════════════════ کلاس‌های اصلی اپدونی ══════════════════
-keep class com.appdony.app.** { *; }
