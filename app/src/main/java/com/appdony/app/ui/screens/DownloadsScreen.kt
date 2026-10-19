package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.DownloadItem
import java.io.File

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val vm: DownloadsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DownloadsViewModel(context.applicationContext) as T
            }
        }
    )
    val downloads by vm.downloads.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Text(
            text = "دانلودها",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("هنوز چیزی دانلود نکردی", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadCard(
                        item = item,
                        onDelete = { vm.removeDownload(item.id) },
                        onInstall = {
                            try {
                                val file = File(Uri.parse(item.localUri).path!!)
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // نمایش خطا در صورت خراب بودن فایل
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadCard(item: DownloadItem, onDelete: () -> Unit, onInstall: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(
                        when {
                            item.isCompleted() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            item.isFailed() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    item.isCompleted() -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    item.isFailed() -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                    else -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title.ifBlank { item.fileName ?: "دانلود" },
                    color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = when {
                        item.isCompleted() -> "✓ آماده نصب"
                        item.isFailed() -> "✗ خطا در دانلود"
                        else -> item.sizeText()
                    },
                    color = if (item.isCompleted()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Box(modifier = Modifier.size(34.dp).clip(CircleShape).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        if (item.isRunning() && item.totalBytes > 0) {
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { item.progress() },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.background
            )
        }

        // 👇 دکمه نصب فقط وقتی فایل دانلود شده
        if (item.isCompleted()) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onInstall,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.InstallMobile, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("نصب برنامه", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
