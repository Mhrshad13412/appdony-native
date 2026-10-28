package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.DownloadHelper
import java.io.File
import java.util.Locale

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val vm: DownloadsViewModel = viewModel()
    val items by vm.items.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            text = "دانلودها",
            color = AppdonyTextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز چیزی دانلود نکردی.", color = AppdonyTextMuted)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onOpen = { openDownload(context, item) },
                        onDelete = { vm.delete(item) },
                        onRetry = {
                            Toast.makeText(context, "در حال آماده‌سازی لینک...", Toast.LENGTH_SHORT).show()
                            vm.retry(item) { result ->
                                result
                                    .onSuccess { Toast.makeText(context, "دانلود دوباره شروع شد", Toast.LENGTH_SHORT).show() }
                                    .onFailure {
                                        Toast.makeText(context, it.message ?: "خطا در دریافت لینک", Toast.LENGTH_LONG).show()
                                    }
                            }
                        }
                    )
                }
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
    val running = !done && !failed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppdonyCardBg)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = statusText(item),
                    color = if (failed) MaterialTheme.colorScheme.error else AppdonyTextMuted,
                    fontSize = 12.sp
                )
            }
        }

        if (running) {
            Spacer(Modifier.height(10.dp))
            if (item.total > 0) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (done) {
                val isApk = item.fileName.lowercase().endsWith(".apk")
                Button(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                    Text(if (isApk) "نصب" else "باز کردن")
                }
            }
            if (failed) {
                OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تلاش دوباره")
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AppdonyTextMuted)
            }
        }
    }
}

private fun statusText(item: DownloadItem): String = when (item.status) {
    DownloadManager.STATUS_SUCCESSFUL -> "دانلود کامل شد • ${formatSize(item.total)}"
    DownloadManager.STATUS_FAILED -> item.message ?: "دانلود ناموفق بود"
    DownloadManager.STATUS_PENDING -> "در صف دانلود..."
    DownloadManager.STATUS_PAUSED -> item.message ?: "متوقف شد"
    else -> if (item.total > 0) {
        "${item.progress}% • ${formatSize(item.bytes)} از ${formatSize(item.total)}"
    } else {
        "در حال دانلود..."
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
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var uri: Uri? = try {
            manager.getUriForDownloadedFile(item.id)
        } catch (e: Exception) {
            null
        }

        if (uri == null && !item.localUri.isNullOrBlank()) {
            val parsed = Uri.parse(item.localUri)
            uri = if (parsed.scheme == "file") {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(parsed.path ?: ""))
            } else {
                parsed
            }
        }

        if (uri == null) {
            Toast.makeText(context, "فایل پیدا نشد. شاید حذف شده باشد.", Toast.LENGTH_LONG).show()
            return
        }

        val ext = item.fileName.substringAfterLast('.', "").lowercase()
        val mime = if (ext == "apk") DownloadHelper.APK_MIME
        else MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"

        if (ext == "apk" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            Toast.makeText(context, "اجازه‌ی نصب برنامه را برای اپدونی فعال کن و دوباره بزن", Toast.LENGTH_LONG).show()
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
        Toast.makeText(context, "این فایل را با برنامه‌ی مدیریت فایل باز کن (پوشه‌ی Download/Appdony)", Toast.LENGTH_LONG).show()
    }
}
