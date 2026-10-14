package com.appdony.app.data.api

import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://appdony.ir/"

    private var cache: Cache? = null

    private fun getCache(context: android.content.Context): Cache {
        return cache ?: Cache(
            File(context.cacheDir, "http_cache"),
            50L * 1024 * 1024 // ۵۰ مگ
        ).also { cache = it }
    }

    fun init(context: android.content.Context) {
        getCache(context)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cache(cache)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header("Cache-Control", "public, max-age=300")
                    .build()
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
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
