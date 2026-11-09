package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * عکس پروفایل کاربر از خودِ وردپرس (avatar_urls).
 * تا وقتی عکس نیومده یا اگه نیاد، حرف اول اسم توی دایره‌ی رنگی نشون داده می‌شه.
 */
@Composable
fun UserAvatar(
    url: String,
    initial: String,
    modifier: Modifier = Modifier,
    textSize: TextUnit = 18.sp
) {
    var loaded by remember(url) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (!loaded) {
            Text(
                text = initial.trim().firstOrNull()?.uppercase() ?: "U",
                color = Color.White,
                fontSize = textSize,
                fontWeight = FontWeight.Bold
            )
        }
        if (url.isNotBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "عکس پروفایل",
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
