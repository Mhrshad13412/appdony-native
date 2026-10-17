package com.appdony.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val bg = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(
        colors = listOf(
            bg,
            bg.copy(alpha = 0.4f),
            bg
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(translateAnim + 400f, translateAnim + 400f)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(ShimmerBrush())
    )
}

@Composable
fun CardSkeleton() {
    Column(
        modifier = Modifier.width(110.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ShimmerBox(modifier = Modifier.size(100.dp), cornerRadius = 20)
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(10.dp), cornerRadius = 4)
    }
}

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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardSkeleton()
        CardSkeleton()
        CardSkeleton()
    }
}

@Composable
fun SectionTitleSkeleton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(4.dp, 18.dp), cornerRadius = 2)
        Spacer(modifier = Modifier.width(8.dp))
        ShimmerBox(modifier = Modifier.width(120.dp).height(16.dp), cornerRadius = 4)
    }
}

@Composable
fun BlogCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(200.dp), cornerRadius = 16)
        Spacer(modifier = Modifier.height(10.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(6.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp), cornerRadius = 4)
        Spacer(modifier = Modifier.height(4.dp))
        ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp), cornerRadius = 4)
    }
}

@Composable
fun DetailSkeleton() {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(260.dp), cornerRadius = 0)
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(78.dp), cornerRadius = 18)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                ShimmerBox(modifier = Modifier.width(200.dp).height(16.dp))
                Spacer(modifier = Modifier.height(8.dp))
                ShimmerBox(modifier = Modifier.width(100.dp).height(12.dp))
            }
        }
        ShimmerBox(modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp), cornerRadius = 14)
        ShimmerBox(modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp), cornerRadius = 14)
        Column(modifier = Modifier.padding(16.dp)) {
            ShimmerBox(modifier = Modifier.width(100.dp).height(16.dp))
            Spacer(modifier = Modifier.height(12.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(12.dp))
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp))
        }
    }
}
