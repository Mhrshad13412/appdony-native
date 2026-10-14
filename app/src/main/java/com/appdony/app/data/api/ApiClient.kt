package com.appdony.app.data.api

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://appdony.ir/"
    const val WP_LOGIN_URL = "https://appdony.ir/wp-login.php"
    const val WP_ADMIN_URL = "https://appdony.ir/wp-admin/"

    private var cookieJar: PersistentCookieJar? = null

    fun init(context: Context) {
        if (cookieJar == null) {
            cookieJar = PersistentCookieJar(
                SetCookieCache(),
                SharedPrefsCookiePersistor(context.applicationContext)
            )
        }
    }

    fun clearCookies() {
        cookieJar?.clear()
    }

    val cookieJarInstance: PersistentCookieJar?
        get() = cookieJar

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar ?: throw IllegalStateException("ApiClient.init() رو صدا بزن!"))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    val api: AppdonyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppdonyApi::class.java)
    }
}
