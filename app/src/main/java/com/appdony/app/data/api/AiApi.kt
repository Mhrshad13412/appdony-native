package com.appdony.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiApi {
    // آدرس API مدل زبانی LLM7
    private const val API_URL = "https://api.llm7.io/v1/chat/completions"
    
    // 👈 کلید اختصاصی شما
    private const val API_KEY = "Pv77blYidBv5Ay2kBagJx/CEUqS3MeRdptv73csgaIjBcppfe70XQS5VAYa83b+Hni7VxeqYuMMU8NbIRWUlk0kHEP0Ja8Wkzu0Dc+dd5lFxm+baAhyWo4bjnBwaDain81yMfVMp2eI2OS5zbHS+eg=="

    // 👈 متن شخصیت دکتر طاعون (System Prompt)
    private const val SYSTEM_PROMPT = """
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
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * ارسال پیام به هوش مصنوعی و دریافت پاسخ
     * @param userMessage پیام جدیدی که کاربر تایپ کرده است
     * @param history تاریخچه چت (لیستی از جفت‌های role و content)
     */
    suspend fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList()
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            
            // ۱. اضافه کردن شخصیت (System Prompt)
            val systemMsgObj = JSONObject().apply {
                put("role", "system")
                put("content", SYSTEM_PROMPT)
            }
            messagesArray.put(systemMsgObj)

            // ۲. اضافه کردن تاریخچه چت
            history.forEach { (role, content) ->
                val msgObj = JSONObject().apply {
                    put("role", role)
                    put("content", content)
                }
                messagesArray.put(msgObj)
            }

            // ۳. اضافه کردن پیام جدید کاربر
            val userMsgObj = JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            }
            messagesArray.put(userMsgObj)

            // ساخت بدنه درخواست (JSON)
            val requestBodyJson = JSONObject().apply {
                put("model", "gpt-3.5-turbo") // مدل پیش‌فرض LLM7
                put("messages", messagesArray)
                put("temperature", 0.7) // میزان خلاقیت (0 تا 1)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            // ساخت درخواست HTTP
            val requestBuilder = Request.Builder()
                .url(API_URL)
                .post(body)

            // 👈 ارسال کلید در هدر درخواست
            if (API_KEY.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $API_KEY")
            }

            val request = requestBuilder.build()

            // ارسال درخواست و دریافت پاسخ
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("خطا از سمت سرور: ${response.code}"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("پاسخ خالی از سرور"))

                // پارس کردن پاسخ JSON
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.optJSONArray("choices")
                
                if (choices != null && choices.length() > 0) {
                    val firstChoice = choices.getJSONObject(0)
                    val messageObj = firstChoice.optJSONObject("message")
                    val content = messageObj?.optString("content") ?: ""
                    
                    if (content.isNotBlank()) {
                        Result.success(content.trim())
                    } else {
                        Result.failure(Exception("پاسخ نامعتبر از هوش مصنوعی"))
                    }
                } else {
                    Result.failure(Exception("پاسخی از هوش مصنوعی دریافت نشد"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطا در ارتباط با هوش مصنوعی: ${e.message}"))
        }
    }
}
