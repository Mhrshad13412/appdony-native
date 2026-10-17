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
    private const val API_KEY = "Pv77blYidBv5Ay2kBagJx/CEUqS3MeRdptv73csgaIjBcppfe70XQS5VAYa83b+Hni7VxeqYuMMU8NbIRWUlk0kHEP0Ja8Wkzu0Dc+dd5lFxm+baAhyWo4bjnBwaDain81yMfVMp2eI2OS5zbHS+eg=="
    private const val API_URL = "https://api.llm7.io/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendMessage(userMessage: String, history: List<Pair<String, String>> = emptyList()): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", "تو یک دستیار هوش مصنوعی در اپلیکیشن دانلود اپدونی هستی. اسمت دکتر طاعونه. لحنت دوستانه، بامزه و مفیده. به فارسی جواب بده و خیلی خلاصه و مفید توضیح بده. اگر کاربر درباره بازی یا برنامه پرسید راهنماییش کن.")
            })
            history.forEach { (role, content) ->
                messagesArray.put(JSONObject().apply {
                    put("role", role)
                    put("content", content)
                })
            }
            messagesArray.put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
            val jsonBody = JSONObject().apply {
                put("model", "default")
                put("messages", messagesArray)
            }
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (!response.isSuccessful) return@withContext Result.failure(Exception("خطا در اتصال (کد ${response.code}): $responseBody"))
            if (responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return@withContext Result.success(message.getString("content"))
                }
            }
            Result.failure(Exception("پاسخی دریافت نشد."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
