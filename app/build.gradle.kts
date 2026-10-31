plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.21-1.0.28"
}

android {
    namespace = "com.appdony.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.appdony.app"
        minSdk = 24
        targetSdk = 35
        // نسخه: زیربنا.قابلیت‌ها.دیباگ‌ها (فقط همین خط رو عوض کن؛ کد نسخه خودش از روی اون ساخته می‌شه)
        versionName = "2.39.76"
        versionCode = versionName!!.split(".").let { (a, b, c) -> a.toInt() * 1_000_000 + b.toInt() * 1_000 + c.toInt() }

        // توکن هوش مصنوعی از متغیر محیطی AI_KEY (GitHub Secret) یا از local.properties می‌آد؛ توی ریپو نمی‌مونه
        val aiKey = System.getenv("AI_KEY")
            ?: rootProject.file("local.properties").takeIf { it.exists() }
                ?.readLines()?.firstOrNull { it.startsWith("AI_KEY=") }?.removePrefix("AI_KEY=")
            ?: ""
        buildConfigField("String", "AI_KEY", "\"${aiKey.trim()}\"")

        // آدرس و کلید عمومی Supabase برای چت پشتیبانی (از GitHub Secret یا local.properties)
        fun secret(name: String): String =
            System.getenv(name)
                ?: rootProject.file("local.properties").takeIf { it.exists() }
                    ?.readLines()?.firstOrNull { it.startsWith("$name=") }?.removePrefix("$name=")
                ?: ""
        buildConfigField("String", "SUPA_URL", "\"${secret("SUPA_URL").trim()}\"")
        buildConfigField("String", "SUPA_KEY", "\"${secret("SUPA_KEY").trim()}\"")

        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            // ✅ روشن کردن فشرده‌سازی برای کاهش حجم
            isMinifyEnabled = true 
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implemen
