package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.appdony.app.R

@Composable
fun PlagueDoctorAvatar(
    modifier: Modifier = Modifier,
    sizeDp: Int = 70,
    hasShadow: Boolean = true
) {
    // انیمیشن شناور بودن (بالا و پایین رفتن نرم)
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .offset(y = floatY.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary) // رنگ اصلی اپ پشت عکس
            .padding(6.dp), // یه حاشیه کوچیک که عکس تو دایره قشنگ بشینه
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // 👇 اینجا عکسی که آپلود کردی رو فراخوانی می‌کنیم
        Image(
            painter = painterResource(id = R.drawable.plague_doctor),
            contentDescription = "دستیار اپدونی",
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize()
        )
    }
}
