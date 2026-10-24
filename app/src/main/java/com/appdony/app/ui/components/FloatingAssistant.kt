package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.R

@Composable
fun FloatingAssistant(
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
        if (!isExpanded) {
            Surface(
                modifier = Modifier.padding(bottom = 8.dp, end = 4.dp).shadow(4.dp, RoundedCornerShape(16.dp)),
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
            ) {
                Text("سلام! کمکی نیاز داری؟", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.5f),
            exit = fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.5f)
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantMenuItem(icon = Icons.Default.Email, label = "چت با دکتر", onClick = { isExpanded = false; onChatClick() })
                AssistantMenuItem(icon = Icons.Default.Info, label = "راهنما", onClick = { isExpanded = false; onHelpClick() })
                AssistantMenuItem(icon = Icons.Default.Download, label = "دانلودها", onClick = { isExpanded = false; onDownloadsClick() })
                AssistantMenuItem(icon = Icons.Default.Search, label = "جستجو", onClick = { isExpanded = false; onSearchClick() })
                Spacer(modifier = Modifier.padding(bottom = 4.dp))
            }
        }

        Box(
            modifier = Modifier.size(64.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center
        ) {
            if (isExpanded) {
                Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(28.dp))
            } else {
                Image(painter = painterResource(id = R.drawable.plague_doctor), contentDescription = "دستیار", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun AssistantMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.shadow(2.dp, RoundedCornerShape(12.dp)).clip(RoundedCornerShape(12.dp)).clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
    }
}
