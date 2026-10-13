package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSheet(
    displayName: String,
    username: String,
    onDismiss: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    onFavorites: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppdonyBg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // دکمه بستن
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "بستن",
                        tint = AppdonyTextPrimary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // کارت پروفایل
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppdonyCardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AppdonyGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName.ifBlank { "کاربر اپدونی" },
                        color = AppdonyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "@$username",
                        color = AppdonyTextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // آیتم‌های منو
            AccountMenuItem(
                icon = Icons.Default.Download,
                title = "دانلودهای من",
                onClick = {
                    onDownloads()
                    onDismiss()
                }
            )
            AccountMenuItem(
                icon = Icons.Default.Star,
                title = "علاقه‌مندی‌ها",
                onClick = {
                    onFavorites()
                    onDismiss()
                }
            )
            AccountMenuItem(
                icon = Icons.Default.Settings,
                title = "تنظیمات",
                onClick = {
                    onSettings()
                    onDismiss()
                }
            )
            AccountMenuItem(
                icon = Icons.Default.Info,
                title = "درباره اپدونی",
                onClick = {
                    onAbout()
                    onDismiss()
                }
            )

            Spacer(Modifier.height(8.dp))

            // دکمه خروج
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppdonyCardBg)
                    .clickable { onLogout() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "خروج از حساب",
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppdonyTextPrimary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            color = AppdonyTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
