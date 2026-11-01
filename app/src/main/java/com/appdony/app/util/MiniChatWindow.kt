package com.appdony.app.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.appdony.app.data.api.AiApi
import com.appdony.app.data.preferences.AiLimitManager
import com.appdony.app.data.preferences.AiModel
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.theme.AppdonyTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.hypot
import kotlin.math.roundToInt

private data class MiniMsg(val text: String, val isUser: Boolean)

/**
 * چت کوچیک و شناور پاپی (بیرون از برنامه).
 * - با دست از نوار بالاش جابه‌جا می‌شه
 * - با آیکن ✕ بسته می‌شه
 * - از همون سهمیه و همون مدلِ چت اصلیِ اپ استفاده می‌کنه
 */
class MiniChatWindow(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onClosed: () -> Unit
) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val limits = AiLimitManager(context)
    private val session = SessionManager(context)

    // گفتگو تا وقتی سرویس زنده‌ست می‌مونه؛ با بستن پنجره پاک نمی‌شه
    private val messages = mutableStateListOf<MiniMsg>()
    private var loading by mutableStateOf(false)

    private var frame: ChatDragFrame? = null
    private var owner: MiniOwner? = null
    private var lp: WindowManager.LayoutParams? = null
    private var sendJob: Job? = null

    val isShown: Boolean get() = frame != null

    private fun dp(v: Float) = (v * context.resources.displayMetrics.density).roundToInt()
    private val screenW get() = context.resources.displayMetrics.widthPixels
    private val screenH get() = context.resources.displayMetrics.heightPixels

    fun show(ts: ThemeState) {
        if (frame != null) return
        val o = MiniOwner().also {
            it.create()
            it.resume()
        }
        val color = ThemeManager.themeColors.getOrElse(ts.colorIndex) { ThemeManager.themeColors[0] }
        val dark = ts.mode == ThemeMode.DARK

        val f = ChatDragFrame(context, headerPx = dp(44f))
        val compose = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                AppdonyTheme(darkTheme = dark, primaryColor = color) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        MiniChatContent(
                            messages = messages,
                            loading = loading,
                            onSend = { send(it) },
                            onClose = { close() }
                        )
                    }
                }
            }
        }
        f.addView(
            compose,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        f.setViewTreeLifecycleOwner(o)
        f.setViewTreeSavedStateRegistryOwner(o)
        f.setViewTreeViewModelStoreOwner(o)

        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val w = minOf(dp(290f), screenW - dp(16f))
        val h = minOf(dp(380f), screenH - dp(80f))
        // فوکوس‌پذیره تا کیبورد کار کنه؛ لمس بیرونِ پنجره به برنامه‌ی زیرین می‌رسه
        val params = WindowManager.LayoutParams(
            w, h, type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = ((screenW - w) / 2).coerceAtLeast(0)
            y = (screenH * 0.12f).roundToInt().coerceIn(0, (screenH - h).coerceAtLeast(0))
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }

        f.onDrag = { dx, dy ->
            val p = lp
            if (p != null) {
                p.x = (p.x + dx.roundToInt()).coerceIn(0, (screenW - p.width).coerceAtLeast(0))
                p.y = (p.y + dy.roundToInt()).coerceIn(0, (screenH - p.height).coerceAtLeast(0))
                runCatching { wm.updateViewLayout(f, p) }
            }
        }

        try {
            wm.addView(f, params)
            frame = f
            owner = o
            lp = params
        } catch (_: Exception) {
            o.destroy()
            onClosed()
        }
    }

    /** پنجره رو برمی‌داره (بدون خبر دادن به سرویس) */
    fun hide() {
        sendJob?.cancel()
        sendJob = null
        loading = false
        val f = frame
        if (f != null) runCatching { wm.removeView(f) }
        frame = null
        lp = null
        owner?.destroy()
        owner = null
    }

    private fun close() {
        hide()
        onClosed()
    }

    private fun send(raw: String) {
        val text = raw.trim()
        if (text.isBlank() || loading) return
        loading = true

        // تاریخچه‌ی کوتاه قبل از اضافه شدن پیام جدید
        val history = messages.takeLast(6)
            .dropWhile { !it.isUser }
            .map { (if (it.isUser) "user" else "assistant") to it.text }
        messages.add(MiniMsg(text, true))

        sendJob = scope.launch {
            var uid = 0
            var model = AiModel.DEEPSEEK
            var begun = false
            var done = false
            try {
                uid = runCatching { session.userId.first() }.getOrDefault(0)
                model = runCatching { limits.selectedModel() }.getOrDefault(AiModel.DEEPSEEK)

                val block = runCatching { limits.quota(uid, model).blockReason() }.getOrNull()
                if (block != null) {
                    messages.add(MiniMsg("⏳ $block", false))
                    done = true
                    return@launch
                }

                runCatching { limits.begin(uid, model) }
                begun = true

                val result = withTimeoutOrNull(70_000L) {
                    AiApi.sendMessage(
                        userMessage = text,
                        model = model.id,
                        history = history,
                        assistantNotes = AssistantBus.recentNotes()
                    )
                } ?: Result.failure(Exception("پاسخ دیر رسید. دوباره امتحان کن."))

                result.onSuccess { reply ->
                    messages.add(MiniMsg(reply.text, false))
                    runCatching { limits.addTokens(uid, model, reply.totalTokens) }
                    done = true
                }.onFailure { e ->
                    messages.add(MiniMsg("⚠️ ${e.message.orEmpty().ifBlank { "خطا در ارتباط" }}", false))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                messages.add(MiniMsg("⚠️ مشکلی پیش اومد. دوباره امتحان کن.", false))
            } finally {
                withContext(NonCancellable) {
                    // درخواست ناموفق از سهم کاربر کم نمی‌شه
                    if (begun && !done) runCatching { limits.refund(uid, model) }
                    loading = false
                }
            }
        }
    }
}

// ───────────────────────── کمکی‌ها ─────────────────────────

private class MiniOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val controller = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun create() {
        controller.performAttach()
        controller.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    fun resume() {
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}

/** فقط وقتی انگشت از نوار بالا شروع به کشیدن کنه پنجره جابه‌جا می‌شه؛ ضربه‌ها (مثل ✕) به کامپوز می‌رسن */
private class ChatDragFrame(context: Context, private val headerPx: Int) : FrameLayout(context) {
    var onDrag: (Float, Float) -> Unit = { _, _ -> }

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var inHeader = false
    private var dragging = false

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                lastX = downX
                lastY = downY
                inHeader = e.y <= headerPx
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (inHeader && !dragging && hypot(e.rawX - downX, e.rawY - downY) > slop) {
                dragging = true
                lastX = e.rawX
                lastY = e.rawY
                return true
            }
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> if (dragging) {
                onDrag(e.rawX - lastX, e.rawY - lastY)
                lastX = e.rawX
                lastY = e.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> dragging = false
        }
        return dragging || super.onTouchEvent(e)
    }
}

@Composable
private fun MiniChatContent(
    messages: List<MiniMsg>,
    loading: Boolean,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, loading) {
        val last = messages.size + (if (loading) 1 else 0) - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Surface(
        modifier = Modifier.fillMaxSize().padding(4.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, primary),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // نوار بالا: بکش تا جابه‌جا شه، ✕ برای بستن
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primary.copy(alpha = 0.14f))
                    .height(40.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("🐾 چت با پاپی", color = onSurface, fontSize = 13.sp)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = onSurface, modifier = Modifier.size(18.dp))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp)
            ) {
                if (messages.isEmpty() && !loading) {
                    item(key = "hello") {
                        Text("سلام! من پاپیم 🐾 هر چی خواستی بپرس.", color = onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
                // کلید با ایندکس: پیام‌ها ممکنه متن تکراری داشته باشن
                itemsIndexed(messages, key = { i, _ -> "m$i" }) { _, m ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (m.isUser) Arrangement.Start else Arrangement.End
                    ) {
                        Text(
                            text = m.text,
                            color = if (m.isUser) MaterialTheme.colorScheme.onPrimary else onSurface,
                            fontSize = 12.5.sp,
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (m.isUser) primary else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
                if (loading) {
                    item(key = "loading") {
                        Text("در حال نوشتن…", color = onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (input.isEmpty()) {
                        Text("بنویس…", color = onSurface.copy(alpha = 0.5f), fontSize = 12.5.sp)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        textStyle = TextStyle(color = onSurface, fontSize = 12.5.sp),
                        cursorBrush = SolidColor(primary),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (loading || input.isBlank()) primary.copy(alpha = 0.35f) else primary)
                        .clickable(enabled = !loading && input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "ارسال",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
