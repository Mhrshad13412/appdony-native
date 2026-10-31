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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
private val RingColors = intArrayOf(
    Color(0x00FFB300).toArgb(),
    Color(0x00FFB300).toArgb(),
    PlusGoldLight.toArgb(),
    Color(0x00FFB300).toArgb()
)
private val RingStops = floatArrayOf(0f, 0.55f, 0.98f, 1f)

// نور نرم طلایی زیر کارت که آروم نفس می‌کشه
private fun Modifier.plusGlow(corner: Dp): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "plus_glow")
    val pulse = transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "plus_glow_pulse"
    )
    drawBehind {
        val r = corner.toPx()
        // عرض (dp) ، شفافیت ، جابه‌جایی به پایین (dp)
        val layers = listOf(
            Triple(12f, 0.05f, 5f),
            Triple(8f, 0.09f, 4f),
            Triple(4f, 0.14f, 3f)
        )
        for ((wDp, a, dyDp) in layers) {
            val w = wDp.dp.toPx()
            val half = w / 2f
            drawRoundRect(
                color = GoldColor.copy(alpha = a * pulse.value),
                topLeft = Offset(-half, -half + dyDp.dp.toPx()),
                size = Size(size.width + w, size.height + w),
                cornerRadius = CornerRadius(r + half),
                style = Stroke(width = w)
            )
        }
    }
}

// حلقه‌ی طلایی باریک دور کارت + یه نور که دورش می‌چرخه + برق ملایمی که هر چند ثانیه از رو کارت رد می‌شه
private fun Modifier.plusRing(corner: Dp): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "plus_ring")
    val angle = transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "plus_ring_angle"
    )
    val shine = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "plus_shine"
    )
    val paint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            style = android.graphics.Paint.Style.STROKE
        }
    }
    val matrix = remember { android.graphics.Matrix() }

    drawWithContent {
        drawContent()

        // برق کج: یه نوار نورانی که اریب از رو کارت رد می‌شه
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

        val r = corner.toPx()

        // حلقه‌ی ثابت و کم‌رنگ
        val thin = 1.5.dp.toPx()
        val h = thin / 2f
        drawRoundRect(
            color = GoldColor.copy(alpha = 0.55f),
            topLeft = Offset(h, h),
            size = Size(size.width - thin, size.height - thin),
            cornerRadius = CornerRadius(r - h),
            style = Stroke(width = thin)
        )

        // نورِ در حال چرخش
        val sw = 3.dp.toPx()
        val half = sw / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f
        val shader = android.graphics.SweepGradient(cx, cy, RingColors, RingStops)
        matrix.setRotate(angle.value, cx, cy)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        paint.strokeWidth = sw
        val rect = android.graphics.RectF(half, half, size.width - half, size.height - half)
        drawContext.canvas.nativeCanvas.drawRoundRect(rect, r - half, r - half, paint)
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
 * ظاهر طلاییِ تک‌رنگ: نور نرم زیر کارت، حلقه‌ی باریک با نور چرخان، برق کج و نقطه‌های متحرک.
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

    // فضای خالی دور کارت برای اینکه نور بیرون‌زده بریده نشه
    Box(modifier = modifier.padding(horizontal = 10.dp, vertical = 16.dp)) {
        Box(
            modifier = Modifier
                .width(300.dp)
                .height(200.dp)
                .plusGlow(24.dp)
                .appdonyCard()
                .plusRing(24.dp)
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
                NavChip(
                    text = category,
                    onImage = true,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                )
            }

            // «جدید» (تا ۴ روز بعد از انتشار یا ویرایش)
            if (isNew) {
                NewBadge(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
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
