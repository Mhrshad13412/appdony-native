package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "به اپدونی خوش آمدید! 👋",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "اینجا مرجع دانلود اپلیکیشن و بازی‌های اندروید است. بذار یه راهنمای سریع بهت بدم:",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(24.dp))

            OnboardingItem(
                icon = Icons.Default.VideogameAsset,
                title = "بازی‌ها",
                desc = "جدیدترین و محبوب‌ترین بازی‌های اندروید با لینک مستقیم."
            )
            OnboardingItem(
                icon = Icons.Default.Android,
                title = "برنامه‌ها",
                desc = "بهترین اپلیکیشن‌های کاربردی و شبکه‌های اجتماعی."
            )
            OnboardingItem(
                icon = Icons.Default.Article,
                title = "وبلاگ",
                desc = "اخبار داغ دنیای گیم و تکنولوژی، همراه با نقد و بررسی."
            )
            OnboardingItem(
                icon = Icons.Default.Download,
                title = "دانلودها",
                desc = "مدیریت فایل‌های در حال دانلود و دانلود شده."
            )
            OnboardingItem(
                icon = Icons.Default.Search,
                title = "جستجو",
                desc = "پیدا کردن سریع هر بازی یا برنامه‌ای که دنبالش هستی."
            )
            OnboardingItem(
                icon = Icons.Default.Person,
                title = "حساب کاربری",
                desc = "با حساب وردپرسی سایت وارد شو تا بتونی نظر بذاری و برنامه‌ها رو ذخیره کنی."
            )
            OnboardingItem(
                icon = Icons.Default.Star,
                title = "اپدونی پلاس",
                desc = "بازی‌های خاص و نسخه‌های ویژه (مثل مود یا اورجینال) در بالای صفحه اصلی."
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "شروع می‌کنیم!",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingItem(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = desc,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
