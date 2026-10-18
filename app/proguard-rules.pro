# ══════════════════ Appdony Models ══════════════════
-keep class com.appdony.app.data.model.** { *; }

# ══════════════════ Retrofit ══════════════════
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
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keepclassmembers class androidx.compose.ui.platform.** {
    *;
}

# ══════════════════ Coil (تصاویر) ══════════════════
-dontwarn coil.**
-keep class coil.** { *; }

# ══════════════════ DataStore & Security ══════════════════
-keep class androidx.datastore.** { *; }
-keep class androidx.security.crypto.** { *; }

# ══════════════════ Kotlin Coroutines ══════════════════
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ══════════════════ Appdony Custom ══════════════════
-keep class com.appdony.app.** { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keep @androidx.annotation.Keep class * { *; }
