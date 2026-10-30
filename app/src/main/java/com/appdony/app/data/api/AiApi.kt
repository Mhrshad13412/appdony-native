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
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Appdony) AppleWebKit/537.36 (KHTML, like Gecko) Mobile Safari/537.36"

    /** آخرین دلیل خطا برای نمایش در چت (کد HTTP یا نوع خطای شبکه) */
    @Volatile
    var lastDebug: String = ""
        private set

    // کلید AvalAI (شروع با aa-) از GitHub Secret با اسم AI_KEY می‌آد (داخل کد و ریپو نیست)
    private val API_KEY: String = BuildConfig.AI_KEY.trim()

    private val SYSTEM_PROMPT = """
        تو «دکتر طاعون» هستی، دستیار اپلیکیشن «اپدونی» (مرجع دانلود بازی و برنامه‌ی اندروید).

        قوانین:
        - فقط به فارسی ساده و روان جواب بده. کوتاه بنویس (حداکثر ۴ جمله).
        - فقط به چیزی که کاربر پرسیده جواب بده. اگه سؤال نامفهوم بود، یک سؤال کوتاه بپرس.
        - هیچ‌وقت اسم بازی، لینک، قیمت یا نسخه از خودت نساز. فقط از «پست‌های مرتبط اپدونی» که در ادامه بهت داده می‌شه پیشنهاد بده. اگه مرتبط نبودن یا چیزی نبود، بگو با دکمه‌ی جستجوی اپ پیداش کنه.
        - اگه کاربر مشکل نصب یا اجرا داشت، مراحل ساده بگو: فعال کردن «نصب از منابع ناشناس»، فضای خالی گوشی، نسخه‌ی اندروید و پاک کردن کش.
        - لحن دوستانه و کمی شوخ داشته باش، با حداکثر یک ایموجی.
        - «اپدونی پلاس» بخش ویژه‌ی اپدونیه؛ با خرید اپدونی پلاس کاربر به بازی‌های با کیفیت دسترسی پیدا می‌کنه. قیمت یا جزئیات دیگه‌ای از خودت نساز.
        - اگه کاربر عکس فرستاد (مثلاً اسکرین‌شات خطا)، عکس رو ببین و کوتاه کمکش کن.
        - تو روی صفحه‌ی گوشی کاربر شناوری و گاهی خودت پیام می‌دی (نکته، خبر تموم شدن دانلود، پیشنهاد جستجو). اگه کاربر درباره‌ی اون‌ها پرسید، با همون‌ها هماهنگ باش.
    """.trimIndent()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // متن: اول مدل ارزون، اگه خطا یا جواب خالی بود مدل دوم
    private val TEXT_MODELS = listOf("deepseek-v4-flash", "claude-haiku-5-5")

    // عکس: فقط مدلی که تصویر می‌فهمه
    private val IMAGE_MODELS = listOf("claude-haiku-5-5")

    private class ApiException(val code: Int, message: String) : Exception(message)

    suspend fun sendMessage(
        userMessage: String,
        history: List<Pair<String, String>> = emptyList(),
        siteContext: String = "",
        assistantNotes: String = "",
        imageBase64: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (API_KEY.isBlank()) {
                lastDebug = "AI_KEY خالیه"
                return@withContext Result.failure(Exception("کلید هوش مصنوعی توی اپ تنظیم نشده."))
            }

            // همه‌ی پیام‌های system توی یکی ادغام می‌شن (برای سازگاری با همه‌ی مدل‌ها)
            val systemText = buildString {
                append(SYSTEM_PROMPT)
                if (siteContext.isNotBlank()) {
                    append("\n\nپست‌های مرتبط اپدونی (ممکنه به سؤال ربطی نداشته باشن؛ اگه نداشتن نادیده بگیر):\n")
                    append(siteContext)
                }
                if (assistantNotes.isNotBlank()) {
                    append("\n\nاین‌ها پیام‌هایی هستن که خودت (دکتر طاعون) به‌تازگی روی صفحه به کاربر نشون دادی. با همین‌ها هماهنگ جواب بده و خلافشون نگو:\n")
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
                val parts = JSONArray()
                parts.put(JSONObject().put("type", "text").put("text", userMessage))
                parts.put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64"))
                )
                userObj.put("content", parts)
            }
            messages.put(userObj)

            val models = if (imageBase64.isNullOrBlank()) TEXT_MODELS else IMAGE_MODELS
            var lastError: ApiException? = null

            for (model in models) {
                try {
                    val reply = requestOnce(API_KEY, model, messages)
                    return@withContext Result.success(reply)
                } catch (e: ApiException) {
                    lastError = e
                    lastDebug = "model=$model code=${e.code} ${e.message.orEmpty().take(80)}"
                    Log.w(TAG, "model=$model code=${e.code} msg=${e.message}")
                    when (e.code) {
                        // کلید اشتباه، اعتبار تموم یا محدودیت؛ مدل عوض کردن فایده نداره
                        401, 402, 403, 429 -> break
                        else -> continue
                    }
                }
            }

            Result.failure(Exception(describe(lastError)))
        } catch (e: UnknownHostException) {
            lastDebug = "UnknownHost"
            Result.failure(Exception("اتصال به اینترنت یا DNS مشکل داره."))
        } catch (e: SocketTimeoutException) {
            lastDebug = "Timeout"
            Result.failure(Exception("پاسخ دیر رسید. دوباره امتحان کن."))
        } catch (e: ConnectException) {
            lastDebug = "Connect: ${e.message.orEmpty().take(60)}"
            Result.failure(Exception("اتصال به سرویس هوش مصنوعی برقرار نشد. اینترنتت رو چک کن."))
        } catch (e: SSLException) {
            lastDebug = "SSL: ${e.message.orEmpty().take(60)}"
            Result.failure(Exception("اتصال امن به سرویس هوش مصنوعی برقرار نشد. اینترنتت رو چک کن."))
        } catch (e: Exception) {
            Log.w(TAG, "unexpected", e)
            lastDebug = "${e.javaClass.simpleName}: ${e.message.orEmpty().take(60)}"
            Result.failure(Exception("مشکلی پیش اومد. دوباره امتحان کن."))
        }
    }

    private fun describe(e: ApiException?): String = when (e?.code) {
        400 -> "درخواست یا عکس پذیرفته نشد (کد ۴۰۰)."
        401, 403 -> "کلید هوش مصنوعی درست نیست یا دسترسی نداره."
        402 -> "اعتبار دکتر تموم شده. کمی بعد دوباره بپرس."
        429 -> "تعداد پیام‌ها زیاده. چند لحظه صبر کن و دوباره بپرس."
        null -> "مشکلی پیش اومد. دوباره امتحان کن."
        else -> "سرویس هوش مصنوعی در دسترس نیست (کد ${e.code})."
    }

    private fun requestOnce(key: String, model: String, messages: JSONArray): String {
        // temperature عمداً فرستاده نمی‌شه؛ max_tokens بالاست چون مدل‌های DeepSeek قبل از جواب «فکر» می‌کنن
        val json = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("max_tokens", 1500)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", USER_AGENT)
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val text = response.body?.string()

            if (!response.isSuccessful) {
                throw ApiException(response.code, text.orEmpty().take(200))
            }
            if (text.isNullOrBlank()) throw ApiException(204, "empty body")

            val choices = JSONObject(text).optJSONArray("choices")
            if (choices == null || choices.length() == 0) throw ApiException(204, "no choices")

            val reply = choices.getJSONObject(0).optJSONObject("message")?.optString("content", "")?.trim().orEmpty()
            if (reply.isBlank()) throw ApiException(204, "blank reply")
            return reply
        }
    }
}
