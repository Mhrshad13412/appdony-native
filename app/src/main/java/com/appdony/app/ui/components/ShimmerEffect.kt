package com.appdony.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -900f,
        targetValue = 1700f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // نور براق: رنگ پایه + یه لایه‌ی روشن‌تر که مورب رد می‌شه
    val base = MaterialTheme.colorScheme.surfaceVariant
    val shine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f).compositeOver(base)

    return Brush.linearGradient(
        colors = listOf(base, shine, base),
        // مورب: نور براق کج (از بالا-چپ به پایین-راست) رد می‌شه
        start = Offset(translateAnim, 0f),
        end = Offset(translateAnim + 320f, 320f)
    )
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, cornerRadius: Int = 12) {
    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius.dp)).background(ShimmerBrush()))
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
