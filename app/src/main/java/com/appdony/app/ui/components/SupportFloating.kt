package com.appdony.app.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ───────────────────────── حضور پشتیبان جای گربه ─────────────────────────

/** OFF: گربه سر جاشه · ENTER: عصا گربه رو از قاب می‌کشه بیرون · ON: پشتیبان ۲ دقیقه جای گربه‌ست · EXIT: برگشت گربه */
internal enum class SupportPhase { OFF, ENTER, ON, EXIT }

internal object SupportPresence {
    const val HOLD_MS = 120_000L

    var phase by mutableStateOf(SupportPhase.OFF)
    var endAt by mutableLongStateOf(0L)

    /** باز کردن چت پشتیبانی؛ SupportFloating ثبتش می‌کنه تا «چت با من» روی پشتیبان از اون استفاده کنه */
    var openSupport: (() -> Unit)? = null

    /** وقتی کاربر از چت پشتیبانی میاد بیرون صدا زده می‌شه */
    fun activate() {
        endAt = SystemClock.elapsedRealtime() + HOLD_MS
        if (phase != SupportPhase.ON) phase = SupportPhase.ENTER
    }

    fun reset() {
        phase = SupportPhase.OFF
    }
}

/** جای فعلی گربه‌ی داخل اپ؛ پشتیبان دقیقاً همون‌جا می‌شینه */
internal object AssistantSlot {
    @Volatile
    var motion: AvatarMotion? = null
}

// ───────────────────────── کامپوننت اصلی ─────────────────────────

/**
 * فقط انیمیشن عوض شدن گربه و پشتیبان رو نشون می‌ده (همه‌چیز توی خود قابِ گربه).
 * خود پشتیبان وقتی ON شد، توی [FloatingAssistant] کار می‌کنه.
 */
@Composable
internal fun SupportFloating(
    visible: Boolean,
    onOpenSupport: () -> Unit
) {
    val phase = SupportPresence.phase
    val visibleNow by rememberUpdatedState(visible)
    val openNow by rememberUpdatedState(onOpenSupport)
    val density = LocalDensity.current

    DisposableEffect(Unit) {
        SupportPresence.openSupport = { openNow() }
        onDispose { SupportPresence.openSupport = null }
    }

    val caneIn = remember { Animatable(0f) }   // ۰ بیرون قاب ← ۱ دور گربه قفل شده
    val yank = remember { Animatable(0f) }     // ۰ ← ۱ گربه با عصا از قاب کشیده می‌شه بیرون
    val supIn = remember { Animatable(0f) }    // ورود پشتیبان به قاب (فنر؛ کمی رد می‌کنه و برمی‌گرده)
    val catBack = remember { Animatable(0f) }  // برگشت گربه موقع تعویض

    LaunchedEffect(phase) {
        when (phase) {
            SupportPhase.OFF -> {
                caneIn.snapTo(0f); yank.snapTo(0f); supIn.snapTo(0f); catBack.snapTo(0f)
            }

            SupportPhase.ENTER -> {
                caneIn.snapTo(0f); yank.snapTo(0f); supIn.snapTo(0f); catBack.snapTo(0f)
                // تا صفحه‌ی اصلی جلوی کاربر نیومده شروع نمی‌کنیم
                snapshotFlow { visibleNow }.first { it }
                delay(350)
                caneIn.animateTo(1f, tween(420, easing = FastOutSlowInEasing))   // عصا میاد توی قاب و دور گربه قفل می‌شه
                delay(260)                                                        // گربه می‌ترسه
                yank.animateTo(1f, tween(450, easing = FastOutLinearInEasing))   // تَق! از قاب کشیده می‌شه بیرون
                delay(150)
                supIn.animateTo(1f, spring(0.45f, Spring.StiffnessLow))          // پشتیبان میاد توی قاب
                SupportPresence.endAt = SystemClock.elapsedRealtime() + SupportPresence.HOLD_MS
                SupportPresence.phase = SupportPhase.ON
            }

            SupportPhase.ON -> {
                supIn.snapTo(1f)
                while (true) {
                    if (SystemClock.elapsedRealtime() >= SupportPresence.endAt) {
                        SupportPresence.phase = if (visibleNow) SupportPhase.EXIT else SupportPhase.OFF
                        break
                    }
                    delay(250)
                }
            }

            SupportPhase.EXIT -> {
                catBack.snapTo(0f)
                coroutineScope {
                    launch { supIn.animateTo(0f, tween(260, easing = FastOutLinearInEasing)) }
                    launch {
                        delay(120)
                        catBack.animateTo(1f, spring(0.4f, Spring.StiffnessMediumLow))
                    }
                }
                delay(120)
                SupportPresence.phase = SupportPhase.OFF
            }
        }
    }

    if (!visible || (phase != SupportPhase.ENTER && phase != SupportPhase.EXIT)) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val sizePx = with(density) { AvatarSize.toPx() }

            // جای گربه (اگه هنوز ثبت نشده، پایین-چپ)
            val m = AssistantSlot.motion
            val slotX = m?.offX?.value ?: with(density) { EdgeMargin.toPx() }
            val slotY = m?.offY?.value ?: with(density) { (maxHeight - 170.dp).toPx() }
            val onRight = slotX + sizePx / 2f > wPx / 2f

            val entering = phase == SupportPhase.ENTER
            SwapFrame(
                modifier = Modifier.absoluteOffset { IntOffset(slotX.roundToInt(), slotY.roundToInt()) },
                outDir = if (onRight) 1f else -1f,      // گربه از سمت لبه‌ی صفحه از قاب بیرون می‌ره
                face = if (onRight) -1f else 1f,
                catOut = if (entering) yank.value else 1f - catBack.value,
                scare = if (entering) caneIn.value else 0f,
                showCane = entering,
                cane = caneIn.value,
                supIn = supIn.value
            )
        }
    }
}

/**
 * قابِ گربه: گربه، عصا و پشتیبان همه داخل همین قاب جا به جا می‌شن و بیرونش دیده نمی‌شن.
 * [outDir] ۱ = گربه به راست کشیده می‌شه، ‎-۱ = به چپ.
 */
@Composable
private fun SwapFrame(
    modifier: Modifier,
    outDir: Float,
    face: Float,
    catOut: Float,
    scare: Float,
    showCane: Boolean,
    cane: Float,
    supIn: Float
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.size(AvatarSize)) {
        // خود قاب؛ وقتی پشتیبان کامل اومد کم‌رنگ می‌شه (پشتیبان دایره‌ایه)
        Surface(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = (1f - supIn).coerceIn(0f, 1f) },
            shape = AvatarShape,
            color = surfaceColor,
            border = BorderStroke(1.5.dp, primary),
            shadowElevation = 10.dp,
            content = {}
        )

        // همه‌چیز داخل قاب بریده می‌شه
        Box(modifier = Modifier.matchParentSize().clip(AvatarShape)) {
            // ── گربه‌ی ترسیده؛ وقتی کشیده می‌شه کمی کشیده و باریک می‌شه
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val s = (catOut * (1f - catOut) * 4f).coerceAtLeast(0f)
                        translationX = outDir * catOut * size.width * 1.2f
                        scaleX = 1f + 0.25f * s
                        scaleY = 1f - 0.12f * s
                    }
            ) {
                drawCat(
                    CatPose(
                        face = face,
                        blink = 0f,
                        ear = 0f,
                        tail = 0f,
                        mouth = 0.6f * scare,
                        paw = 0f,
                        lookX = face,
                        lookY = 0f,
                        scare = scare,
                        happy = 0f,
                        wiggle = 0f,
                        yarn = primary
                    )
                )
            }

            // ── عصا: از لبه‌ی خود قاب میاد، دور گربه قلاب می‌شه و باهاش بیرون می‌ره
            if (showCane) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val u = 1.dp.toPx()
                    val w = size.width
                    val h = size.height
                    val hookR = 16f * u
                    val cy = h * 0.55f
                    val hx = w / 2f + (1f - cane) * w * 0.95f + catOut * w * 1.2f
                    withTransform({ if (outDir < 0f) scale(-1f, 1f, Offset(w / 2f, 0f)) }) {
                        val path = Path().apply {
                            moveTo(hx + w, cy + hookR)
                            lineTo(hx, cy + hookR)
                            arcTo(Rect(Offset(hx - hookR, cy - hookR), Size(hookR * 2f, hookR * 2f)), 90f, 180f, false)
                            lineTo(hx + hookR * 0.55f, cy - hookR)
                        }
                        drawPath(
                            path, Color(0xFF6B4423),
                            style = Stroke(width = 5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path, Color(0xFFB07A45),
                            style = Stroke(width = 2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }

            // ── پشتیبان: از سمت مخالف میاد توی قاب
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationX = -outDir * size.width * (1f - supIn)
                        alpha = (supIn * 5f).coerceIn(0f, 1f)
                    }
            ) {
                SupportAvatar(sizeDp = 60)
            }
        }
    }
}
