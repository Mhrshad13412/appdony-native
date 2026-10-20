package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FloatingAssistant(
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (!isExpanded) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 8.dp, end = 4.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            ) {
                Text(
                    text = "سلام! کمکی نیاز داری؟",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.5f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.5f)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistantMenuItem(icon = Icons.AutoMirrored.Filled.Chat, label = "چت با دکتر", onClick = { isExpanded = false; onChatClick() })
                AssistantMenuItem(icon = Icons.AutoMirrored.Filled.Help, label = "راهنما", onClick = { isExpanded = false; onHelpClick() })
                AssistantMenuItem(icon = Icons.Default.Download, label = "دانلودها", onClick = { isExpanded = false; onDownloadsClick() })
                AssistantMenuItem(icon = Icons.Default.Search, label = "جستجو", onClick = { isExpanded = false; onSearchClick() })
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            // ... (بقیه کد اصلی این باکس)
        }
    }
}

@Composable
private fun AssistantMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    // ... (کد اصلی AssistantMenuItem)
}
