package com.appdony.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** تنظیم‌های سقف مصرف؛ همه‌ی عددها رو از همین‌جا می‌تونی عوض کنی */
object AiLimits {
    /** طول هر بازه؛ از لحظه‌ی اولین پیام شروع می‌شه */
    const val WINDOW_MS = 5L * 60 * 60 * 1000

    /** نرخ تقریبی دلار به تومان برای محاسبه‌ی هزینه */
    const val USD_TO_TOMAN = 100_000.0

    fun costToman(model: AiModel, promptTokens: Int, completionTokens: Int): Double =
        (promptTokens * model.usdPerMInput + completionTokens * model.usdPerMOutput) / 1_000_000.0 * USD_TO_TOMAN
}

enum class AiModel(
    val id: String,
    val label: String,
    val hint: String,
    /** حداکثر پیام در هر بازه‌ی ۵ ساعته */
    val windowMessages: Int,
    /** حداکثر توکن واقعی در هر بازه‌ی ۵ ساعته */
    val windowTokens: Int,
    /** حداکثر پیام در روز */
    val dailyMessages: Int,
    /** قیمت هر یک میلیون توکن ورودی/خروجی به دلار (تقریبی، با قیمت آوالای چک کن) */
    val usdPerMInput: Double,
    val usdPerMOutput: Double,
    val supportsImage: Boolean
) {
    DEEPSEEK("deepseek-v4-flash", "دیپ‌سیک", "سریع و سبک", 20, 30_000, 60, 0.25, 0.38, false),
    HAIKU("claude-haiku-5-5", "کلود هایکو", "دقیق‌تر؛ عکس هم می‌بینه", 10, 20_000, 30, 0.90, 4.50, true);

    companion object {
        fun fromId(id: String?): AiModel = values().firstOrNull { it.id == id } ?: DEEPSEEK
    }
}

data class AiQuota(
    val model: AiModel,
    val usedMessages: Int,
    val usedTokens: Int,
    val dailyUsed: Int,
    /** زمان ریست بازه؛ صفر یعنی هنوز بازه‌ای شروع نشده */
    val windowResetAt: Long
) {
    /** اگه کاربر نمی‌تونه پیام بده، دلیلش رو برمی‌گردونه؛ وگرنه null */
    fun blockReason(now: Long = System.currentTimeMillis()): String? {
        if (dailyUsed >= model.dailyMessages) {
            return "سهم امروزت برای ${model.label} تموم شد. فردا دوباره بیا یا مدل دیگه رو انتخاب کن."
        }
        val windowActive = windowResetAt > now
        if (windowActive && (usedMessages >= model.windowMessages || usedTokens >= model.windowTokens)) {
            return "سقف ${model.label} برای این بازه پر شده. ${formatRemaining(windowResetAt - now)} دیگه دوباره بپرس یا مدل دیگه رو انتخاب کن."
        }
        return null
    }
}

fun formatRemaining(ms: Long): String {
    val totalMin = (ms / 60_000).coerceAtLeast(1)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "$h ساعت و $m دقیقه"
        h > 0 -> "$h ساعت"
        else -> "$m دقیقه"
    }
}

private val Context.aiLimitStore by preferencesDataStore(name = "ai_limits")

/** مصرف هر کاربر (جدا برای هر مدل) روی گوشی ذخیره می‌شه */
class AiLimitManager(private val context: Context) {

    private companion object {
        val KEY_MODEL = stringPreferencesKey("selected_model")
    }

    private data class Raw(
        val ws: Long = 0,      // شروع بازه
        val wc: Int = 0,       // پیام‌های بازه
        val wt: Int = 0,       // توکن‌های بازه
        val day: String = "",  // تاریخ روز
        val dc: Int = 0        // پیام‌های امروز
    ) {
        fun encode() = "$ws|$wc|$wt|$day|$dc"
    }

    private fun key(uid: Int, model: AiModel) = stringPreferencesKey("q_${uid}_${model.id}")

    private fun decode(s: String?): Raw = try {
        val p = s!!.split("|")
        Raw(p[0].toLong(), p[1].toInt(), p[2].toInt(), p[3], p[4].toInt())
    } catch (e: Exception) {
        Raw()
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun fresh(r: Raw, now: Long): Raw {
        var x = r
        if (x.ws > 0 && now - x.ws >= AiLimits.WINDOW_MS) x = x.copy(ws = 0, wc = 0, wt = 0)
        if (x.day != today()) x = x.copy(day = today(), dc = 0)
        return x
    }

    suspend fun quota(uid: Int, model: AiModel): AiQuota {
        val now = System.currentTimeMillis()
        val r = fresh(decode(context.aiLimitStore.data.first()[key(uid, model)]), now)
        return AiQuota(
            model = model,
            usedMessages = r.wc,
            usedTokens = r.wt,
            dailyUsed = r.dc,
            windowResetAt = if (r.ws > 0) r.ws + AiLimits.WINDOW_MS else 0L
        )
    }

    /** قبل از ارسال صدا زده می‌شه؛ اولین پیام بازه، ساعت شروع رو ثبت می‌کنه */
    suspend fun begin(uid: Int, model: AiModel) {
        val now = System.currentTimeMillis()
        val k = key(uid, model)
        context.aiLimitStore.edit { p ->
            val r = fresh(decode(p[k]), now)
            p[k] = r.copy(ws = if (r.ws == 0L) now else r.ws, wc = r.wc + 1, dc = r.dc + 1).encode()
        }
    }

    suspend fun addTokens(uid: Int, model: AiModel, tokens: Int) {
        val now = System.currentTimeMillis()
        val k = key(uid, model)
        context.aiLimitStore.edit { p ->
            val r = fresh(decode(p[k]), now)
            p[k] = r.copy(wt = r.wt + tokens.coerceAtLeast(0)).encode()
        }
    }

    /** اگه درخواست شکست خورد، پیام از سهم کاربر کم نمی‌شه */
    suspend fun refund(uid: Int, model: AiModel) {
        val now = System.currentTimeMillis()
        val k = key(uid, model)
        context.aiLimitStore.edit { p ->
            val r = fresh(decode(p[k]), now)
            val wc = (r.wc - 1).coerceAtLeast(0)
            val dc = (r.dc - 1).coerceAtLeast(0)
            val ws = if (wc == 0 && r.wt == 0) 0L else r.ws
            p[k] = r.copy(ws = ws, wc = wc, dc = dc).encode()
        }
    }

    suspend fun selectedModel(): AiModel =
        AiModel.fromId(context.aiLimitStore.data.first()[KEY_MODEL])

    suspend fun setSelectedModel(model: AiModel) {
        context.aiLimitStore.edit { it[KEY_MODEL] = model.id }
    }
}
