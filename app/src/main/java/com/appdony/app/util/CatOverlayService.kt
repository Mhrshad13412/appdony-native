package com.appdony.app.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.appdony.app.MainActivity
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.ui.components.AvatarMotion
import com.appdony.app.ui.components.AvatarShape
import com.appdony.app.ui.components.AvatarSize
import com.appdony.app.ui.components.CatPose
import com.appdony.app.ui.components.SpeechBubble
import com.appdony.app.ui.components.YarnButton
import com.appdony.app.ui.components.drawCat
import com.appdony.app.ui.components.yarnPalette
import com.appdony.app.ui.theme.AppdonyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * پاپیِ بیرون از برنامه.
 * پیش‌فرض خاموشه؛ فقط وقتی کاربر از تنظیمات روشنش کنه (و اجازه‌ی نمایش روی سایر برنامه‌ها رو بده) بالا میاد.
 * بیرون از برنامه حرف نمی‌زنه؛ فقط خودش، دکمه‌های منو و «چت با من» رو داره.
 * صرفه‌جویی باتری:
 *  - فقط وقتی صفحه روشنه و اپ جلوی کاربر نیست، پنجره وجود داره (وگرنه کلاً حذف می‌شه و هیچ انیمیشنی اجرا نمی‌شه)
 *  - بعد از ۱۵ ثانیه بی‌کاری می‌خوابه (تا نصفه می‌ره پشت لبه و کم‌رنگ می‌شه) و همه‌ی حلقه‌های انیمیشن کامل متوقف می‌شن
 *  - توی حالت «بهینه‌سازی باتری» و زیر ۱۵٪ شارژ، انیمیشن مداوم یا خود پاپی خاموشه
 *  - شبکه، wakelock و تایمر پس‌زمینه نداره
 */
class CatOverlayService : Service() {

    companion object {
        private const val PREFS = "cat_overlay"
        private const val KEY_ON = "enabled"
        private const val KEY_X = "x"
        private const val KEY_Y = "y"
        private const val KEY_RIGHT = "right"
        private const val CHANNEL = "cat_overlay"
        private const val NOTIF_ID = 4711
        const val ACTION_STOP = "com.appdony.app.CAT_STOP"

        // پیش‌فرض خاموش: تا کاربر خودش روشن نکنه (و دسترسی نده) پاپی بیرونی بالا نمیاد
        fun isEnabled(c: Context): Boolean =
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, false)

        fun canDraw(c: Context): Boolean =
            Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c)

        fun permissionIntent(c: Context): Intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))

        fun setEnabled(c: Context, on: Boolean) {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply()
            if (on) startIfEnabled(c) else c.stopService(Intent(c, CatOverlayService::class.java))
        }

        fun startIfEnabled(c: Context) {
            if (!isEnabled(c) || !canDraw(c)) return
            try {
                ContextCompat.startForegroundService(c, Intent(c, CatOverlayService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ui = OverlayUi()
    private lateinit var wm: WindowManager
    private lateinit var pm: PowerManager

    private var root: DragFrame? = null
    private var owner: OverlayOwner? = null
    private var params: WindowManager.LayoutParams? = null
    private var attaching = false
    private var chatOpen = false
    private var chat: MiniChatWindow? = null

    private var screenOn = true
    private var lowBattery = false
    private var expandedWin = false
    private var catX = 0f
    private var catY = 0f

    // وقتی پاپی خوابه، تا نصفه می‌ره پشت لبه‌ی صفحه (مثل داخل برنامه) و کم‌رنگ می‌شه
    private var hideShift = 0f
    private var hideAnim: ValueAnimator? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> screenOn = false
                Intent.ACTION_SCREEN_ON -> screenOn = true
                Intent.ACTION_BATTERY_LOW -> lowBattery = true
                Intent.ACTION_BATTERY_OKAY -> lowBattery = false
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> ui.active = !pm.isPowerSaveMode
            }
            refresh()
        }
    }

    private fun dp(v: Float) = (v * resources.displayMetrics.density).roundToInt()
    private val screenW get() = resources.displayMetrics.widthPixels
    private val screenH get() = resources.displayMetrics.heightPixels
    private val catPx get() = dp(60f)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        startAsForeground()
        if (!canDraw(this)) {
            stopSelf()
            return
        }

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        catX = prefs.getFloat(KEY_X, dp(12f).toFloat())
        catY = prefs.getFloat(KEY_Y, screenH * 0.6f)
        ui.onRight = prefs.getBoolean(KEY_RIGHT, false)
        ui.lowerHalf = catY + catPx / 2f > screenH / 2f
        ui.active = !pm.isPowerSaveMode
        screenOn = pm.isInteractive

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch { AppForeground.inForeground.collect { refresh() } }
        refresh()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            setEnabled(this, false)
            stopSelf()
        }
        // اگه سیستم یا کاربر پروسه رو بست، سیستم دوباره سرویس رو بالا میاره
        return START_STICKY
    }

    // کاربر اپ رو از لیست اخیر پاک کرد: پاپی بمونه
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        startIfEnabled(applicationContext)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        catX = catX.coerceIn(0f, (screenW - catPx).toFloat().coerceAtLeast(0f))
        catY = catY.coerceIn(0f, (screenH - catPx).toFloat().coerceAtLeast(0f))
        applyLayout()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(receiver) }
        closeChat()
        detach()
        scope.cancel()
        super.onDestroy()
    }

    // ───────── نوتیفیکیشن سرویس ─────────

    private fun startAsForeground() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "پاپی همراه", NotificationManager.IMPORTANCE_MIN)
            )
        }
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, CatOverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle("پاپی همراهته")
            .setContentText("برای خاموش کردن، دکمه‌ی زیر رو بزن")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .addAction(0, "خاموش کردن", stop)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    // ───────── نمایش / حذف پنجره ─────────

    private fun shouldShow() =
        canDraw(this) && screenOn && !lowBattery && !AppForeground.inForeground.value && !chatOpen

    private fun refresh() {
        // چت کوچیک فقط بیرون از برنامه و وقتی صفحه روشنه معنی داره
        if (chatOpen && (AppForeground.inForeground.value || !screenOn || !canDraw(this))) closeChat()
        if (shouldShow()) {
            if (root == null && !attaching) {
                attaching = true
                scope.launch {
                    val ts = runCatching { ThemeManager(applicationContext).state.first() }
                        .getOrDefault(ThemeState())
                    attaching = false
                    if (shouldShow() && root == null) attach(ts)
                }
            }
        } else {
            detach()
        }
    }

    private fun attach(ts: ThemeState) {
        val o = OverlayOwner().also {
            it.create()
            it.resume()
        }
        val frame = DragFrame(this)
        val color = ThemeManager.themeColors.getOrElse(ts.colorIndex) { ThemeManager.themeColors[0] }
        val compose = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                OverlayCat(
                    ui = ui,
                    dark = ts.mode == ThemeMode.DARK,
                    primaryColor = color,
                    onExpand = { onExpand(it) },
                    onSleep = { onSleep(it) },
                    onOpen = { openApp(it) },
                    onOpenChat = { openChat() }
                )
            }
        }
        frame.addView(
            compose,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )
        frame.setViewTreeLifecycleOwner(o)
        frame.setViewTreeSavedStateRegistryOwner(o)
        frame.setViewTreeViewModelStoreOwner(o)

        frame.onDragStart = {
            ui.dragging = true
            resetHide()
            ui.collapseSignal++
            if (expandedWin) {
                expandedWin = false
                applyLayout()
            }
        }
        frame.onDragMove = { dx, dy ->
            catX = (catX + dx).coerceIn(0f, (screenW - catPx).toFloat().coerceAtLeast(0f))
            catY = (catY + dy).coerceIn(0f, (screenH - catPx).toFloat().coerceAtLeast(0f))
            applyLayout()
        }
        frame.onDragEnd = {
            ui.dragging = false
            dock()
        }
        frame.onOutside = { ui.collapseSignal++ }

        val type = if (Build.VERSION.SDK_INT >= 26) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        val lp = WindowManager.LayoutParams(
            catPx + dp(16f), catPx + dp(16f), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        owner = o
        params = lp
        root = frame
        expandedWin = false
        applyLayout(addIfNeeded = true)
    }

    private fun detach() {
        val r = root ?: return
        runCatching { wm.removeView(r) }
        root = null
        params = null
        expandedWin = false
        resetHide()
        owner?.destroy()
        owner = null
    }

    private fun applyLayout(addIfNeeded: Boolean = false) {
        val lp = params ?: return
        val r = root ?: return
        val pad = dp(8f)
        val cat = catPx
        val w = if (expandedWin) dp(300f) else cat + 2 * pad
        val h = if (expandedWin) dp(190f) else cat + 2 * pad
        val xRaw = when {
            !expandedWin -> catX.roundToInt() - pad
            ui.onRight -> catX.roundToInt() + cat + pad - w
            else -> catX.roundToInt() - pad
        }
        val yRaw = when {
            !expandedWin -> catY.roundToInt() - pad
            ui.lowerHalf -> catY.roundToInt() + cat + pad - h
            else -> catY.roundToInt() - pad
        }
        lp.width = w
        lp.height = h
        lp.x = xRaw.coerceIn(0, (screenW - w).coerceAtLeast(0)) +
            (if (expandedWin) 0 else hideShift.roundToInt())
        lp.y = yRaw.coerceIn(0, (screenH - h).coerceAtLeast(0))
        try {
            if (addIfNeeded) wm.addView(r, lp) else wm.updateViewLayout(r, lp)
        } catch (_: Exception) {
            if (addIfNeeded) {
                root = null
                params = null
                owner?.destroy()
                owner = null
            }
        }
    }

    private fun onExpand(expanded: Boolean) {
        if (expanded) resetHide()
        if (expandedWin != expanded && root != null) {
            expandedWin = expanded
            applyLayout()
        }
    }

    private fun resetHide() {
        hideAnim?.cancel()
        hideAnim = null
        hideShift = 0f
    }

    /** خواب: آروم تا نصفه می‌ره پشت لبه‌ی نزدیک؛ بیداری: برمی‌گرده سر جاش */
    private fun onSleep(asleep: Boolean) {
        if (root == null) return
        hideAnim?.cancel()
        val side = if (ui.onRight) 1f else -1f
        val target = if (asleep && !expandedWin && !ui.dragging) dp(43f) * side else 0f
        val from = hideShift
        hideAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 450
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                hideShift = from + (target - from) * (it.animatedValue as Float)
                applyLayout()
            }
            start()
        }
    }

    // ───────── چسبیدن به لبه‌ی صفحه ─────────

    private fun dock() {
        val margin = dp(12f)
        val right = catX + catPx / 2f > screenW / 2f
        val tx = if (right) (screenW - catPx - margin).toFloat() else margin.toFloat()
        val sx = catX
        ui.onRight = right
        ui.lowerHalf = catY + catPx / 2f > screenH / 2f
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                catX = sx + (tx - sx) * (it.animatedValue as Float)
                applyLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ui.landTick++
                    getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putFloat(KEY_X, catX).putFloat(KEY_Y, catY).putBoolean(KEY_RIGHT, ui.onRight).apply()
                }
            })
            start()
        }
    }

    // ───────── چت کوچیک شناور ─────────

    private fun openChat() {
        if (chatOpen || !canDraw(this)) return
        chatOpen = true
        detach() // گربه تا بسته شدن چت قایم می‌شه
        scope.launch {
            val ts = runCatching { ThemeManager(applicationContext).state.first() }
                .getOrDefault(ThemeState())
            if (!chatOpen) return@launch
            val w = chat ?: MiniChatWindow(applicationContext, scope) {
                // کاربر ✕ رو زد: چت بسته شد، گربه برگرده
                chatOpen = false
                refresh()
            }.also { chat = it }
            w.show(ts)
        }
    }

    private fun closeChat() {
        chatOpen = false
        chat?.hide()
    }

    private fun openApp(route: String?) {
        try {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("route", route)
            )
        } catch (_: Exception) {
        }
    }
}

// ───────────────────────── کمکی‌ها ─────────────────────────

private class OverlayUi {
    var onRight by mutableStateOf(false)
    var lowerHalf by mutableStateOf(true)
    var dragging by mutableStateOf(false)
    var active by mutableStateOf(true)          // false = حالت بهینه‌سازی باتری، بدون حلقه‌ی انیمیشن
    var collapseSignal by mutableIntStateOf(0)  // کلیک بیرون یا شروع کشیدن: منو و بالن بسته بشه
    var landTick by mutableIntStateOf(0)        // فرود نرم بعد از چسبیدن به لبه
}

private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
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

/** کشیدن پنجره با انگشت؛ ضربه‌ها به خود کامپوز می‌رسن */
private class DragFrame(context: Context) : FrameLayout(context) {
    var onDragStart: () -> Unit = {}
    var onDragMove: (Float, Float) -> Unit = { _, _ -> }
    var onDragEnd: () -> Unit = {}
    var onOutside: () -> Unit = {}

    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var dragging = false

    override fun dispatchTouchEvent(e: MotionEvent): Boolean {
        if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) {
            onOutside()
            return false
        }
        return super.dispatchTouchEvent(e)
    }

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = e.rawX
                downY = e.rawY
                lastX = downX
                lastY = downY
                dragging = false
            }
            MotionEvent.ACTION_MOVE -> if (!dragging && hypot(e.rawX - downX, e.rawY - downY) > slop) {
                dragging = true
                lastX = e.rawX
                lastY = e.rawY
                onDragStart()
                return true
            }
        }
        return dragging
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_MOVE -> if (dragging) {
                onDragMove(e.rawX - lastX, e.rawY - lastY)
                lastX = e.rawX
                lastY = e.rawY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragging) {
                dragging = false
                onDragEnd()
            }
        }
        return dragging || super.onTouchEvent(e)
    }
}

// ───────────────────────── خود پاپی ─────────────────────────

@Composable
private fun OverlayCat(
    ui: OverlayUi,
    dark: Boolean,
    primaryColor: Long,
    onExpand: (Boolean) -> Unit,
    onSleep: (Boolean) -> Unit,
    onOpen: (String?) -> Unit,
    onOpenChat: () -> Unit
) {
    AppdonyTheme(darkTheme = dark, primaryColor = primaryColor) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            CatBody(ui, onExpand, onSleep, onOpen, onOpenChat)
        }
    }
}

@Composable
private fun CatBody(
    ui: OverlayUi,
    onExpand: (Boolean) -> Unit,
    onSleep: (Boolean) -> Unit,
    onOpen: (String?) -> Unit,
    onOpenChat: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val yarn = remember(primary) { yarnPalette(primary) }
    val motion = remember { AvatarMotion() }

    var menuOpen by remember { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<AssistantSay?>(null) }
    var lastSay by remember { mutableStateOf(AssistantSay("")) }
    var asleep by remember { mutableStateOf(false) }
    var activity by remember { mutableIntStateOf(0) }

    val bob = remember { Animatable(0f) }
    val tailA = remember { Animatable(0f) }
    val talk = remember { Animatable(0f) }
    val animate = ui.active && !asleep
    val menuVisible = menuOpen || motion.menuProg.any { it.value > 0.01f }

    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با من", "ai_chat"),
        Triple(Icons.Outlined.Download, "دانلودها", "downloads"),
        Triple(Icons.Outlined.Search, "جستجو", "search")
    )

    // اندازه‌ی پنجره با باز بودن منو یا بالن هماهنگ می‌شه
    LaunchedEffect(menuVisible, bubble != null) { onExpand(menuVisible || bubble != null) }

    // خوابیدن/بیدار شدن: پنجره تا نصفه پشت لبه می‌ره و برمی‌گرده
    LaunchedEffect(asleep) { onSleep(asleep) }

    // کلیک بیرون یا شروع کشیدن: همه‌چیز بسته می‌شه
    LaunchedEffect(ui.collapseSignal) {
        if (ui.collapseSignal > 0) {
            menuOpen = false
            bubble = null
        }
    }

    // بی‌کاری ۱۵ ثانیه‌ای: می‌خوابه و همه‌ی حلقه‌های انیمیشن متوقف می‌شن
    LaunchedEffect(activity, menuOpen, bubble, ui.dragging) {
        asleep = false
        if (!menuOpen && bubble == null && !ui.dragging) {
            delay(15_000)
            asleep = true
        }
    }

    // تاب خوردن بدن و دم (فقط وقتی بیداره)
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        while (true) {
            bob.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
            bob.animateTo(0f, tween(1600, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        while (true) {
            tailA.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
            tailA.animateTo(0f, tween(1500, easing = FastOutSlowInEasing))
        }
    }
    // حرف زدن
    LaunchedEffect(bubble != null, animate) {
        if (bubble == null || !animate) {
            talk.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            talk.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
            talk.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
        }
    }
    // پلک و گوش
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        scope.launch {
            while (true) {
                delay((2_200L..4_800L).random())
                motion.blinkOnce()
            }
        }
        while (true) {
            delay((3_500L..8_000L).random())
            motion.twitchEar()
        }
    }

    // کاموا‌های منو یکی‌یکی بیرون میان
    LaunchedEffect(menuOpen) {
        coroutineScope {
            motion.menuProg.forEachIndexed { i, a ->
                launch {
                    delay(if (menuOpen) 140L + i * 200L else (2 - i) * 90L)
                    a.animateTo(if (menuOpen) 1f else 0f, spring(0.62f, Spring.StiffnessLow))
                }
            }
        }
    }

    // فرود نرم بعد از چسبیدن به لبه
    LaunchedEffect(ui.landTick) {
        if (ui.landTick > 0) motion.landSquash()
    }

    // بیرون از برنامه حرف نمی‌زنه؛ جمله‌های تصادفی و خبرها فقط داخل برنامه‌ان.
    // اینجا فقط خودش، چت و دکمه‌هاست.

    val bodyAlpha by animateFloatAsState(if (asleep) 0.38f else 1f, tween(500), label = "alpha")
    val speakAmt by animateFloatAsState(if (bubble != null) 1f else 0f, tween(300), label = "speak")
    val scare by animateFloatAsState(
        if (ui.dragging) 1f else 0f, spring(0.6f, Spring.StiffnessLow), label = "scare"
    )
    val happy by animateFloatAsState(if (menuVisible) 1f else 0f, tween(250), label = "happy")
    val face by animateFloatAsState(
        if (ui.onRight) -1f else 1f, spring(0.7f, Spring.StiffnessLow), label = "face"
    )
    val bobPx = with(density) { 3.dp.toPx() }
    val hopPx = with(density) { 12.dp.toPx() }

    val corner = when {
        ui.lowerHalf && ui.onRight -> Alignment.BottomEnd
        ui.lowerHalf -> Alignment.BottomStart
        ui.onRight -> Alignment.TopEnd
        else -> Alignment.TopStart
    }

    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {

        // بالن پیام (بیرون از برنامه پیامی نمی‌آد، ولی ساختارش مونده)
        Column(
            modifier = Modifier
                .align(corner)
                .padding(
                    bottom = if (ui.lowerHalf) AvatarSize + 10.dp else 0.dp,
                    top = if (ui.lowerHalf) 0.dp else AvatarSize + 10.dp
                ),
            horizontalAlignment = if (ui.onRight) Alignment.End else Alignment.Start
        ) {
            AnimatedVisibility(
                visible = bubble != null && !menuVisible,
                enter = fadeIn(tween(200)) + scaleIn(spring(0.6f, Spring.StiffnessMediumLow), initialScale = 0.7f),
                exit = fadeOut(tween(150))
            ) {
                Box(modifier = Modifier.widthIn(max = 270.dp)) {
                    SpeechBubble(
                        say = lastSay,
                        onRight = ui.onRight,
                        lowerHalf = ui.lowerHalf,
                        onClick = {
                            bubble = null
                            if (lastSay.kind == SayKind.DONE) onOpen("downloads")
                        }
                    )
                }
            }
        }

        // دکمه‌های منو: کنار گربه، به سمت وسط صفحه (نه زیرش)
        if (menuVisible) {
            Row(
                modifier = Modifier
                    .align(corner)
                    .padding(
                        start = if (ui.onRight) 0.dp else AvatarSize + 8.dp,
                        end = if (ui.onRight) AvatarSize + 8.dp else 0.dp
                    ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Top
            ) {
                items.forEachIndexed { i, (icon, label, route) ->
                    YarnButton(
                        icon = icon,
                        label = label,
                        tint = yarn[i],
                        progress = { motion.menuProg[i].value },
                        spin = { motion.menuProg[i].value * 600f },
                        bob = { bob.value },
                        flip = if (i % 2 == 0) 1f else -1f,
                        onClick = {
                            menuOpen = false
                            // «چت با من» همین‌جا توی پنجره‌ی کوچیک باز می‌شه، نه توی برنامه
                            if (route == "ai_chat") onOpenChat() else onOpen(route)
                        }
                    )
                }
            }
        }

        // خود گربه
        Box(
            modifier = Modifier
                .align(corner)
                .size(AvatarSize)
                .graphicsLayer {
                    alpha = bodyAlpha
                    transformOrigin = TransformOrigin(0.5f, 1f)
                    rotationZ = motion.tilt.value
                    translationY = bob.value * bobPx - motion.pounce.value * hopPx
                    scaleY = 1f + 0.10f * scare - 0.14f * motion.land.value
                    scaleX = 1f - 0.05f * scare + 0.10f * motion.land.value
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        activity++
                        scope.launch { motion.hop(0.6f) }
                        bubble = null
                        menuOpen = !menuOpen
                    })
                }
        ) {
            Surface(
                modifier = Modifier.matchParentSize(),
                shape = AvatarShape,
                color = surface,
                border = BorderStroke(1.5.dp, primary),
                shadowElevation = 6.dp,
                content = {}
            )
            Canvas(modifier = Modifier.matchParentSize()) {
                var reach = 0f
                motion.menuProg.forEach { m ->
                    val v = m.value
                    if (v > 0f && v < 0.45f) {
                        val r = kotlin.math.sin(Math.PI * v / 0.45).toFloat()
                        if (r > reach) reach = r
                    }
                }
                drawCat(
                    CatPose(
                        face = face,
                        blink = motion.blink.value,
                        ear = motion.ear.value,
                        tail = (tailA.value - 0.5f) * 2f,
                        mouth = maxOf(talk.value * speakAmt, reach * 0.7f, scare * 0.45f),
                        paw = reach,
                        lookX = (bob.value - 0.5f) * 0.8f,
                        lookY = scare * 0.4f,
                        scare = scare,
                        happy = happy,
                        wiggle = (bob.value - 0.5f) * 2f,
                        yarn = primary
                    )
                )
            }
        }
    }
}
