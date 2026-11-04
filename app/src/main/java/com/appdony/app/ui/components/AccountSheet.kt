package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * منوی اکانت: کارت شناور، هم‌سبک با لاگین. هر دکمه اول منو را می‌بندد و بعد عمل خودش را انجام می‌دهد.
 * برای مهمان (بدون حساب): کارت کاربر و «خروج از حساب» مخفی‌اند و به‌جاشون «ورود یا ثبت‌نام» میاد.
 */
@Composable
fun AccountSheet(
    isLoggedIn: Boolean,
    displayName: String, username: String, onDismiss: () -> Unit,
    onDownloads: () -> Unit, onSettings: () -> Unit, onFavorites: () -> Unit,
    onAbout: () -> Unit, onLogin: () -> Unit, onLogout: () -> Unit
) {
    FloatingDialog(onDismiss = onDismiss) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isLoggedIn) "حساب کاربری" else "منو",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            val glass = LocalGlassMode.current
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (glass) Modifier
                            .background(Color.White.copy(alpha = if (isGlassDark()) 0.05f else 0.35f))
                            .border(1.dp, glassRimBrush(), CircleShape)
                        else Modifier
                    )
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (isLoggedIn) {
            val userCardShape = RoundedCornerShape(20.dp)
            val glassUser = LocalGlassMode.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(userCardShape)
                    // شیشه‌ای: کاشی روشنِ کم‌رنگ + لبه‌ی منو (مثل کاشی انتخاب‌شده‌ی منوی پایین)، نه یه مستطیل مات
                    .then(
                        if (glassUser) Modifier
                            .background(Color.White.copy(alpha = if (isGlassDark()) 0.04f else 0.45f))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(1.dp, glassRimBrush(), userCardShape)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.firstOrNull()?.uppercase() ?: "U",
                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(text = displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "@$username", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
        } else {
            AccountRow(
                Icons.Outlined.Person, "ورود یا ثبت‌نام",
                tint = MaterialTheme.colorScheme.primary
            ) { onDismiss(); onLogin() }
        }

        AccountRow(Icons.Outlined.Download, "دانلودها") { onDismiss(); onDownloads() }
        AccountRow(Icons.Outlined.StarBorder, "علاقه‌مندی‌ها") { onDismiss(); onFavorites() }
        AccountRow(Icons.Outlined.Settings, "تنظیمات") { onDismiss(); onSettings() }
        AccountRow(Icons.Outlined.Info, "درباره اپدونی") { onDismiss(); onAbout() }
        if (isLoggedIn) {
            AccountRow(
                Icons.AutoMirrored.Outlined.Logout, "خروج از حساب",
                tint = MaterialTheme.colorScheme.error
            ) { onDismiss(); onLogout() }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun AccountRow(
    icon: ImageVector,
    title: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val iconShape = RoundedCornerShape(12.dp)
    val glass = LocalGlassMode.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp, horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(iconShape)
                .then(
                    // شیشه‌ای: کاشی کم‌رنگِ هم‌رنگ آیکن + همون لبه‌ی منو (به‌جای بوردرِ رنگی)
                    if (glass) Modifier
                        .background(tint.copy(alpha = 0.10f))
                        .border(1.dp, glassRimBrush(), iconShape)
                    else Modifier.background(tint.copy(alpha = 0.08f))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text = title, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** درباره اپدونی: کارت شناور ساده. */
@Composable
fun AboutDialog(versionName: String, onDismiss: () -> Unit) {
    FloatingDialog(onDismiss = onDismiss) {
        Spacer(Modifier.height(8.dp))
        PlagueDoctorAvatar(sizeDp = 72)
        Spacer(Modifier.height(18.dp))
        Text("اپدونی", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("نسخه $versionName", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "دانلود برنامه و بازی اندروید با لینک مستقیم و پشتیبانی فارسی",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text("باشه", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(4.dp))
    }
}
