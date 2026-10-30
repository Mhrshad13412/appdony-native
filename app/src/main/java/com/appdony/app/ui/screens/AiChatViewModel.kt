package com.appdony.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.appdony.app.data.preferences.ChatHistoryManager
import com.appdony.app.data.preferences.StoredChatMessage
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.api.AiApi
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.util.AssistantBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val excludeFromContext: Boolean = false
)

class AiChatViewModel(app: Application) : AndroidViewModel(app) {

    private val history = ChatHistoryManager(app)

    private val welcome = ChatMessage("سلام! من دکتر طاعونم 🩺 چطور می‌تونم کمکت کنم؟", false, excludeFromContext = true)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    init {
        _messages.value = listOf(welcome)
        // چت‌های قبلی از حافظه‌ی گوشی برگردن
        viewModelScope.launch {
            val saved = try { history.load() } catch (e: Exception) { emptyList() }
            if (saved.isNotEmpty() && _messages.value.size <= 1) {
                _messages.value = saved.map { ChatMessage(it.text, it.isUser, it.excludeFromContext) }
            }
        }
    }

    private fun persist() {
        val snapshot = _messages.value.map { StoredChatMessage(it.text, it.isUser, it.excludeFromContext) }
        viewModelScope.launch { try { history.saveChatHistory(snapshot) } catch (e: Exception) { } }
    }

    fun sendMessage(userMessage: String) {
        val text = userMessage.trim()
        if (text.isBlank() || _isLoading.value) return

        val currentMessages = _messages.value + ChatMessage(text, true)
        _messages.value = currentMessages
        _isLoading.value = true

        viewModelScope.launch {
            val ctxHistory = currentMessages
                .dropLast(1)
                .filter { !it.excludeFromContext }
                .takeLast(8)
                .dropWhile { !it.isUser }
                .map { (if (it.isUser) "user" else "assistant") to it.text }

            val context = loadSiteContext(text)
            val result = AiApi.sendMessage(text, ctxHistory, context, AssistantBus.recentNotes())

            result.onSuccess { response ->
                _isOffline.value = false
                _messages.value = _messages.value + ChatMessage(response, false)
            }.onFailure { error ->
                _isOffline.value = true
                val reply = LocalDoctor.reply(text) +
                    "\n\n⚠️ ${error.message.orEmpty()}"
                _messages.value = _messages.value + ChatMessage(reply, false, excludeFromContext = true)
            }
            _isLoading.value = false
            persist()
        }
    }

    private suspend fun loadSiteContext(query: String): String {
        if (query.length < 4) return ""
        return try {
            ApiClient.api.searchPosts(query = query.take(60), perPage = 4)
                .joinToString("\n") { "- " + it.cleanTitle() }
        } catch (e: Exception) {
            ""
        }
    }

    fun clearChat() {
        _messages.value = listOf(welcome)
        viewModelScope.launch { try { history.clearHistory() } catch (e: Exception) { } }
    }
}

private object LocalDoctor {

    private val stopWords = setOf(
        "یه", "یک", "بازی", "بازیه", "برنامه", "پیشنهاد", "بده", "بهم", "بدی", "میشه", "میخوام", "می‌خوام",
        "یکی", "خوب", "خوبی", "جدید", "لطفا", "لطفاً", "رو", "را", "از", "برای", "یه‌", "خوشگل", "باحال", "بهترین",
        "چی", "چیه", "کدوم", "کدومه", "هست", "سبک", "نوع", "اندروید"
    )

    suspend fun reply(raw: String): String {
        val text = normalize(raw)

        return when {
            has(text, "سلام", "درود") || text == "hi" || text.startsWith("hello") ->
                "سلام رفیق! 👋 من دکتر طاعونم. می‌تونم بازی پیشنهاد بدم یا مشکل نصب و دانلودت رو حل کنم."

            has(text, "ممنون", "مرسی", "تشکر", "دمت گرم", "thanks") ->
                "قابلی نداشت! هر وقت نسخه‌ای لازم داشتی من همین‌جام 😉"

            has(text, "کی هستی", "تو کی", "اسمت", "کیستی", "who are you") ->
                "من دکتر طاعون، دستیار اپدونیم. نسخه‌ی بازی و برنامه می‌پیچم و مشکل نصب رو درمان می‌کنم 🩺"

            has(text, "نصب نمی", "نصب نم", "نصب نشد", "نصب نمیشه", "باز نمی", "اجرا نمی", "ارور", "خطا", "کرش", "پارس", "parse") ->
                "برای مشکل نصب این‌ها رو چک کن:\n" +
                    "۱) تنظیمات ← امنیت ← «نصب از منابع ناشناس» رو برای مرورگر/مدیر فایل فعال کن.\n" +
                    "۲) فضای خالی گوشی حداقل ۲ برابر حجم فایل باشه.\n" +
                    "۳) نسخه‌ی اندروید با نیازمندی بازی بخونه.\n" +
                    "۴) نسخه‌ی قبلی رو پاک کن و کش رو خالی کن، بعد دوباره نصب کن."

            has(text, "دانلود نمی", "دانلود نم", "قطع", "کند", "دانلود نشد", "لینک") ->
                "اگه دانلود گیر کرده: اینترنت/VPN رو عوض کن، توی تنظیمات اپ گزینه‌ی «فقط وای‌فای» رو چک کن و از تب «دانلودها» دوباره شروعش کن."

            has(text, "پیشنهاد", "چی بازی", "بازی کنم", "یه بازی", "یک بازی", "بازی خوب", "برنامه خوب", "معرفی") ->
                suggest(text)

            else -> {
                val found = search(text)
                if (found.isNotEmpty()) {
                    "این‌ها رو توی اپدونی پیدا کردم:\n" + found.joinToString("\n") { "• ${shortTitle(it)}" } +
                        "\nاز دکمه‌ی جستجوی بالای صفحه بازشون کن 😉"
                } else {
                    "الان نمی‌تونم به مغز هوشمندم وصل بشم، ولی می‌تونم: بازی پیشنهاد بدم، مشکل نصب/دانلود رو حل کنم یا چیزی رو توی اپدونی برات پیدا کنم. کدوم؟"
                }
            }
        }
    }

    private suspend fun suggest(text: String): String {
        val keywords = keywordsOf(text)
        var posts: List<Post> = if (keywords.isNotBlank()) searchSafe(keywords) else emptyList()
        if (posts.isEmpty()) posts = latestSafe()
        if (posts.isEmpty()) {
            return "الان به سرور اپدونی وصل نشدم. اینترنتت رو چک کن یا یه بار از تب «بازی‌ها» سر بزن 🎮"
        }
        val picks = posts.shuffled().take(3)
        return "این‌ها رو برات انتخاب کردم 🎮\n" +
            picks.joinToString("\n") { "• ${shortTitle(it.cleanTitle())}" } +
            "\nاسمش رو توی جستجوی اپ بزن تا صفحه‌ی دانلودش باز بشه."
    }

    private suspend fun search(text: String): List<String> {
        val keywords = keywordsOf(text)
        if (keywords.length < 3) return emptyList()
        return searchSafe(keywords).take(3).map { it.cleanTitle() }
    }

    private suspend fun searchSafe(query: String): List<Post> = try {
        ApiClient.api.searchPosts(query = query.take(60), perPage = 8)
    } catch (e: Exception) {
        emptyList()
    }

    private suspend fun latestSafe(): List<Post> = try {
        ApiClient.api.getPosts(perPage = 12)
    } catch (e: Exception) {
        emptyList()
    }

    private fun keywordsOf(text: String): String =
        text.split(" ", "،", ",", "؟", "?", "!", ".")
            .map { it.trim() }
            .filter { it.length > 1 && it !in stopWords }
            .joinToString(" ")

    private fun shortTitle(title: String): String {
        val quoted = Regex("[“”\"«»](.+?)[“”\"«»]").find(title)?.groupValues?.getOrNull(1)?.trim()
        if (!quoted.isNullOrBlank()) return quoted
        return title.removePrefix("دانلود").trim().take(50)
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('ي', 'ی')
            .replace('ك', 'ک')
            .replace('\u200c', ' ')
            .trim()

    private fun has(text: String, vararg words: String): Boolean = words.any { text.contains(it) }
}
