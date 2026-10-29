package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.util.AssistantBus
import com.appdony.app.util.AssistantSay
import com.appdony.app.util.DownloadPhase
import com.appdony.app.util.SayKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random


// ───────────────────────── کامپوننت اصلی ─────────────────────────

@Composable
fun FloatingAssistant(
    currentRoute: String?,
    visible: Boolean,
    bottomBarHeight: Dp,
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val motion = remember { AvatarMotion() }
    val routeNow by rememberUpdatedState(currentRoute)
    val visibleNow by rememberUpdatedState(visible)

    var ready by remember { mutableStateOf(false) }
    var onRight by remember { mutableStateOf(false) }
    var lowerHalf by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var hidden by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    var bubbleSay by remember { mutableStateOf<AssistantSay?>(null) }
    var lastSay by remember { mutableStateOf(AssistantSay("")) }
    var activity by remember { mutableIntStateOf(0) }
    var dlActive by remember { mutableStateOf(false) }
    var offerOnly by remember { mutableStateOf(false) }

    // ترتیب: چت، دانلودها، جستجو
    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با من", onChatClick),
        Triple(Icons.Outlined.Download, "دانلودها", onDownloadsClick),
        Triple(Icons.Outlined.Search, "جستجو", onSearchClick)
    )
    val angles = listOf(0f, 62f, -62f)

    // نقطه‌ی شروع و پایان نخ هر گلوله؛ توی Layout پر می‌شه و Canvas نخ‌ها می‌خونه
    // هر گلوله ۶ عدد: ballX, ballY, anchorX, anchorY, alpha, sag
    val threadData = remember { FloatArray(18) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            val sizePx = with(density) { AvatarSize.toPx() }
            val marginPx = with(density) { EdgeMargin.toPx() }
            val barPx = with(density) { bottomBarHeight.toPx() }
            val topInset = WindowInsets.statusBars.getTop(density).toFloat()
            val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()

            val topLimit = topInset + marginPx / 2f
            val bottomLimit = (hPx - bottomInset - barPx - sizePx - with(density) { 6.dp.toPx() })
                .coerceAtLeast(topLimit)

            // جای اولیه: پایین-چپ، بالای نوار پایینی
            LaunchedEffect(wPx, hPx) {
                if (!ready && wPx > 0f && hPx > 0f) {
                    motion.offX.snapTo(marginPx)
                    motion.offY.snapTo(
                        (bottomLimit - with(density) { 24.dp.toPx() }).coerceIn(topLimit, bottomLimit)
                    )
                    onRight = false
                    lowerHalf = true
                    ready = true
                }
            }

            LaunchedEffect(ready, topLimit, bottomLimit) {
                if (ready) {
                    val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                    if (ty != motion.offY.value) {
                        motion.offY.animateTo(ty, spring(0.8f, Spring.StiffnessLow))
                    }
                    lowerHalf = ty + sizePx / 2f > hPx / 2f
                }
            }
            LaunchedEffect(ready, wPx) {
                if (ready) {
                    motion.offX.snapTo(if (onRight) wPx - sizePx - marginPx else marginPx)
                }
            }

            // قایم شدن کنار صفحه و کم‌رنگ شدن
            LaunchedEffect(hidden, onRight, ready) {
                val target = if (!hidden) 0f else (if (onRight) 1f else -1f) * (sizePx * 0.52f + marginPx)
                motion.hideShift.animateTo(target, spring(0.8f, Spring.StiffnessLow))
            }
            val bodyAlpha by animateFloatAsState(if (hidden) 0.38f else 1f, tween(500), label = "assistant_alpha")

            LaunchedEffect(activity, expanded, bubbleSay, dragging, hidden, ready, visible) {
                if (ready && visible && !expanded && bubbleSay == null && !dragging && !hidden) {
                    delay(12_000)
                    hidden = true
                }
            }

            // پلک زدن (گاهی دوبار پشت هم)
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                while (true) {
                    delay((2_200L..4_800L).random())
                    motion.blinkOnce()
                    if (Random.nextFloat() < 0.25f) {
                        delay(160)
                        motion.blinkOnce()
                    }
                }
            }

            // تکون خوردن گوش
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                while (true) {
                    delay((3_500L..8_000L).random())
                    motion.twitchEar()
                }
            }

            // کاموا‌های منو: یکی‌یکی از کنار گربه قل می‌خورن بیرون
            LaunchedEffect(expanded) {
                if (expanded) { offerOnly = false; showMenu = true }
                coroutineScope {
                    motion.menuProg.forEachIndexed { i, a ->
                        launch {
                            delay(if (expanded) 120L + i * 230L else (2 - i) * 120L)
                            a.animateTo(if (expanded) 1f else 0f, spring(0.62f, Spring.StiffnessLow))
                        }
                    }
                }
                if (!expanded) showMenu = false
            }

            suspend fun speak(say: AssistantSay, holdMs: Long) {
                hidden = false
                lastSay = say
                bubbleSay = say
                delay(holdMs)
                if (bubbleSay === say) bubbleSay = null
            }

            // پرش‌های بازیگوشانه‌ی گربه تا وقتی لینک بررسی می‌شه
            LaunchedEffect(dlActive) {
                if (dlActive) {
                    delay(400)
                    while (true) {
                        motion.hop(1f)
                        delay((250L..600L).random())
                    }
                }
            }

            // خبرهای فوری (مثلاً تموم شدن دانلود)
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                AssistantBus.events.collect { say ->
                    if (visibleNow && !dragging) {
                        expanded = false
                        speak(say, 9_000)
                    }
                }
            }

            // دکمه‌ی دانلود: کاموا‌ها از بالا آویزون می‌شن و گربه باهاشون بازی می‌کنه،
            // بعد قل می‌خورن سمت راست و آخرش فقط کاموای «دانلودها» تعارف می‌شه
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                var startMs = 0L
                AssistantBus.download.collect { phase ->
                    when (phase) {
                        DownloadPhase.CHECKING -> {
                            if (!visibleNow || dragging) return@collect
                            motion.dlJob?.cancel()
                            hidden = false
                            expanded = false
                            showMenu = false
                            offerOnly = false
                            bubbleSay = null
                            motion.menuProg.forEach { it.snapTo(0f) }
                            motion.dlPos.forEach { it.snapTo(0f) }
                            dlActive = true
                            startMs = System.currentTimeMillis()
                            scope.launch {
                                val say = AssistantSay("تا لینک رو چک می‌کنم، بیا با کاموا بازی کنیم 🧶", SayKind.DOWNLOAD)
                                AssistantBus.noteShown(say.text)
                                speak(say, 6_000)
                            }
                            coroutineScope {
                                motion.dlPos.forEachIndexed { i, a ->
                                    launch {
                                        delay(i * 260L)
                                        a.animateTo(1f, tween(1300, easing = FastOutSlowInEasing))
                                        delay(350)
                                        a.animateTo(2f, tween(1100, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        }

                        DownloadPhase.READY, DownloadPhase.FAILED -> {
                            if (dlActive) {
                                val wait = 3_500L - (System.currentTimeMillis() - startMs)
                                if (wait > 0) delay(wait)
                                // یکی‌یکی قل می‌خورن پیش گربه
                                coroutineScope {
                                    motion.dlPos.forEachIndexed { i, a ->
                                        launch {
                                            delay(i * 380L)
                                            a.animateTo(3f, tween(900, easing = FastOutSlowInEasing))
                                        }
                                    }
                                }
                                dlActive = false
                                motion.dlPos.forEach { it.snapTo(0f) }
                            }
                            if (!visibleNow) return@collect
                            if (phase == DownloadPhase.READY) {
                                // فقط کاموای «دانلودها» تعارف می‌شه
                                offerOnly = true
                                showMenu = true
                                motion.menuProg.forEach { it.snapTo(0f) }
                                scope.launch {
                                    val say = AssistantSay("بیا، این کاموا تو رو می‌بره بخش دانلودها 🧶", SayKind.DOWNLOAD)
                                    AssistantBus.noteShown(say.text)
                                    speak(say, 9_000)
                                }
                                motion.dlJob = scope.launch {
                                    motion.menuProg[1].animateTo(1f, spring(0.55f, Spring.StiffnessLow))
                                    delay(8_500)
                                    if (offerOnly) {
                                        motion.menuProg[1].animateTo(0f, spring(0.6f, Spring.StiffnessMediumLow))
                                        offerOnly = false
                                        showMenu = false
                                    }
                                }
                            } else {
                                scope.launch {
                                    val say = AssistantSay("این لینک جواب نداد. یه لینک دیگه امتحان کن.", SayKind.FAIL)
                                    AssistantBus.noteShown(say.text)
                                    speak(say, 6_000)
                                }
                            }
                        }
                    }
                }
            }

            // هر ~۳۰ ثانیه یه جمله
            LaunchedEffect(ready) {
                if (!ready) return@LaunchedEffect
                delay(7_000)
                while (true) {
                    if (visibleNow && !expanded && !dragging && bubbleSay == null) {
                        val say = if (Random.nextFloat() < 0.15f) {
                            fetchSuggestion() ?: AssistantPhrases.next(routeNow)
                        } else {
                            AssistantPhrases.next(routeNow)
                        }
                        AssistantBus.noteShown(say.text)
                        speak(say, 7_500)
                    }
                    delay((28_000L..38_000L).random())
                }
            }

            val infinite = rememberInfiniteTransition(label = "assistant_infinite")
            val bob by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "bob"
            )
            val talk by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "talk"
            )
            val tailSlow by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "tail_slow"
            )
            val tailFast by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(280, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "tail_fast"
            )
            val swat by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(330, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "swat"
            )
            val speakAmt by animateFloatAsState(
                targetValue = if (bubbleSay != null) 1f else 0f,
                animationSpec = tween(300),
                label = "speak"
            )
            // بغل گرفته شدن (موقع کشیدن)
            val scare by animateFloatAsState(
                targetValue = if (dragging) 1f else 0f,
                animationSpec = spring(0.6f, Spring.StiffnessLow),
                label = "scare"
            )
            val playAmt by animateFloatAsState(
                targetValue = if (dlActive) 1f else 0f,
                animationSpec = tween(350),
                label = "play"
            )
            val offerAmt by animateFloatAsState(
                targetValue = if (offerOnly) 1f else 0f,
                animationSpec = tween(300),
                label = "offer"
            )
            val happyAmt by animateFloatAsState(
                targetValue = if (offerOnly || expanded) 1f else 0f,
                animationSpec = tween(250),
                label = "happy"
            )
            // جهت نگاه: همیشه به داخل صفحه؛ موقع عوض شدن طرف، نرم برمی‌گرده
            val face by animateFloatAsState(
                targetValue = if (onRight) -1f else 1f,
                animationSpec = spring(0.7f, Spring.StiffnessLow),
                label = "face"
            )
            val bobPx = with(density) { 3.dp.toPx() }
            val jellyPx = with(density) { 6.dp.toPx() }
            val hopPx = with(density) { 12.dp.toPx() }

            // پرش نرم و ژله‌ای وقتی گربه حرف می‌زنه
            val jelly = remember { Animatable(0f) }
            LaunchedEffect(bubbleSay != null) {
                if (bubbleSay != null) {
                    while (true) {
                        jelly.animateTo(1f, spring(0.3f, Spring.StiffnessLow))
                        jelly.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
                    }
                } else {
                    jelly.animateTo(0f, spring(0.4f, Spring.StiffnessLow))
                }
            }

            if (ready && visible) {
                Layout(
                    modifier = Modifier.fillMaxSize(),
                    content = {
                        // ── نخ کاموا‌ها (پشت همه چیز) ──
                        Canvas(modifier = Modifier.layoutId("threads").fillMaxSize()) {
                            // این‌ها فقط برای اینه که با هر حرکت دوباره کشیده بشه
                            motion.menuProg.forEach { it.value }
                            motion.dlPos.forEach { it.value }
                            motion.offX.value
                            motion.offY.value
                            val u = 1.dp.toPx()
                            for (i in 0..2) {
                                val a = threadData[i * 6 + 4]
                                if (a <= 0.02f) continue
                                val bx = threadData[i * 6]
                                val by = threadData[i * 6 + 1]
                                val ax = threadData[i * 6 + 2]
                                val ay = threadData[i * 6 + 3]
                                val sag = threadData[i * 6 + 5] * u
                                val path = Path().apply {
                                    moveTo(ax, ay)
                                    quadraticBezierTo((ax + bx) / 2f, (ay + by) / 2f + sag, bx, by)
                                }
                                drawPath(
                                    path, YarnColors[i].copy(alpha = a),
                                    style = Stroke(width = 1.7f * u, cap = StrokeCap.Round)
                                )
                            }
                        }

                        // ── بالن پیام ──
                        Box(modifier = Modifier.layoutId("extras")) {
                            AnimatedVisibility(
                                visible = bubbleSay != null && !expanded,
                                enter = fadeIn(tween(220)) + scaleIn(
                                    spring(0.6f, Spring.StiffnessMediumLow),
                                    initialScale = 0.7f,
                                    transformOrigin = TransformOrigin(if (onRight) 1f else 0f, if (lowerHalf) 1f else 0f)
                                ),
                                exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.8f)
                            ) {
                                SpeechBubble(
                                    say = lastSay,
                                    onRight = onRight,
                                    lowerHalf = lowerHalf,
                                    onClick = {
                                        val s = lastSay
                                        bubbleSay = null
                                        activity++
                                        when (s.kind) {
                                            SayKind.DONE -> onDownloadsClick()
                                            SayKind.SUGGEST -> onSearchClick()
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                        }

                        // ── گلوله‌های کاموا (منو) ──
                        if (showMenu || dlActive) {
                            items.forEachIndexed { i, (icon, label, action) ->
                                if (dlActive || !offerOnly || i == 1) {
                                    Box(modifier = Modifier.layoutId("item$i")) {
                                        YarnButton(
                                            icon = icon,
                                            label = label,
                                            tint = YarnColors[i],
                                            progress = {
                                                if (dlActive) dlAlpha(motion.dlPos[i].value)
                                                else motion.menuProg[i].value
                                            },
                                            spin = {
                                                if (dlActive) dlSpin(motion.dlPos[i].value)
                                                else motion.menuProg[i].value * 600f
                                            },
                                            bob = { bob },
                                            flip = if (i % 2 == 0) 1f else -1f,
                                            onClick = {
                                                if (!dlActive) {
                                                    if (expanded) {
                                                        expanded = false
                                                        activity++
                                                        action()
                                                    } else if (offerOnly) {
                                                        offerOnly = false
                                                        showMenu = false
                                                        scope.launch { motion.menuProg[1].snapTo(0f) }
                                                        activity++
                                                        action()
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // ── خود گربه ──
                        Box(
                            modifier = Modifier
                                .layoutId("avatar")
                                .size(AvatarSize)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            activity++
                                            scope.launch { motion.hop(0.6f) }
                                            if (hidden) {
                                                hidden = false
                                            } else {
                                                bubbleSay = null
                                                expanded = !expanded
                                            }
                                        }
                                    )
                                }
                                .pointerInput(wPx, hPx, topLimit, bottomLimit) {
                                    detectDragGestures(
                                        onDragStart = {
                                            dragging = true
                                            expanded = false
                                            bubbleSay = null
                                            hidden = false
                                            activity++
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            scope.launch {
                                                motion.offX.snapTo(
                                                    (motion.offX.value + drag.x).coerceIn(0f, wPx - sizePx)
                                                )
                                                motion.offY.snapTo(
                                                    (motion.offY.value + drag.y).coerceIn(topLimit, bottomLimit)
                                                )
                                            }
                                            val target = (drag.x * 1.6f).coerceIn(-16f, 16f)
                                            if (abs(target - motion.lastLean) > 1.5f) {
                                                motion.lastLean = target
                                                scope.launch { motion.lean(target) }
                                            }
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            activity++
                                            motion.lastLean = 0f
                                            scope.launch {
                                                launch { motion.relax() }
                                                val right = motion.offX.value + sizePx / 2f > wPx / 2f
                                                onRight = right
                                                val tx = if (right) wPx - sizePx - marginPx else marginPx
                                                val ty = motion.offY.value.coerceIn(topLimit, bottomLimit)
                                                lowerHalf = ty + sizePx / 2f > hPx / 2f
                                                launch { motion.offX.animateTo(tx, spring(0.65f, Spring.StiffnessLow)) }
                                                launch { motion.offY.animateTo(ty, spring(0.75f, Spring.StiffnessMediumLow)) }
                                                // مثل گربه‌ها روی چهار پا فرود میاد
                                                launch { motion.landSquash() }
                                            }
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            motion.lastLean = 0f
                                            scope.launch { motion.relax() }
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        alpha = bodyAlpha
                                        transformOrigin = TransformOrigin(0.5f, 1f)
                                        rotationZ = motion.tilt.value
                                        translationY = bob * bobPx - jelly.value * jellyPx - motion.pounce.value * hopPx
                                        scaleY = 1f + 0.10f * jelly.value + 0.10f * scare - 0.14f * motion.land.value
                                        scaleX = 1f - 0.06f * jelly.value - 0.05f * scare + 0.10f * motion.land.value
                                    }
                                    .semantics { contentDescription = "دستیار" },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.matchParentSize(),
                                    shape = AvatarShape,
                                    color = surfaceColor,
                                    border = BorderStroke(1.5.dp, primary),
                                    shadowElevation = 10.dp,
                                    content = {}
                                )

                                Canvas(modifier = Modifier.matchParentSize()) {
                                    // وقتی کاموا از کنارش قل می‌خوره، پنجه‌اش بالا میاد
                                    var reach = 0f
                                    motion.menuProg.forEach { m ->
                                        val v = m.value
                                        if (v > 0f && v < 0.45f) {
                                            val r = sin(Math.PI * v / 0.45).toFloat()
                                            if (r > reach) reach = r
                                        }
                                    }
                                    val tail = ((tailSlow - 0.5f) * 2f) * (1f - playAmt) +
                                        ((tailFast - 0.5f) * 2f) * playAmt
                                    val pose = CatPose(
                                        face = face,
                                        blink = motion.blink.value,
                                        ear = motion.ear.value,
                                        tail = tail,
                                        mouth = maxOf(talk * speakAmt, reach * 0.7f, scare * 0.45f),
                                        paw = maxOf(reach, swat * playAmt, offerAmt * 0.75f),
                                        lookX = (bob - 0.5f) * 0.8f * (1f - playAmt) + (swat - 0.5f) * 2f * playAmt,
                                        lookY = -0.8f * playAmt + scare * 0.4f,
                                        scare = scare,
                                        happy = happyAmt,
                                        wiggle = (bob - 0.5f) * 2f * (1f + playAmt)
                                    )
                                    drawCat(pose)
                                }
                            }
                        }
                    }
                ) { measurables, constraints ->
                    val threadsM = measurables.first { it.layoutId == "threads" }
                    val avatarM = measurables.first { it.layoutId == "avatar" }
                    val extrasM = measurables.first { it.layoutId == "extras" }
                    val avatarSizeInt = sizePx.roundToInt()
                    val marginInt = marginPx.roundToInt()
                    val gapInt = with(density) { 10.dp.roundToPx() }
                    val maxExtrasW = min(
                        constraints.maxWidth - 2 * marginInt,
                        with(density) { 270.dp.roundToPx() }
                    ).coerceAtLeast(0)
                    val itemMaxW = with(density) { 130.dp.roundToPx() }

                    val threadsP = threadsM.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
                    val avatarP = avatarM.measure(Constraints.fixed(avatarSizeInt, avatarSizeInt))
                    val extrasP = extrasM.measure(Constraints(maxWidth = maxExtrasW))
                    val itemPs = List(3) { i ->
                        measurables.firstOrNull { it.layoutId == "item$i" }
                            ?.measure(Constraints(maxWidth = itemMaxW))
                    }

                    layout(constraints.maxWidth, constraints.maxHeight) {
                        threadsP.place(0, 0)

                        val baseX = motion.offX.value.roundToInt()
                        val ay = motion.offY.value.roundToInt()

                        // کاموا‌ها نسبت به جای «عادی» گربه دورش پخش می‌شن
                        val cx = baseX + avatarP.width / 2f
                        val cy = ay + avatarP.height / 2f
                        val dirIn = if (onRight) -1f else 1f
                        val radius = with(density) { 92.dp.toPx() }
                        val minYf = topInset
                        val maxYf = constraints.maxHeight - bottomInset
                        // پیام (بالن)
                        val rawX = if (onRight) baseX + avatarP.width - extrasP.width else baseX
                        val maxX = (constraints.maxWidth - marginInt - extrasP.width).coerceAtLeast(marginInt)
                        val ex = rawX.coerceIn(marginInt, maxX)
                        val rawY = if (lowerHalf) ay - extrasP.height - gapInt else ay + avatarP.height + gapInt
                        val minY = topInset.roundToInt()
                        val maxY = (constraints.maxHeight - bottomInset.roundToInt() - extrasP.height)
                            .coerceAtLeast(minY)
                        val ey = rawY.coerceIn(minY, maxY)
                        extrasP.place(ex, ey)

                        avatarP.place(baseX + motion.hideShift.value.roundToInt(), ay)

                        // کاموا‌ها از کنار پنجه‌ی گربه شروع می‌کنن، کمی بالا میان و بعد می‌رن سرجاشون
                        val pawX = baseX + with(density) { (30f + 7f * face).dp.toPx() }
                        val pawY = ay + with(density) { 50.dp.toPx() }
                        val liftPx = with(density) { 24.dp.toPx() }
                        val ballCenterFromTop = with(density) { 31.dp.toPx() }
                        val sagMenu = 16f

                        for (i in 0..2) threadData[i * 6 + 4] = 0f

                        itemPs.forEachIndexed { i, p ->
                            if (p != null) {
                                val rad = Math.toRadians(angles[i].toDouble())
                                val tx0 = cx + dirIn * radius * cos(rad).toFloat()
                                val ty0 = cy + radius * sin(rad).toFloat()
                                val loX = marginInt + p.width / 2f
                                val hiX = (constraints.maxWidth - marginInt - p.width / 2f).coerceAtLeast(loX)
                                val loY = minYf + p.height / 2f
                                val hiY = (maxYf - p.height / 2f).coerceAtLeast(loY)
                                val tx = tx0.coerceIn(loX, hiX)
                                val ty = ty0.coerceIn(loY, hiY)
                                var dx: Float
                                var dy: Float
                                if (dlActive) {
                                    // ۰→۱ از بالای صفحه آویزون می‌شه و بازیگوشانه می‌پره،
                                    // ۱→۲ قل می‌خوره سمت راست، ۲→۳ برمی‌گرده پیش گربه
                                    val q = motion.dlPos[i].value
                                    val topX = constraints.maxWidth * (0.25f + 0.25f * i)
                                    val pullY = topInset + with(density) { 150.dp.toPx() }
                                    val startY = -p.height.toFloat()
                                    val rightX = constraints.maxWidth - marginInt - p.width / 2f
                                    val rightY = pullY + i * (p.height + with(density) { 6.dp.toPx() })
                                    val bounceH = with(density) { 46.dp.toPx() }
                                    when {
                                        q <= 1f -> {
                                            dx = topX + sin(q * Math.PI * 4.0).toFloat() * with(density) { 10.dp.toPx() } * (1f - q)
                                            dy = startY + (pullY - startY) * q - dlBounce(q) * bounceH
                                        }
                                        q <= 2f -> {
                                            val f = q - 1f
                                            dx = topX + (rightX - topX) * f
                                            dy = pullY + (rightY - pullY) * f
                                        }
                                        else -> {
                                            val f = q - 2f
                                            dx = rightX + (pawX - rightX) * f
                                            dy = rightY + (pawY - rightY) * f
                                        }
                                    }
                                    // نخ: اول از بالای صفحه آویزونه، آخر مسیر به گربه وصله
                                    val hangAlpha = if (q <= 0f) 0f else (1f - (q - 1f) * 2f).coerceIn(0f, 1f)
                                    val returnAlpha = ((q - 2f)).coerceIn(0f, 1f) * dlAlpha(q)
                                    if (hangAlpha > 0f) {
                                        threadData[i * 6 + 2] = topX
                                        threadData[i * 6 + 3] = -with(density) { 20.dp.toPx() }
                                        threadData[i * 6 + 4] = hangAlpha
                                        threadData[i * 6 + 5] = 0f
                                    } else if (returnAlpha > 0f) {
                                        threadData[i * 6 + 2] = pawX
                                        threadData[i * 6 + 3] = pawY
                                        threadData[i * 6 + 4] = returnAlpha
                                        threadData[i * 6 + 5] = sagMenu
                                    }
                                } else {
                                    val prog = motion.menuProg[i].value
                                    dx = pawX + (tx - pawX) * prog
                                    dy = pawY + (ty - pawY) * prog - sin(Math.PI * prog.coerceIn(0f, 1f)).toFloat() * liftPx
                                    threadData[i * 6 + 2] = pawX
                                    threadData[i * 6 + 3] = pawY
                                    threadData[i * 6 + 4] = (prog * 0.9f).coerceIn(0f, 0.9f)
                                    threadData[i * 6 + 5] = sagMenu
                                }
                                threadData[i * 6] = dx
                                threadData[i * 6 + 1] = dy - p.height / 2f + ballCenterFromTop
                                p.place((dx - p.width / 2f).roundToInt(), (dy - p.height / 2f).roundToInt())
                            }
                        }
                    }
                }
            }
        }
    }
}
