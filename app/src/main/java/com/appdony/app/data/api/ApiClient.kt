package com.appdony.app.data.api

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    const val BASE_URL = "https://appdony.ir/"
    const val WP_LOGIN_URL = "https://appdony.ir/wp-login.php"
    const val WP_ADMIN_URL = "https://appdony.ir/wp-admin/"

    private var cookieJar: PersistentCookieJar? = null
    private var httpCache: Cache? = null

    // ── کش دیسکی + «اول کش، بعد تازه‌سازی پشت صحنه» ──
    // داده‌ی قدیمی فوراً نشون داده می‌شه؛ اگه از TTL گذشته باشه، پشت صحنه تازه می‌شه تا دفعه‌ی بعد جدیده.
    private const val STALE_MAX_DAYS = 14
    private const val TTL_CATEGORIES_MS = 60L * 60 * 1000 // دسته‌ها: ۱ ساعت
    private const val TTL_POSTS_MS = 5L * 60 * 1000       // لیست پست‌ها: ۵ دقیقه
    private val refreshing = ConcurrentHashMap.newKeySet<String>()

    // مسیرهای حساس (کاربر/کامنت) هیچ‌وقت کش نمی‌شن
    private fun cacheable(url: String): Boolean =
        url.contains("/wp-json/wp/v2/") && !url.contains("/users/") && !url.contains("/comments")

    private fun ttlFor(url: String): Long =
        if (url.contains("/categories")) TTL_CATEGORIES_MS else TTL_POSTS_MS

    fun init(context: Context) {
        if (cookieJar == null) {
            cookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context.applicationContext))
        }
        if (httpCache == null) {
            httpCache = Cache(context.applicationContext.cacheDir.resolve("http_cache"), 40L * 1024 * 1024)
        }
    }

    // سرور وردپرس هدر no-cache می‌فرسته؛ اینجا اجازه‌ی کش شدن می‌دیم (TTL رو خودمون چک می‌کنیم)
    private val rewriteNetwork = Interceptor { chain ->
        val req = chain.request()
        val resp = chain.proceed(req)
        if (req.method == "GET" && resp.isSuccessful && cacheable(req.url.toString())) {
            resp.newBuilder()
                .removeHeader("Pragma")
                .removeHeader("Vary")
                .header("Cache-Control", "public, max-age=${STALE_MAX_DAYS * 24 * 3600}")
                .build()
        } else resp
    }

    private fun swr(refreshClient: () -> OkHttpClient) = Interceptor { chain ->
        val req = chain.request()
        val url = req.url.toString()
        if (req.method != "GET" || !cacheable(url) || req.cacheControl.noCache) {
            return@Interceptor chain.proceed(req)
        }
        val cachedReq = req.newBuilder()
            .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(STALE_MAX_DAYS, java.util.concurrent.TimeUnit.DAYS).build())
            .build()
        val cached = try { chain.proceed(cachedReq) } catch (e: Exception) { null }
        if (cached != null && cached.code == 200) {
            val age = System.currentTimeMillis() - cached.receivedResponseAtMillis
            if (age > ttlFor(url) && refreshing.add(url)) {
                // تازه‌سازی پشت صحنه، بدون معطل کردن کاربر
                refreshClient().newCall(req.newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build())
                    .enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) { refreshing.remove(url) }
                        override fun onResponse(call: Call, response: Response) { response.close(); refreshing.remove(url) }
                    })
            }
            return@Interceptor cached
        }
        cached?.close()
        chain.proceed(req)
    }

    // درخواست‌های API توی صف جدا از تصاویرن، پس عکس‌ها هیچ‌وقت جلوی متن رو نمی‌گیرن
    private fun apiDispatcher() = Dispatcher().apply { maxRequests = 32; maxRequestsPerHost = 12 }

    /** کلاینت سبک (بدون کوکی) با همون کش و صف؛ برای ریپازیتوری‌ها */
    fun lightClient(timeoutSec: Long = 20): OkHttpClient {
        val base = client
        return OkHttpClient.Builder()
            .cache(httpCache)
            .dispatcher(base.dispatcher)
            .connectionPool(base.connectionPool)
            .addInterceptor(swr { base })
            .addNetworkInterceptor(rewriteNetwork)
            .connectTimeout(timeoutSec, TimeUnit.SECONDS)
            .readTimeout(timeoutSec, TimeUnit.SECONDS)
            .writeTimeout(timeoutSec, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /** کلاینت تصاویر (Coil): صف جدا و محدود تا شبکه‌ی API خفه نشه */
    fun imageClient(): OkHttpClient = OkHttpClient.Builder()
        .dispatcher(Dispatcher().apply { maxRequests = 12; maxRequestsPerHost = 6 })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun clearCookies() { cookieJar?.clear() }
    val cookieJarInstance: PersistentCookieJar? get() = cookieJar

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(cookieJar ?: throw IllegalStateException("ApiClient.init() صدا زده نشده!"))
            .cache(httpCache)
            .dispatcher(apiDispatcher())
            .addInterceptor(swr { client })
            .addNetworkInterceptor(rewriteNetwork)
            // 👇 اینترسپتور برای دور زدن کش در مسیرهای حساس
            .addInterceptor { chain ->
                val request = chain.request()
                val url = request.url.toString()
                
                if (url.contains("/users/me") || url.contains("/comments")) {
                    val newRequest = request.newBuilder()
                        .header("Cache-Control", "no-cache, no-store, must-revalidate")
                        .header("Pragma", "no-cache")
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(request)
                }
            }
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.NONE })
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
