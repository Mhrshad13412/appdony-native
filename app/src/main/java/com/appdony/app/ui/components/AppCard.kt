package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun AppCard(title: String, imageUrl: String?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val imageRequest = ImageRequest.Builder(context).data(imageUrl).size(250).crossfade(true).build()

    Column(
        modifier = modifier.width(100.dp).clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp).clip(RoundedCornerShape(16.dp)).background(AppdonyCardBg)) {
            SubcomposeAsyncImage(
                model = imageRequest, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0) },
                error = {
                    Box(modifier = Modifier.fillMaxSize().background(AppdonyCardBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, "خطا", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                },
                success = { SubcomposeAsyncImageContent() }
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title, color = AppdonyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium,
            maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, lineHeight = 16.sp
        )
    }
}
