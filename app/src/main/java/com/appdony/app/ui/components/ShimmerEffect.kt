package com.appdony.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp

/** یک دور رفتنِ نور از راست به چپ (میلی‌ثانیه) */
private const val SHIMMER_PERIOD_MS = 1300L

/**
 * نور براقِ کج که از راست به چپ رد می‌شه (چیدمان فارسی/RTL: همون جهتی که منوها باز می‌شن).
 * همه‌ی جعبه‌ها از یه ساعت مشترک پیروی می‌کنن، پس کنار هم هماهنگ و هم‌فاز می‌رن.
 * فقط مرحله‌ی «رسم» دوباره اجرا می‌شه، نه کل ترکیب.
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Int = 12) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    // فقط برای این‌که هر فریم دوباره رسم بشه؛ مقدار واقعی از ساعت مشترک گرفته می‌شه
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = SHIMMER_PERIOD_MS.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_tick"
    )

    val base = MaterialTheme.colorScheme.surfaceVariant
    val shine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f).compositeOver(base)
    val colors = listOf(base, shine, base)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .drawBehind {
                val progress = (SystemClock.uptimeMillis() % SHIMMER_PERIOD_MS).toFloat() / SHIMMER_PERIOD_MS + tick * 0f
                val band = size.width.coerceAtLeast(240f) * 0.55f
                // نوار نور از لبه‌ی راست وارد می‌شه و از لبه‌ی چپ بیرون می‌ره
                val x0 = (size.width + band) * (1f - progress)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = colors,
                        // جهت: بالا-راست به پایین-چپ؛ پس نور کج «\» رد می‌شه (آینه‌ی حالت LTR)
                        start = Offset(x0, 0f),
                        end = Offset(x0 - band, band * 0.45f)
                    )
                )
            }
    )
}

@Composable
fun CardSkeleton() {
    Column(modifier = Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ShimmerBox(modifier = Modifier.size(100.dp), cornerRadius = 20)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(10.dp), cornerRadius = 4)
    }
}

// 👇 این تابع همان BannerSkeleton هست که ارور می داد
@Composable
fun BannerSkeleton() {
    Column(modifier = Modifier.width(280.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(170.dp), cornerRadius = 18)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 4)
    }
}

@Composable
fun SectionRowSkeleton() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CardSkeleton()
        CardSkeleton()
        CardSkeleton()
    }
}

@Composable
fun SectionTitleSkeleton() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ShimmerBox(modifier = Modifier.size(4.dp, 18.dp), cornerRadius = 2)
        Spacer(modifier = Modifier.width(8.dp))
        ShimmerBox(modifier = Modifier.width(120.dp).height(16.dp), cornerRadius = 4)
    }
}

@Composable
fun BlogCardSkeleton() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp), cornerRadius = 16)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp), cornerRadius = 4)
    }
}

@Composable
fun DetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(280.dp), cornerRadius = 0)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(80.dp), cornerRadius = 20)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                ShimmerBox(modifier = Modifier.width(220.dp).height(18.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.width(100.dp).height(14.dp))
            }
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp), cornerRadius = 16)
        Spacer(modifier = Modifier.height(12.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp), cornerRadius = 16)
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(modifier = Modifier.width(100.dp).height(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp))
        }
    }
}

@Composable
fun DownloadCardSkeleton() {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(64.dp), cornerRadius = 32)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(15.dp), cornerRadius = 6)
                    Spacer(modifier = Modifier.height(10.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(11.dp), cornerRadius = 6)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShimmerBox(modifier = Modifier.width(96.dp).height(36.dp), cornerRadius = 20)
                ShimmerBox(modifier = Modifier.width(36.dp).height(36.dp), cornerRadius = 18)
            }
        }
    }
}

/** یه ردیفِ لیست نوشته‌ها/بلاگ‌ها (هم‌اندازه‌ی کارت واقعی) */
@Composable
fun PostRowSkeleton() {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassTrack(shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(64.dp), cornerRadius = 14)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp), cornerRadius = 6)
            Spacer(modifier = Modifier.height(6.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp), cornerRadius = 6)
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.width(56.dp).height(16.dp), cornerRadius = 8)
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerBox(modifier = Modifier.width(80.dp).height(10.dp), cornerRadius = 5)
            }
        }
    }
}

/** لودینگ لیست نوشته‌ها و بلاگ‌ها: چند ردیف خالیِ نورانی به‌جای چرخان */
@Composable
fun PostListSkeleton(count: Int = 8) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) { PostRowSkeleton() }
    }
}

/** لودینگ ادیتور: عنوان بزرگ + چند بلوک (پاراگراف، تصویر، فهرست) که با نور پر می‌شن */
@Composable
fun EditorLoadSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 20)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                ShimmerBox(modifier = Modifier.width(120.dp).height(14.dp), cornerRadius = 6)
                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.width(70.dp).height(10.dp), cornerRadius = 5)
            }
            ShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 20)
            Spacer(modifier = Modifier.width(6.dp))
            ShimmerBox(modifier = Modifier.size(40.dp), cornerRadius = 20)
        }
        Spacer(modifier = Modifier.height(28.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(26.dp), cornerRadius = 8)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.62f).height(26.dp), cornerRadius = 8)
        Spacer(modifier = Modifier.height(24.dp))
        ShimmerBox(modifier = Modifier.width(110.dp).height(18.dp), cornerRadius = 6)
        Spacer(modifier = Modifier.height(14.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(13.dp), cornerRadius = 5)
        Spacer(modifier = Modifier.height(9.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(13.dp), cornerRadius = 5)
        Spacer(modifier = Modifier.height(9.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.8f).height(13.dp), cornerRadius = 5)
        Spacer(modifier = Modifier.height(22.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(170.dp), cornerRadius = 18)
        Spacer(modifier = Modifier.height(22.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(13.dp), cornerRadius = 5)
        Spacer(modifier = Modifier.height(9.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(13.dp), cornerRadius = 5)
    }
}
