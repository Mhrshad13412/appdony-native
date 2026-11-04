package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility as BannerAnimatedVisibility
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically as bannerSlideIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp as bannerDp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * آیا بلور پشت پنجره واقعاً روشنه؟
 * فقط چک نسخه‌ی اندروید کافی نیست: روی بعضی گوشی‌ها (شیائومی، حالت باتری، کاهش انیمیشن) بلور خاموشه.
 */
@Composable
private fun rememberBlurBehindSupported(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) rememberCrossWindowBlurEnabled() else false

@SuppressLint("NewApi")
@Composable
private fun rememberCrossWindowBlurEnabled(): Boolean {
    val context = LocalContext.current
    val windowManager = remember { context.getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    var enabled by remember { mutableStateOf(windowManager.isCrossWindowBlurEnabled) }
    DisposableEffect(windowManager) {
        val listener = java.util.function.Consumer<Boolean> { enabled = it }
        windowManager.addCrossWindowBlurEnabledListener(listener)
        onDispose { windowManager.removeCrossWindowBlurEnabledListener(listener) }
    }
    return enabled
}

/**
 * کارت شناور مشترک (لاگین / منوی اکانت / درباره):
 * از هر چهار طرف فاصله دارد، پس‌زمینه تیره + بلور، و با انیمیشن فنری وارد می‌شود.
 * اگه بلور پنجره روی گوشی خاموش باشه، کارت تقریباً مات میاد تا محتوای پشتش دیده نشه.
 */
@Composable
fun FloatingDialog(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurSupported = rememberBlurBehindSupported()

    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                // بدون بلور، دیم قوی‌تر تا پشت کارت کمتر دیده بشه
                window.setDimAmount(if (blurSupported) 0.45f else 0.6f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (blurSupported) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                        val params = window.attributes
                        params.blurBehindRadius = 40
                        window.attributes = params
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    }
                }
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)) +
                    scaleIn(initialScale = 0.88f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 8 }
            ) {
                // شیشه‌ای: همون ته‌رنگ و لبه‌ی منوی پایین روی بلورِ پشت پنجره (کمی غلیظ‌تر چون متن داره)؛
                // اگه بلور واقعی در دسترس نباشه (اندروید ۱۱ و پایین‌تر یا بلور خاموش)، ته‌رنگ تقریباً مات میاد
                val glass = LocalGlassMode.current
                val dialogShape = RoundedCornerShape(28.dp)
                val dialogColor = if (glass) {
                    glassTintOf(isGlassDark(), alpha = if (blurSupported) 0.74f else 0.97f)
                } else MaterialTheme.colorScheme.surface
                Surface(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    shape = dialogShape,
                    color = dialogColor,
                    // سایه‌ی المنت از زیر شیشه‌ی شفاف دیده می‌شه؛ تو شیشه‌ای فقط دیم و بلور پشت پنجره جداش می‌کنه
                    shadowElevation = if (glass) 0.dp else 24.dp,
                    border = if (glass) BorderStroke(1.dp, glassRimBrush()) else null
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = content
                    )
                }
            }
        }
    }
}


// ───────────── اعلان داخل اپ (جای Toast) ─────────────
// زرد = در حال بررسی / هشدار ، سبز = موفق (فایل پیدا شد) ، قرمز = خطا

enum class BannerKind { CHECKING, WARNING, SUCCESS, ERROR }

data class BannerMsg(val kind: BannerKind, val text: String, val seq: Long)

object AppBanner {
    private val _msg = MutableStateFlow<BannerMsg?>(null)
    val msg: StateFlow<BannerMsg?> = _msg.asStateFlow()
    private var seq = 0L
    private var hideAfter = 3200L

    val duration: Long get() = hideAfter

    fun show(kind: BannerKind, text: String, durationMs: Long = 3200L) {
        hideAfter = durationMs
        _msg.value = BannerMsg(kind, text, ++seq)
    }

    fun dismiss() {
        _msg.value = null
    }
}

private val BannerYellow = Color(0xFFFFC53D)
private val BannerGreen = Color(0xFF34C77B)
private val BannerRed = Color(0xFFFF5C5C)

@Composable
fun AppBannerHost(modifier: Modifier = Modifier) {
    UpdateHost() // چک آپدیت خودکار از گیت‌هاب

    val current by AppBanner.msg.collectAsState()
    var last by remember { mutableStateOf<BannerMsg?>(null) }
    SideEffect { if (current != null) last = current }
    val shown = current ?: last

    LaunchedEffect(current?.seq) {
        val m = current ?: return@LaunchedEffect
        delay(AppBanner.duration)
        if (AppBanner.msg.value?.seq == m.seq) AppBanner.dismiss()
    }

    BannerAnimatedVisibility(
        visible = current != null,
        enter = bannerSlideIn(tween(260)) { -it } + fadeIn(tween(200)),
        exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(160)),
        modifier = modifier
    ) {
        val m = shown ?: return@BannerAnimatedVisibility
        val accent = when (m.kind) {
            BannerKind.CHECKING, BannerKind.WARNING -> BannerYellow
            BannerKind.SUCCESS -> BannerGreen
            BannerKind.ERROR -> BannerRed
        }
        val icon = when (m.kind) {
            BannerKind.CHECKING -> Icons.Default.Info
            BannerKind.WARNING -> Icons.Default.Warning
            BannerKind.SUCCESS -> Icons.Default.CheckCircle
            BannerKind.ERROR -> Icons.Default.Error
        }
        Box(modifier = Modifier.statusBarsPadding().padding(horizontal = 16.bannerDp, vertical = 8.bannerDp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { AppBanner.dismiss() },
                shape = RoundedCornerShape(14.bannerDp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.bannerDp, accent.copy(alpha = 0.55f)),
                shadowElevation = 10.bannerDp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.bannerDp, vertical = 10.bannerDp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.bannerDp)
                            .clip(RoundedCornerShape(10.bannerDp))
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.bannerDp))
                    }
                    Spacer(Modifier.width(10.bannerDp))
                    Text(
                        text = m.text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
