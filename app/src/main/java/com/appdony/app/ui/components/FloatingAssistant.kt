package com.appdony.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.R

private val AssistantShape = RoundedCornerShape(24.dp)

/** دستیار شناور؛ هم‌سبک با منوی پایین: سطح هم‌رنگ سطح، بوردر فیروزه‌ای، آیکن‌های خطی. */
@Composable
fun FloatingAssistant(
    onSearchClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onChatClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary

    val items = listOf(
        Triple(Icons.Outlined.ChatBubbleOutline, "چت با دکتر", onChatClick),
        Triple(Icons.Outlined.Info, "راهنما", onHelpClick),
        Triple(Icons.Outlined.Download, "دانلودها", onDownloadsClick),
        Triple(Icons.Outlined.Search, "جستجو", onSearchClick)
    )

    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Bottom) {
        // حباب پیام
        AnimatedVisibility(
            visible = !isExpanded,
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
            exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f)
        ) {
            Surface(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 6.dp),
                border = BorderStroke(1.dp, primary.copy(alpha = 0.6f)),
                shadowElevation = 6.dp
            ) {
                Text(
                    "سلام! کمکی نیاز داری؟",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }

        // آیتم‌های منو؛ هرکدام با تأخیر کوچک، از پایین و با فنر وارد می‌شوند
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEachIndexed { index, (icon, label, action) ->
                val enterOrder = items.size - 1 - index // نزدیک‌ترین آیتم به دکمه اول وارد شود
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(180, delayMillis = enterOrder * 45)) +
                        slideInVertically(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) { it / 2 } +
                        scaleIn(tween(220, delayMillis = enterOrder * 45), initialScale = 0.7f),
                    exit = fadeOut(tween(120)) + slideOutVertically(tween(150)) { it / 2 } + scaleOut(tween(150), targetScale = 0.7f)
                ) {
                    AssistantMenuItem(icon = icon, label = label, onClick = { isExpanded = false; action() })
                }
            }
            Spacer(modifier = Modifier.size(2.dp))
        }

        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) 90f else 0f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
            label = "assistant_rotation"
        )

        Surface(
            modifier = Modifier
                .size(60.dp)
                .clip(AssistantShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { isExpanded = !isExpanded },
            shape = AssistantShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, primary),
            shadowElevation = 10.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isExpanded) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "بستن",
                        tint = primary,
                        modifier = Modifier.size(26.dp).rotate(rotation)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.plague_doctor),
                        contentDescription = "دستیار",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
