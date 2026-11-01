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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ───────────────────────── حضور پشتیبان جای گربه ─────────────────────────

/** OFF: گربه سر جاشه · ENTER: انیمیشن عصا · ON: پشتیبان ۲ دقیقه جای گربه‌ست · EXIT: برگشت گربه */
internal enum class SupportPhase { OFF, ENTER, ON, EXIT }

internal object SupportPresence {
    const val HOLD_MS = 120_000L

    var phase by mutableStateOf(SupportPhase.OFF)
    var endAt by mutableLongStateOf(0L)

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

@Composable
internal fun SupportFloating(
    visible: Boolean,
    onOpenSupport: () -> Unit
) {
    val phase = SupportPresence.phase
    val visibleNow by rememberUpdatedState(visible)
    val openNow by rememberUpdatedState(onOpenSupport)
    val density = LocalDensity.current
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val caneIn = remember { Animatable(0f) }   // ۰ بیرون صفحه ← ۱ دور گربه قفل شده
    val yank = remember { Animatable(0f) }     // ۰ ← ۱ گربه با عصا از سمت راست کشیده می‌شه بیرون
    val supIn = remember { Animatable(0f) }    // ورود پشتیبان (فنر؛ کمی رد می‌کنه و برمی‌گرده)
    val catBack = remember { Animatable(0f) }  // برگشت گربه موقع تعویض
    var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }

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
                caneIn.animateTo(1f, tween(420, easing = FastOutSlowInEasing))   // عصا میاد دور گربه
                delay(260)                                                        // گربه جا می‌خوره
                yank.animateTo(1f, tween(450, easing = FastOutLinearInEasing))   // تَق! کشیده می‌شه بیرون
                delay(150)
                supIn.animateTo(1f, spring(0.45f, Spring.StiffnessLow))          // پشتیبان میاد جاش
                SupportPresence.endAt = SystemClock.elapsedRealtime() + SupportPresence.HOLD_MS
                SupportPresence.phase = SupportPhase.ON
            }

            SupportPhase.ON -> {
                supIn.snapTo(1f)
                while (true) {
                    nowMs = SystemClock.elapsedRealtime()
                    if (nowMs >= SupportPresence.endAt) {
                        SupportPresence.phase = if (visibleNow) SupportPhase.EXIT else SupportPhase.OFF
                        break
                    }
                    delay(100)
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

    if (!visible || phase == SupportPhase.OFF) return

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val sizePx = with(density) { AvatarSize.toPx() }
            val gapPx = with(density) { 20.dp.toPx() }
            val btnPx = with(density) { 30.dp.toPx() }

            // جای گربه (اگه هنوز ثبت نشده، پایین-چپ)
            val m = AssistantSlot.motion
            val slotX = m?.offX?.value ?: with(density) { EdgeMargin.toPx() }
            val slotY = m?.offY?.value ?: with(density) { (maxHeight - 170.dp).toPx() }
            val onRight = slotX + sizePx / 2f > wPx / 2f
            val face = if (onRight) -1f else 1f

            val cx = slotX + sizePx / 2f
            val cy = slotY + sizePx / 2f
            val farDx = wPx - cx + with(density) { 80.dp.toPx() }   // تا بیرون لبه‌ی راست
            val hookR = with(density) { 36.dp.toPx() }

            // ── ۱) گربه‌ی اشباحی که با عصا کشیده می‌شه
            if (phase == SupportPhase.ENTER) {
                GhostCat(
                    face = face,
                    scare = caneIn.value,
                    modifier = Modifier
                        .absoluteOffset { IntOffset((slotX + yank.value * farDx).roundToInt(), slotY.roundToInt()) }
                        .graphicsLayer {
                            val s = yank.value * (1f - yank.value) * 4f   // وسط کشیده شدن بیشترین کشیدگی
                            transformOrigin = TransformOrigin(0f, 0.5f)
                            scaleX = 1f + 0.35f * s
                            scaleY = 1f - 0.18f * s
                        }
                )

                // ── ۲) عصا: دسته‌ی بلند از راست، خمیدگی دور گربه
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val u = 1.dp.toPx()
                    val hx = cx + (1f - caneIn.value) * farDx + yank.value * farDx
                    val path = Path().apply {
                        moveTo(hx + wPx, cy + hookR)
                        lineTo(hx, cy + hookR)
                        arcTo(Rect(Offset(hx - hookR, cy - hookR), Size(hookR * 2f, hookR * 2f)), 90f, 180f, false)
                        lineTo(hx + hookR * 0.55f, cy - hookR)
                    }
                    drawPath(
                        path, Color(0xFF6B4423),
                        style = Stroke(width = 7f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path, Color(0xFFB07A45),
                        style = Stroke(width = 3f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // ── ۳) موقع برگشت: گربه با فنر می‌پره سر جاش
            if (phase == SupportPhase.EXIT) {
                GhostCat(
                    face = face,
                    scare = 0f,
                    modifier = Modifier
                        .absoluteOffset { IntOffset(slotX.roundToInt(), slotY.roundToInt()) }
                        .graphicsLayer {
                            val v = catBack.value
                            alpha = (v * 4f).coerceIn(0f, 1f)
                            scaleX = v
                            scaleY = v
                        }
                )
            }

            // ── ۴) پشتیبان: بوردرش همون تایمره (۲ دقیقه، پاد‌ساعت‌گرد کم می‌شه)
            val ring = if (phase == SupportPhase.ENTER) {
                1f
            } else {
                ((SupportPresence.endAt - nowMs).toFloat() / SupportPresence.HOLD_MS).coerceIn(0f, 1f)
            }
            fun supX(): Float = slotX - (slotX + sizePx + gapPx) * (1f - supIn.value)

            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(supX().roundToInt(), slotY.roundToInt()) }
                    .size(AvatarSize)
                    .graphicsLayer { alpha = (supIn.value * 5f).coerceIn(0f, 1f) }
                    .shadow(10.dp, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (SupportPresence.phase == SupportPhase.ON) openNow()
                        })
                    }
            ) {
                SupportAvatar(sizeDp = 60, ring = ring)
            }

            // ── ۵) دکمه‌ی «AI» بالای پشتیبان؛ با زدنش دوباره گربه (هوش مصنوعی) برمی‌گرده
            val above = slotY > with(density) { 90.dp.toPx() }
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        val bx = supX() + (sizePx - btnPx) / 2f
                        val by = if (above) slotY - btnPx - with(density) { 6.dp.toPx() }
                        else slotY + sizePx + with(density) { 6.dp.toPx() }
                        IntOffset(bx.roundToInt(), by.roundToInt())
                    }
                    .size(30.dp)
                    .graphicsLayer { alpha = ((supIn.value - 0.7f) / 0.3f).coerceIn(0f, 1f) }
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(surfaceColor)
                    .border(1.5.dp, primary, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (SupportPresence.phase == SupportPhase.ON) {
                                SupportPresence.phase = SupportPhase.EXIT
                            }
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("AI", color = primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** کپی ثابت گربه (همون ظاهر گربه‌ی شناور) برای انیمیشن تعویض */
@Composable
private fun GhostCat(face: Float, scare: Float, modifier: Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.size(AvatarSize)) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = AvatarShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, primary),
            shadowElevation = 10.dp,
            content = {}
        )
        Canvas(modifier = Modifier.matchParentSize()) {
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
    }
}
