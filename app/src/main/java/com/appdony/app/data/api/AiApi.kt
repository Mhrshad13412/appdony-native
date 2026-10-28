package com.appdony.app.data.api

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

    private const val BASE_URL = "https://api.llm7.io/v1"

    private val API_KEY = "Pv77blYidBv5Ay2kBagJx/CEUqS3MeRdptv73csgaIjBcppfe70XQS5VAYa83b+Hni7VxeqYuMMU8NbIRWUlk0kHEP0Ja8Wkzu0Dc+dd5lFxm+baAhyWo4bjnBwaDain81yMfVMp2eI2OS5zbHS+eg=="

    private val SYSTEM_PROMPT = """
        تو «دکتر طاعون» هستی، دستیار اپلیکیشن «اپدونی» (مرجع دانلود بازی و برنامه‌ی اندروید).

        قوانین:
        - فقط به فارسی ساده و روان جواب بده. کوتاه بنویس (حداکثر ۴ جمله).
        - فقط به چیزی که کاربر پرسیده جواب بده. اگه سؤال نامفهوم بود، یک سؤال کوتاه بپرس.
        - هیچ‌وقت اسم بازی، لینک، قیمت یا نسخه از خودت نساز. فقط از «پست‌های مرتبط اپدونی» که در ادامه بهت داده می‌شه پیشنهاد بده. اگه مرتبط نبودن یا چیزی نبود، بگو با دکمه‌ی جستجوی اپ پیداش کنه.
        - اگه کاربر مشکل نصب یا اجرا داشت، مراحل ساده بگو: فعال کردن «نصب از منابع ناشناس»، فضای خالی گوشی، نسخه‌ی اندروید و پاک کردن کش.
        - لحن دوستانه و کمی شوخ داشته باش، با حداکثر یک ایموجی.
    """.trimIndent()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // مدل‌های ضعیف و کوچک رو کنار می‌ذاریم و بهترین مدل موجود رو انتخاب می‌کنیم
    private val PREFERRED = listOf(
        "gpt-4.1", "gpt-4o", "gpt-oss-120b", "llama-3.3-70b", "llama-70b",
        "qwen", "mistral-large", "gpt-oss-20b"
    )
    private val WEAK_MARKS = listOf("nano", "mini", "lite", "tiny", "small", "1b", "3b", "7b", "8b")

    @Volatile
    private var cachedModel: String? = null

    private fun parseModelIds(text: String): List<String> {
        val array: JSONArray = if (text.trimStart().startsWith("[")) {
            JSONArray(text)
        } else {
            JSONObject(text).optJSONArray("data") ?: JSONArray()
        }
        val ids = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.opt(i)
            if (item is JSONObject) {
                val id = item.optString("id")
                if (id.isNotBlank()) ids.add(id)
            } else if (item is String && item.isNotBlank()) {
                ids.add(item)
            }
        }
        return ids
    }

    private fun discoverModel(): String {
        cachedModel?.let { return it }

        val ids: List<String> = try {
            val request = Request.Builder()
                .url("$BASE_URL/models")
                .addHeader("Authorization", "Bearer $API_KEY")
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (response.isSuccessful && text.isNotBlank()) parseModelIds(text) else emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }

        val strong = ids.filter { id -> WEAK_MARKS.none { id.contains(it, ignoreCase = true) } }
        val picked = PREFERRED.firstNotNullOfOrNull { pref ->
            strong.firstOrNull { it.contains(pref, ignoreCase = true) }
        } ?: "default"

        cachedModel = picked
        return picked
    }

    suspend fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        siteContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))

            if (siteContext.isNotBlank()) {
                messages.put(
                    JSONObject().put("role", "system").put(
                        "content",
                        "پست‌های مرتبط اپدونی (ممکنه به سؤال ربطی نداشته باشن؛ اگه نداشتن نادیده بگیر):\n$siteContext"
                    )
                )
            }

            history.forEach { (role, content) ->
                messages.put(JSONObject().put("role", role).put("content", content))
            }
            messages.put(JSONObject().put("role", "user").put("content", userMessage))

            var result = requestOnce(discoverModel(), messages)

            // اگه مدل انتخاب‌شده قبول نشد، با مدل امن‌تر دوباره امتحان می‌کنیم
            if (result.isFailure && result.exceptionOrNull() is BadModelException) {
                cachedModel = "fast"
                result = requestOnce("fast", messages)
            }
            result
        } catch (e: UnknownHostException) {
            Result.failure(Exception("اتصال به اینترنت یا DNS مشکل داره."))
        } catch (e: SocketTimeoutException) {
            Result.failure(Exception("پاسخ دیر رسید. دوباره امتحان کن."))
        } catch (e: ConnectException) {
            Result.failure(Exception("سرویس هوش مصنوعی از شبکه‌ی تو در دسترس نیست. با VPN امتحان کن."))
        } catch (e: SSLException) {
            Result.failure(Exception("سرویس هوش مصنوعی از شبکه‌ی تو در دسترس نیست. با VPN امتحان کن."))
        } catch (e: Exception) {
            Result.failure(Exception("مشکلی پیش اومد. دوباره امتحان کن."))
        }
    }

    private class BadModelException : Exception("bad model")

    private fun requestOnce(model: String, messages: JSONArray): Result<String> {
        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.4)
            .put("max_tokens", 500)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string()

            if (response.code == 400 || response.code == 404) {
                return Result.failure(BadModelException())
            }
            if (response.code == 401 || response.code == 403) {
                return Result.failure(Exception("کلید هوش مصنوعی معتبر نیست."))
            }
            if (response.code == 429) {
                return Result.failure(Exception("تعداد پیام‌ها زیاده. چند لحظه صبر کن و دوباره بپرس."))
            }
            if (!response.isSuccessful || text.isNullOrBlank()) {
                return Result.failure(Exception("سرویس هوش مصنوعی در دسترس نیست (کد ${response.code})."))
            }

            val choices = JSONObject(text).optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return Result.failure(Exception("پاسخی دریافت نشد. دوباره امتحان کن."))
            }
            val reply = choices.getJSONObject(0).getJSONObject("message").optString("content", "").trim()
            if (reply.isBlank()) {
                return Result.failure(Exception("پاسخی دریافت نشد. دوباره امتحان کن."))
            }
            return Result.success(reply)
        }
    }
}
