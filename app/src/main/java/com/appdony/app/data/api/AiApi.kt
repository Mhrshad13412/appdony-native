package com.appdony.app.data.api

import android.util.Log
import com.appdony.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

object AiApi {

    private const val TAG = "AiApi"
    private const val BASE_URL = "https://api.avalai.ir/v1"

    @Volatile
    var lastDebug: String = ""
        private set

    // کلید از GitHub Secret با اسم AI_KEY می‌آد
    private val API_KEY: String = BuildConfig.AI_KEY.trim()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val TEXT_MODELS = listOf("deepseek-v4-flash", "claude-haiku-5-5")
    private val IMAGE_MODELS = listOf("claude-haiku-5-5")

    private class ApiException(val code: Int, message: String) : Exception(message)

    suspend fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        siteContext: String = "",
        assistantNotes: String = "",
        imageBase64: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (API_KEY.isBlank()) {
            lastDebug = "AI_KEY خالیه"
            return@withContext Result.failure(Exception("کلید هوش مصنوعی توی اپ تنظیم نشده."))
        }
        try {
            val messages = buildMessages(userMessage, history, siteContext, assistantNotes, imageBase64)
            val models = if (imageBase64.isNullOrBlank()) TEXT_MODELS else IMAGE_MODELS
            var lastError: ApiException? = null

            for (model in models) {
                try {
                    return@withContext Result.success(requestOnce(model, messages))
                } catch (e: ApiException) {
                    lastError = e
                    lastDebug = "model=$model code=${e.code} ${e.message.orEmpty().take(80)}"
                    Log.w(TAG, lastDebug)
                    // کلید اشتباه، اعتبار تموم یا محدودیت؛ مدل عوض کردن فایده نداره
                    if (e.code in listOf(401, 402, 403, 429)) break
                }
            }
            Result.failure(Exception(describe(lastError)))
        } catch (e: Exception) {
            Log.w(TAG, "failed", e)
            networkFailure(e)
        }
    }

    private fun buildMessages(
        userMessage: String,
        history: List<Pair<String, String>>,
        siteContext: String,
        assistantNotes: String,
        imageBase64: String?
    ): JSONArray {
        // همه‌ی بخش‌های system توی یک پیام ادغام می‌شن
        val systemText = buildString {
            append(AiPrompt.SYSTEM)
            if (siteContext.isNotBlank()) {
                append("\n\nپست‌های مرتبط اپدونی (اگه به سؤال ربطی نداشتن نادیده بگیر):\n")
                append(siteContext)
            }
            if (assistantNotes.isNotBlank()) {
                append("\n\nاین‌ها پیام‌هایی هستن که خودت (پاپی) به‌تازگی به کاربر نشون دادی. با همین‌ها هماهنگ جواب بده:\n")
                append(assistantNotes)
            }
        }

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content", systemText))
        history.forEach { (role, content) ->
            messages.put(JSONObject().put("role", role).put("content", content))
        }

        val userObj = JSONObject().put("role", "user")
        if (imageBase64.isNullOrBlank()) {
            userObj.put("content", userMessage)
        } else {
            val url = "data:image/jpeg;base64,$imageBase64"
            val parts = JSONArray()
            parts.put(JSONObject().put("type", "text").put("text", userMessage))
            parts.put(
                JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", url))
            )
            userObj.put("content", parts)
        }
        messages.put(userObj)
        return messages
    }

    private fun networkFailure(e: Exception): Result<String> {
        lastDebug = "${e.javaClass.simpleName}: ${e.message.orEmpty().take(60)}"
        val msg = when (e) {
            is UnknownHostException -> "اتصال به اینترنت یا DNS مشکل داره."
            is SocketTimeoutException -> "پاسخ دیر رسید. دوباره امتحان کن."
            is ConnectException, is SSLException -> "اتصال به سرویس هوش مصنوعی برقرار نشد. اینترنتت رو چک کن."
            else -> "مشکلی پیش اومد. دوباره امتحان کن."
        }
        return Result.failure(Exception(msg))
    }

    private fun describe(e: ApiException?): String = when (e?.code) {
        400 -> "درخواست یا عکس پذیرفته نشد (کد ۴۰۰)."
        401, 403 -> "کلید هوش مصنوعی درست نیست یا دسترسی نداره."
        402 -> "اعتبار پاپی تموم شده. کمی بعد دوباره بپرس."
        429 -> "تعداد پیام‌ها زیاده. چند لحظه صبر کن و دوباره بپرس."
        null -> "مشکلی پیش اومد. دوباره امتحان کن."
        else -> "سرویس هوش مصنوعی در دسترس نیست (کد ${e.code})."
    }

    private fun requestOnce(model: String, messages: JSONArray): String {
        // temperature عمداً نیست؛ max_tokens بالاست چون DeepSeek قبل از جواب «فکر» می‌کنه
        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("max_tokens", 1500)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string()
            if (!response.isSuccessful) throw ApiException(response.code, text.orEmpty().take(200))
            if (text.isNullOrBlank()) throw ApiException(204, "empty body")

            val choices = JSONObject(text).optJSONArray("choices")
            if (choices == null || choices.length() == 0) throw ApiException(204, "no choices")

            val reply = choices.getJSONObject(0)
                .optJSONObject("message")?.optString("content", "")?.trim().orEmpty()
            if (reply.isBlank()) throw ApiException(204, "blank reply")
            return reply
        }
    }
}
