package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import kotlin.math.PI
import kotlin.math.sin

// استایل اپدونی پلاس: فقط یه رنگ (طلایی)
private val PlusGoldLight = Color(0xFFFFE082)

// نور نرم و محوِ طلایی زیر کارت (بدون لایه‌ی مشخص)؛ آروم نفس می‌کشه
private fun Modifier.plusGlow(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "plus_glow")
    val pulse = transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "plus_glow_pulse"
    )
    drawBehind {
        val cx = size.width / 2f
        val cy = size.height
        val radius = size.width * 0.58f
        val k = pulse.value
        // یه دایره‌ی گرادیانی که پهن‌ و کم‌ارتفاع شده؛ از لبه‌ی پایین کارت به بیرون محو می‌شه
        withTransform({ scale(scaleX = 1f, scaleY = 0.17f, pivot = Offset(cx, cy)) }) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldColor.copy(alpha = 0.42f * k),
                        GoldColor.copy(alpha = 0.27f * k),
                        GoldColor.copy(alpha = 0.13f * k),
                        GoldColor.copy(alpha = 0.04f * k),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

// برق کج: یه نوار نورانی که هر چند ثانیه اریب از رو کارت رد می‌شه
private fun Modifier.plusShine(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "plus_shine")
    val shine = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "plus_shine_t"
    )
    drawWithContent {
        drawContent()
        val p = shine.value
        if (p < 0.6f) {
            val t = p / 0.6f
            val band = size.width * 0.40f
            val x = -band + t * (size.width + band * 2f)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        GoldColor.copy(alpha = 0.10f),
                        PlusGoldLight.copy(alpha = 0.32f),
                        GoldColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    start = Offset(x, 0f),
                    end = Offset(x + band, band * 0.6f)
                ),
                size = size
            )
        }
    }
}

// چیپ دسته‌بندی و نشان «جدید» به رنگ طلایی (مخصوص کارت پلاس؛ بقیه‌ی کارت‌ها همون رنگ تم)
@Composable
private fun PlusChip(text: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .background(GoldColor.copy(alpha = 0.14f))
            .border(1.dp, GoldColor, shape)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = GoldColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlusNewBadge(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .background(GoldColor.copy(alpha = 0.14f))
            .border(1.dp, GoldColor, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = GoldColor, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = "جدید", color = GoldColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

// نقطه‌های طلایی که آروم پیدا و محو می‌شن (مثل چت جمنای)؛ بیشتر پایین کارت
@Composable
private fun PlusDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "plus_dots")
    val t = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "plus_dots_t"
    )
    Canvas(modifier = modifier) {
        val step = 11.dp.toPx()
        val cols = (size.width / step).toInt() + 1
        val rows = (size.height / step).toInt() + 1
        val phase = t.value * 2f * PI.toFloat()
        for (row in 0..rows) {
            val y = row * step
            val yn = (y / size.height).coerceIn(0f, 1f)
            val vertical = yn * yn
            if (vertical < 0.06f) continue
            val rad = (0.6f + 1.1f * yn).dp.toPx()
            val shift = if (row % 2 == 0) 0f else step / 2f
            for (col in 0..cols) {
                val x = col * step + shift
                // موج مورب و نرم؛ هر نقطه کم‌کم روشن و خاموش می‌شه
                val wave = 0.5f + 0.5f * sin(phase - (x + y) * 0.02f)
                val a = vertical * wave * wave * 0.55f
                if (a < 0.02f) continue
                drawCircle(
                    color = GoldColor.copy(alpha = a.coerceAtMost(1f)),
                    radius = rad,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * کارت «ویژه» (اپدونی پلاس): اسکرین‌شات بازی بزرگ پشت کارته،
 * آیکن برنامه پایین سمت راست و اسم بازی کنار آیکن.
 * ظاهر طلاییِ تک‌رنگ: نور محو زیر کارت، برق کج و نقطه‌های متحرک؛ چیپ‌ها هم طلایی‌ان.
 */
@Composable
fun FeaturedGameCard(
    title: String,
    category: String?,
    screenshotUrl: String?,
    iconUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNew: Boolean = false,
    androidReq: String? = null,
    version: String? = null
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    // فضای خالی (بیشتر پایین) برای اینکه نور زیر کارت بریده نشه
    Box(modifier = modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 32.dp)) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(200.dp)
                .plusGlow()
                .appdonyCard()
                .plusShine()
                .clickable { onClick() }
        ) {
            // اسکرین‌شات (اگه پیدا نشد خودِ تصویر شاخص)
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(screenshotUrl ?: iconUrl)
                    .size(700, 420)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0) },
                error = { Box(modifier = Modifier.fillMaxSize().background(scheme.surfaceVariant)) },
                success = { SubcomposeAsyncImageContent() }
            )

            // سایه‌ی پایین کارت تا نوشته‌ها همیشه خوانا باشن
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                            startY = 120f
                        )
                    )
            )

            // نقطه‌های متحرک (زیر نوشته‌ها)
            PlusDots(modifier = Modifier.fillMaxSize())

            // دسته‌بندی، بالای کارت
            if (!category.isNullOrBlank()) {
                PlusChip(
                    text = category,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                )
            }

            // «جدید» (تا ۴ روز بعد از انتشار یا ویرایش)
            if (isNew) {
                PlusNewBadge(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
            }

            // پایین کارت: آیکن برنامه (سمت راست، چون اپ راست‌به‌چپه) + اسم بازی
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surface)
                        .border(1.5.dp, GoldColor, RoundedCornerShape(16.dp))
                ) {
                    if (!iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = iconUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    if (androidReq != null || version != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (androidReq != null) {
                                Icon(Icons.Filled.Android, contentDescription = null, tint = GoldColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(androidReq, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            if (version != null) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(version, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
                            }
                        }
                    }
                }
            }
        }
    }
}
