package com.appdony.app.ui.components

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * خود آدمک با همه‌ی انیمیشن‌های بیکاری‌اش: نفس کشیدن، تاب خوردن، پلک زدن، نگاه کردن به اطراف،
 * تکون خوردن هدفون، پریدن کوچولو، سلام دادن گاه‌گاهی و باز و بسته شدن دهن موقع حرف زدن [talking].
 * [bust] = فقط سر و شونه (برای دکمه‌ی گرد وسط منو). همه‌ی مقدارها فقط موقع کشیدن خونده می‌شن
 * تا هر فریم کل کامپوننت دوباره ساخته نشه.
 */
@Composable
internal fun BuddyCanvas(
    modifier: Modifier = Modifier,
    talking: Boolean = false,
    bust: Boolean = false
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val infinite = rememberInfiniteTransition(label = "buddy")
    val breath = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val sway = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway"
    )
    val look = infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "look"
    )
    val talkT = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "talk"
    )
    val talkAmt = animateFloatAsState(
        targetValue = if (talking) 1f else 0f,
        animationSpec = tween(300),
        label = "talk_amt"
    )

    val blink = remember { Animatable(0f) }
    val ear = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val waveT = remember { Animatable(0f) }
    val hop = remember { Animatable(0f) }
    val jelly = remember { Animatable(0f) }

    // پلک زدن
    LaunchedEffect(Unit) {
        while (true) {
            delay((2_200L..4_800L).random())
            blink.animateTo(1f, tween(80))
            blink.animateTo(0f, tween(130))
        }
    }
    // تکون خوردن هدفون
    LaunchedEffect(Unit) {
        while (true) {
            delay((3_500L..8_000L).random())
            ear.animateTo(1f, tween(70))
            ear.animateTo(-0.5f, tween(70))
            ear.animateTo(0f, tween(140))
        }
    }
    // سلام دادن: دست میره بالا، چند بار تکون می‌خوره، برمی‌گرده
    LaunchedEffect(Unit) {
        delay((1_800L..3_500L).random())
        while (true) {
            wave.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            repeat(4) {
                waveT.animateTo(1f, tween(170))
                waveT.animateTo(-1f, tween(170))
            }
            waveT.animateTo(0f, tween(140))
            wave.animateTo(0f, tween(380, easing = FastOutSlowInEasing))
            delay((6_000L..11_000L).random())
        }
    }
    // پرش کوچولوی ذوق‌زده
    LaunchedEffect(Unit) {
        while (true) {
            delay((7_000L..13_000L).random())
            hop.animateTo(1f, tween(170, easing = FastOutSlowInEasing))
            hop.animateTo(0f, spring(0.4f, Spring.StiffnessMedium))
        }
    }
    // پرش نرم و ژله‌ای موقع حرف زدن
    LaunchedEffect(talking) {
        if (talking) {
            while (true) {
                jelly.animateTo(1f, spring(0.3f, Spring.StiffnessLow))
                jelly.animateTo(0f, spring(0.3f, Spring.StiffnessLow))
            }
        } else {
            jelly.animateTo(0f, spring(0.4f, Spring.StiffnessLow))
        }
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            transformOrigin = TransformOrigin(0.5f, 1f)
            translationY = -(jelly.value * 0.10f + hop.value * 0.10f) * size.height
            scaleY = 1f + 0.10f * jelly.value - 0.05f * hop.value
            scaleX = 1f - 0.06f * jelly.value + 0.04f * hop.value
        }
    ) {
        val unit = 60.dp.toPx()
        // آدمک روی بوم ۶۰×۶۰ طراحی شده
        val k = if (bust) size.width / unit * 1.3f else size.width / unit * 0.92f
        val tx = if (bust) size.width / 2f - k * 30.dp.toPx() else size.width * 0.04f
        val ty = if (bust) size.height / 2f - k * 25.5.dp.toPx() else size.height * 0.04f
        val w = wave.value
        withTransform({
            translate(tx, ty)
            scale(k, k, Offset.Zero)
        }) {
            drawCat(
                CatPose(
                    face = 1f,
                    blink = blink.value,
                    ear = ear.value,
                    tail = (sway.value - 0.5f),
                    mouth = talkT.value * talkAmt.value,
                    paw = 0f,
                    lookX = look.value * 0.7f,
                    lookY = 0f,
                    scare = 0f,
                    happy = w * 0.85f,
                    wiggle = (breath.value - 0.5f) * 2f,
                    yarn = themeColor,
                    wave = w,
                    waveT = waveT.value,
                    tilt = look.value * 0.5f,
                    breath = breath.value
                )
            )
        }
    }
}

/**
 * آدمک اپدونی؛ برای جاهای ثابت اپ (چت، صفحه‌ی خالی، شروع و حساب کاربری).
 * رنگ هودی و هدفونش از رنگ تم برنامه گرفته می‌شه.
 */
@Composable
fun CatAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape((sizeDp * 0.34f).dp)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .border(1.5.dp, primary, shape)
            .semantics { contentDescription = "آدمک اپدونی" },
        contentAlignment = Alignment.Center
    ) {
        BuddyCanvas(
            modifier = Modifier.fillMaxSize().padding((sizeDp * 0.02f).dp),
            talking = talking
        )
    }
}

/** اسم قدیمی؛ تا همه‌ی صفحه‌ها به [CatAvatar] برن، همین آدمک رو نشون می‌ده */
@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) = CatAvatar(modifier = modifier, sizeDp = sizeDp, hasShadow = hasShadow, talking = talking)
