package com.appdony.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object AiApi {

    // درخواست به سایت خودمون می‌ره و سرور سایت به هوش مصنوعی وصل می‌شه
    private const val API_URL = "https://appdony.ir/wp-json/appdony/v1/ai"

    private val SYSTEM_PROMPT = """
        تو "دکتر طاعون" هستی، دستیار هوشمند و بسیار باحال اپلیکیشن "اپدونی".
        اپدونی یک مرجع دانلود بازی و برنامه برای اندروید است.

        وظایف تو:
        1. کمک به کاربران برای پیدا کردن بازی‌ها و برنامه‌های مورد علاقه‌شون.
        2. پاسخ دادن به سوالات فنی در مورد نصب، دانلود و اجرای بازی‌ها.
        3. راهنمایی کاربران در مورد مقالات وبلاگ اپدونی.

        لحن تو باید دوستانه، کمی طنز و شوخ‌طبع (به خاطر اسمت) و در عین حال بسیار مفید و دقیق باشد.
        پاسخ‌هایت را کوتاه، مفید و به زبان فارسی روان بنویس. از ایموجی‌های مناسب استفاده کن.
    """.trimIndent()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            history.forEach { (role, content) ->
                messages.put(JSONObject().put("role", role).put("content", content))
            }
            messages.put(JSONObject().put("role", "user").put("content", userMessage))

            val body = JSONObject().put("messages", messages).toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val text = response.body?.string()

                if (response.code == 429) {
                    return@withContext Result.failure(Exception("تعداد پیام‌ها زیاده. یه دقیقه صبر کن و دوباره بپرس."))
                }
                if (!response.isSuccessful || text.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("سرویس هوش مصنوعی الان در دسترس نیست (کد ${response.code})."))
                }

                val reply = JSONObject(text).optString("reply", "")
                if (reply.isBlank()) {
                    return@withContext Result.failure(Exception("پاسخی دریافت نشد. دوباره امتحان کن."))
                }
                Result.success(reply)
            }
        } catch (e: UnknownHostException) {
            Result.failure(Exception("اتصال به اینترنت برقرار نیست."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("پاسخ دیر رسید. دوباره امتحان کن."))
        } catch (e: Exception) {
            Result.failure(Exception("مشکلی پیش اومد. دوباره امتحان کن."))
        }
    }
}
