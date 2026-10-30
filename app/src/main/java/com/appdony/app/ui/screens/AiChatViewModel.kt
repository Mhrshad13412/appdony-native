package com.appdony.app.ui.screens

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.appdony.app.data.api.AiApi
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.preferences.AiLimitManager
import com.appdony.app.data.preferences.AiModel
import com.appdony.app.data.preferences.AiQuota
import com.appdony.app.data.preferences.ChatHistoryManager
import com.appdony.app.data.preferences.StoredChatMessage
import com.appdony.app.data.session.SessionManager
import com.appdony.app.util.AssistantBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val excludeFromContext: Boolean = false,
    // عکس فقط تا بستن اپ تو حافظه می‌مونه و ذخیره نمی‌شه
    val image: Bitmap? = null
)

class AiChatViewModel(app: Application) : AndroidViewModel(app) {

    private val history = ChatHistoryManager(app)
    private val limits = AiLimitManager(app)
    private val session = SessionManager(app)

    private val welcome = ChatMessage("سلام! من پاپیم 🐾 چطور می‌تونم کمکت کنم؟", false, excludeFromContext = true)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    // مدل انتخاب‌شده و سهم مصرف کاربر برای همون مدل
    private val _model = MutableStateFlow(AiModel.DEEPSEEK)
    val model: StateFlow<AiModel> = _model

    private val _quota = MutableStateFlow<AiQuota?>(null)
    val quota: StateFlow<AiQuota?> = _quota

    // بلاک پیشنهاد برنامه‌ها
    private val _suggestions = MutableStateFlow<List<Post>>(emptyList())
    val suggestions: StateFlow<List<Post>> = _suggestions

    // عکس آماده‌ی ارسال (کوچیک‌شده) و نسخه‌ی base64 اون
    private val _pendingImage = MutableStateFlow<Bitmap?>(null)
    val pendingImage: StateFlow<Bitmap?> = _pendingImage
    private var pendingBase64: String? = null

    init {
        _messages.value = listOf(welcome)
        // چت‌های قبلی از حافظه‌ی گوشی برگردن
        viewModelScope.launch {
            val saved = try { history.load() } catch (e: Exception) { emptyList() }
            if (saved.isNotEmpty() && _messages.value.size <= 1) {
                _messages.value = saved.map { ChatMessage(it.text, it.isUser, it.excludeFromContext) }
            }
        }
        viewModelScope.launch {
            _model.value = try { limits.selectedModel() } catch (e: Exception) { AiModel.DEEPSEEK }
            refreshQuota()
        }
        loadSuggestions()
    }

    private suspend fun currentUid(): Int = try { session.userId.first() } catch (e: Exception) { 0 }

    fun refreshQuota() {
        viewModelScope.launch {
            _quota.value = try { limits.quota(currentUid(), _model.value) } catch (e: Exception) { null }
        }
    }

    fun selectModel(m: AiModel) {
        if (_isLoading.value) return
        _model.value = m
        viewModelScope.launch {
            try { limits.setSelectedModel(m) } catch (e: Exception) { }
            _quota.value = try { limits.quota(currentUid(), m) } catch (e: Exception) { null }
        }
    }

    fun loadSuggestions() {
        viewModelScope.launch {
            _suggestions.value = try {
                ApiClient.api.getPosts(perPage = 20).shuffled().take(6)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun persist() {
        val snapshot = _messages.value.map {
            val t = if (it.text.isBlank() && it.image != null) "📷 عکس" else it.text
            StoredChatMessage(t, it.isUser, it.excludeFromContext)
        }
        viewModelScope.launch { try { history.saveChatHistory(snapshot) } catch (e: Exception) { } }
    }

    /** عکس انتخاب‌شده رو کوچیک می‌کنه (حداکثر ۱۰۲۴ پیکسل) و آماده‌ی ارسال می‌ذاره */
    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try { loadImage(uri) } catch (e: Throwable) { null }
            }
            if (result == null) {
                Toast.makeText(getApplication(), "عکس خونده نشد", Toast.LENGTH_SHORT).show()
            } else {
                _pendingImage.value = result.first
                pendingBase64 = result.second
            }
        }
    }

    fun removePendingImage() {
        _pendingImage.value = null
        pendingBase64 = null
    }

    private fun loadImage(uri: Uri): Pair<Bitmap, String>? {
        val cr = getApplication<Application>().contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

        val rotation = try {
            cr.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }

        val matrix = Matrix()
        if (rotation != 0f) matrix.postRotate(rotation)
        val longest = maxOf(decoded.width, decoded.height)
        if (longest > 1024) {
            val s = 1024f / longest
            matrix.postScale(s, s)
        }
        val bitmap = if (matrix.isIdentity) decoded
        else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        return bitmap to b64
    }

    fun sendMessage(userMessage: String) {
        val text = userMessage.trim()
        val image = _pendingImage.value
        val b64 = pendingBase64
        if ((text.isBlank() && image == null) || _isLoading.value) return

        _isLoading.value = true

        viewModelScope.launch {
            val uid = currentUid()
            // دیپ‌سیک عکس نمی‌بینه؛ برای عکس خودکار هایکو استفاده می‌شه و از سهم هایکو کم می‌شه
            val useModel = if (b64 != null && !_model.value.supportsImage) AiModel.HAIKU else _model.value

            // بررسی نهایی سهم قبل از ارسال
            val block = try { limits.quota(uid, useModel).blockReason() } catch (e: Exception) { null }
            if (block != null) {
                _messages.value = _messages.value + ChatMessage("⏳ $block", false, excludeFromContext = true)
                _quota.value = try { limits.quota(uid, _model.value) } catch (e: Exception) { null }
                _isLoading.value = false
                persist()
                return@launch
            }

            val currentMessages = _messages.value + ChatMessage(text, true, image = image)
            _messages.value = currentMessages
            removePendingImage()

            // ثبت پیام؛ اولین پیام بازه، شروع ۵ ساعت رو ثبت می‌کنه
            try { limits.begin(uid, useModel) } catch (e: Exception) { }

            val ctxHistory = currentMessages
                .dropLast(1)
                .filter { !it.excludeFromContext && it.text.isNotBlank() }
                .takeLast(8)
                .dropWhile { !it.isUser }
                .map { (if (it.isUser) "user" else "assistant") to it.text }

            val context = if (b64 != null) "" else loadSiteContext(text)
            val apiText = text.ifBlank { "این عکس رو ببین و کمکم کن." }
            val result = AiApi.sendMessage(
                userMessage = apiText,
                model = useModel.id,
                history = ctxHistory,
                siteContext = context,
                assistantNotes = AssistantBus.recentNotes(),
                imageBase64 = b64
            )

            result.onSuccess { reply ->
                _isOffline.value = false
                _messages.value = _messages.value + ChatMessage(reply.text, false)
                try { limits.addTokens(uid, useModel, reply.totalTokens) } catch (e: Exception) { }
            }.onFailure { error ->
                _isOffline.value = true
                // درخواست ناموفق از سهم کاربر کم نمی‌شه
                try { limits.refund(uid, useModel) } catch (e: Exception) { }
                val reply = LocalDoctor.reply(text) +
                    "\n\n⚠️ ${error.message.orEmpty()}"
                _messages.value = _messages.value + ChatMessage(reply, false, excludeFromContext = true)
            }

            _quota.value = try { limits.quota(uid, _model.value) } catch (e: Exception) { null }
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
        removePendingImage()
        loadSuggestions()
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
                "سلام رفیق! 👋 من پاپیم. می‌تونم بازی پیشنهاد بدم یا مشکل نصب و دانلودت رو حل کنم."

            has(text, "ممنون", "مرسی", "تشکر", "دمت گرم", "thanks") ->
                "قابلی نداشت! هر وقت نسخه‌ای لازم داشتی من همین‌جام 😉"

            has(text, "کی هستی", "تو کی", "اسمت", "کیستی", "who are you") ->
                "من پاپی، دستیار اپدونیم. بازی و برنامه پیدا می‌کنم و مشکل نصب رو حل می‌کنم 🐾"

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
