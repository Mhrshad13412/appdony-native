package com.appdony.app.data.api

import android.content.Context
import com.appdony.app.data.preferences.AiLimitManager
import com.appdony.app.data.preferences.AiModel
import com.appdony.app.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * یک درخواست تکی به هوش مصنوعی (بدون شخصیت پاپی) با همون سهمیه و همون مدلی که کاربر توی چت انتخاب کرده.
 * مثل چت شناور: قبل از ارسال سهم چک می‌شه و اگه درخواست شکست بخوره از سهم کاربر کم نمی‌شه.
 */
object AiRunner {

    private suspend fun <T> safely(fallback: T, block: suspend () -> T): T =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallback
        }

    suspend fun ask(
        context: Context,
        system: String,
        user: String,
        maxTokens: Int = 2500
    ): Result<String> {
        val app = context.applicationContext
        val limits = AiLimitManager(app)
        var uid = 0
        var model = AiModel.DEEPSEEK
        var begun = false
        var done = false
        try {
            uid = safely(0) { SessionManager(app).userId.first() }
            model = safely(AiModel.DEEPSEEK) { limits.selectedModel() }

            val block = safely<String?>(null) { limits.quota(uid, model).blockReason() }
            if (block != null) return Result.failure(Exception(block))

            safely(Unit) { limits.begin(uid, model) }
            begun = true

            val result = withTimeoutOrNull(75_000L) {
                AiApi.sendMessage(
                    userMessage = user,
                    model = model.id,
                    systemOverride = system,
                    maxTokens = maxTokens
                )
            } ?: Result.failure(Exception("پاسخ دیر رسید. دوباره امتحان کن."))

            val reply = result.getOrElse { return Result.failure(it) }
            safely(Unit) { limits.addTokens(uid, model, reply.totalTokens) }
            done = true
            return Result.success(reply.text)
        } finally {
            withContext(NonCancellable) {
                if (begun && !done) safely(Unit) { limits.refund(uid, model) }
            }
        }
    }
}
