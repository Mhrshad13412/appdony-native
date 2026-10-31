package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.ui.components.DownloadCardSkeleton
import com.appdony.app.ui.components.LocalBottomBarClearance
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.DownloadHelper
import java.io.File
import java.util.Locale

private val StatusGreen = Color(0xFF34C77B)
private val StatusYellow = Color(0xFFFFC53D)

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val vm: DownloadsViewModel = viewModel()
    val items by vm.items.collectAsStateWithLifecycle()
    val loaded by vm.loaded.collectAsStateWithLifecycle()
    // پایین لیست جا می‌ذاریم تا آخرین کارت زیر منوی شیشه‌ای نمونه
    val bottomClearance = LocalBottomBarClearance.current

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "دانلودها",
            color = AppdonyTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        when {
            !loaded -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { DownloadCardSkeleton() }
            }
            items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز چیزی دانلود نکردی.", color = AppdonyTextMuted)
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp + bottomClearance)
            ) {
                items(items, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onOpen = { openDownload(context, item) },
                        onDelete = { vm.delete(item) },
                        onRetry = {
                            AppBanner.show(BannerKind.CHECKING, "در حال آماده‌سازی دانلود...", 8000L)
                            vm.retry(item) { result ->
                                result
                                    .onSuccess { AppBanner.show(BannerKind.SUCCESS, "دانلود از همون‌جا که مونده بود ادامه پیدا کرد") }
                                    .onFailure { AppBanner.show(BannerKind.ERROR, it.message ?: "این لینک کار نمی‌کنه", 4500L) }
                            }
                        }
                    )
                }
            }
        }
    }
}

// دکمه‌ی قرصی هم‌تم با اندیکاتور منوی پایین
@Composable
private fun PillButton(text: String, icon: ImageVector?, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.5.dp, color, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// آیکون برنامه با حلقه‌ی پیشرفت دورش (مثل گوگل‌پلی)
@Composable
private fun IconWithRing(item: DownloadItem, done: Boolean, failed: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(66.dp), contentAlignment = Alignment.Center) {
        when {
            done -> CircularProgressIndicator(
                progress = { 1f }, modifier = Modifier.fillMaxSize(),
                color = StatusGreen, trackColor = StatusGreen.copy(alpha = 0.18f), strokeWidth = 3.dp
            )
            failed -> CircularProgressIndicator(
                progress = { if (item.total > 0) item.progress / 100f else 0f },
                modifier = Modifier.fillMaxSize(),
                color = StatusYellow, trackColor = StatusYellow.copy(alpha = 0.22f), strokeWidth = 3.dp
            )
            item.total > 0 && item.status == DownloadManager.STATUS_RUNNING -> CircularProgressIndicator(
                progress = { item.progress / 100f }, modifier = Modifier.fillMaxSize(),
                color = primary, trackColor = primary.copy(alpha = 0.18f), strokeWidth = 3.dp
            )
            else -> CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(), color = primary,
                trackColor = primary.copy(alpha = 0.18f), strokeWidth = 3.dp
            )
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!item.iconUrl.isNullOrBlank()) {
                AsyncImage(
                    model = item.iconUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit
) {
    val done = item.status == DownloadManager.STATUS_SUCCESSFUL
    val failed = item.status == DownloadManager.STATUS_FAILED
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = when {
        done -> StatusGreen
        failed -> StatusYellow
        else -> AppdonyTextMuted
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconWithRing(item, done, failed)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        color = AppdonyTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (done || failed) {
                            Icon(
                                if (done) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null, tint = statusColor, modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                        }
                        Text(
                            text = statusText(item),
                            color = statusColor,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (done) {
                    val isApk = item.fileName.lowercase().endsWith(".apk")
                    PillButton(if (isApk) "نصب" else "باز کردن", null, primary, onOpen)
                }
                if (failed) {
                    PillButton("تلاش دوباره", Icons.Default.Refresh, StatusYellow, onRetry)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AppdonyTextMuted)
                }
            }
        }
    }
}

// اعداد داخل ایزوله‌ی چپ‌به‌راست تا توی متن فارسی جابه‌جا نشن
private fun ltr(s: String) = "\u2066$s\u2069"

private fun statusText(item: DownloadItem): String = when (item.status) {
    DownloadManager.STATUS_SUCCESSFUL -> "دانلود کامل شد • ${ltr(formatSize(item.total))}"
    DownloadManager.STATUS_FAILED -> item.message ?: "دانلود ناموفق بود"
    DownloadManager.STATUS_PENDING -> item.message ?: "در صف دانلود..."
    DownloadManager.STATUS_PAUSED -> item.message ?: "متوقف شد"
    else -> if (item.total > 0) {
        "${ltr("${item.progress}%")} • ${ltr(formatSize(item.bytes))} از ${ltr(formatSize(item.total))}"
    } else {
        "در حال دانلود... ${ltr(formatSize(item.bytes))}"
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) String.format(Locale.US, "%.1f MB", mb)
    else String.format(Locale.US, "%.0f KB", bytes / 1024.0)
}

private fun openDownload(context: Context, item: DownloadItem) {
    try {
        var uri: Uri? = null
        if (!item.localUri.isNullOrBlank()) {
            val parsed = Uri.parse(item.localUri)
            uri = if (parsed.scheme == "file") {
                val f = File(parsed.path ?: "")
                if (f.exists()) FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f) else null
            } else {
                parsed
            }
        }

        if (uri == null) {
            AppBanner.show(BannerKind.WARNING, "فایل پیدا نشد؛ شاید حذف شده باشه", 4000L)
            return
        }

        val ext = item.fileName.substringAfterLast('.', "").lowercase()
        val mime = if (ext == "apk") DownloadHelper.APK_MIME
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        if (ext == "apk" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            AppBanner.show(BannerKind.WARNING, "اجازه‌ی نصب برنامه رو برای اپدونی فعال کن و دوباره بزن", 4500L)
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }

        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        AppBanner.show(BannerKind.WARNING, "فایل رو با برنامه‌ی مدیریت فایل باز کن (Android/data/com.appdony.app/files/Download/Appdony)", 5000L)
    }
}
