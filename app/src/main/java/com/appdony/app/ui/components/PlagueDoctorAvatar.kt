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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
 * گربه‌ی اپدونی؛ همون گربه‌ی شناور، برای جاهای ثابت اپ (چت، صفحه‌ی خالی، شروع و حساب کاربری).
 * پلک می‌زنه، گوشش تکون می‌خوره، دمش تاب می‌خوره و موقع حرف زدن [talking] دهنش باز و بسته می‌شه.
 * رنگ گربه و کاموا از رنگ تم برنامه گرفته می‌شه.
 */
@Composable
fun CatAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) {
    val themeColor = MaterialTheme.colorScheme.primary
    val infinite = rememberInfiniteTransition(label = "cat_avatar")
    val floatY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val tailT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail"
    )
    val talkT by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "talk"
    )
    val talkAmt by animateFloatAsState(
        targetValue = if (talking) 1f else 0f,
        animationSpec = tween(300),
        label = "talk_amt"
    )

    // پلک زدن و تکون خوردن گوش
    val blink = remember { Animatable(0f) }
    val ear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((2_200L..4_800L).random())
            blink.animateTo(1f, tween(80))
            blink.animateTo(0f, tween(130))
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay((3_500L..8_000L).random())
            ear.animateTo(1f, tween(70))
            ear.animateTo(-0.5f, tween(70))
            ear.animateTo(0f, tween(140))
        }
    }

    // پرش نرم و ژله‌ای موقع حرف زدن
    val jelly = remember { Animatable(0f) }
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

    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape((sizeDp * 0.34f).dp)

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
            .graphicsLayer {
                transformOrigin = TransformOrigin(0.5f, 1f)
                translationY = -jelly.value * (sizeDp * 0.12f).dp.toPx()
                scaleY = 1f + 0.12f * jelly.value
                scaleX = 1f - 0.08f * jelly.value
            }
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .border(1.5.dp, primary, shape)
            .semantics { contentDescription = "گربه‌ی اپدونی" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            // گربه روی بوم ۶۰×۶۰ طراحی شده؛ کمی کوچیک‌ترش می‌کنیم تا گوشه‌ها نبرنش
            val k = size.width / (60.dp.toPx()) * 0.92f
            withTransform({
                translate(size.width * 0.04f, size.height * 0.04f)
                scale(k, k, Offset.Zero)
            }) {
                drawCat(
                    CatPose(
                        face = 1f,
                        blink = blink.value,
                        ear = ear.value,
                        tail = (tailT - 0.5f) * 2f,
                        mouth = talkT * talkAmt,
                        paw = 0f,
                        lookX = (floatY / 6f - 0.5f) * 0.8f,
                        lookY = 0f,
                        scare = 0f,
                        happy = 0f,
                        wiggle = (floatY / 6f - 0.5f) * 2f,
                        yarn = themeColor
                    )
                )
            }
        }
    }
}

/** اسم قدیمی؛ تا همه‌ی صفحه‌ها به [CatAvatar] برن، همین گربه رو نشون می‌ده */
@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true,
    talking: Boolean = false
) = CatAvatar(modifier = modifier, sizeDp = sizeDp, hasShadow = hasShadow, talking = talking)
