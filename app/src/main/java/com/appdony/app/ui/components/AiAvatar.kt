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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * آیکن زنده‌ی هوش مصنوعی: نفس کشیدن، پلک زدن، نگاه کردن به اطراف، تکون خوردن گوشی‌ها
 * و تپش چشم‌ها موقع حرف زدن [talking]. سیاه‌وسفیده و به رنگ تم برنامه وابسته نیست.
 * همه‌ی مقدارها فقط موقع کشیدن خونده می‌شن تا هر فریم کل کامپوننت دوباره ساخته نشه.
 */
@Composable
internal fun BuddyCanvas(
    modifier: Modifier = Modifier,
    talking: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "buddy")
    val breath = infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
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
    val jelly = remember { Animatable(0f) }

    // پلک زدن
    LaunchedEffect(Unit) {
        while (true) {
            delay((2_200L..4_800L).random())
            blink.animateTo(1f, tween(80))
            blink.animateTo(0f, tween(130))
        }
    }
    // تکون خوردن گوشی‌ها
    LaunchedEffect(Unit) {
        while (true) {
            delay((3_500L..8_000L).random())
            ear.animateTo(1f, tween(70))
            ear.animateTo(-0.5f, tween(70))
            ear.animateTo(0f, tween(140))
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
            translationY = -jelly.value * 0.06f * size.height
            scaleY = 1f + 0.06f * jelly.value
            scaleX = 1f - 0.04f * jelly.value
        }
    ) {
        // آیکن روی بوم ۶۰×۶۰ طراحی شده
        val k = size.width / 60.dp.toPx()
        withTransform({ scale(k, k, Offset.Zero) }) {
            drawCat(
                CatPose(
                    face = 1f,
                    blink = blink.value,
                    ear = ear.value,
                    tail = 0f,
                    mouth = talkT.value * talkAmt.value,
                    paw = 0f,
                    lookX = look.value * 0.7f,
                    lookY = 0f,
                    scare = 0f,
                    happy = 0f,
                    wiggle = 0f,
                    tilt = look.value * 0.3f,
                    breath = breath.value
                )
            )
        }
    }
}

/**
 * آیکن هوش مصنوعی اپدونی؛ برای جاهای ثابت اپ (چت، شروع و حساب کاربری).
 * کادر مشکی و ربات سفیده و رنگ تم روش اثر نداره.
 */
@Composable
fun CatAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) {
    val shape = RoundedCornerShape((sizeDp * 0.4f).dp)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
            .semantics { contentDescription = "هوش مصنوعی اپدونی" }
    ) {
        BuddyCanvas(modifier = Modifier.fillMaxSize(), talking = talking)
    }
}

/** اسم قدیمی؛ تا همه‌ی صفحه‌ها به [CatAvatar] برن، همین آیکن رو نشون می‌ده */
@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) = CatAvatar(modifier = modifier, sizeDp = sizeDp, hasShadow = hasShadow, talking = talking)
