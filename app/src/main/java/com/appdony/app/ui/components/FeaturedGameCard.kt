package com.appdony.app.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest

/**
 * کارت «ویژه»: اسکرین‌شات بازی بزرگ پشت کارته،
 * آیکن برنامه پایین سمت راست و اسم بازی کنار آیکن.
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
    val primary = scheme.primary

    Box(
        modifier = modifier
            .width(300.dp)
            .height(200.dp)
            .appdonyCard()
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
                    .border(1.5.dp, primary, RoundedCornerShape(16.dp))
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
                            Icon(Icons.Filled.Android, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(androidReq, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        if (version != null) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, tint = primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(version, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 1, style = TextStyle(textDirection = TextDirection.Ltr))
                        }
                    }
                }
            }
        }
    }
}
