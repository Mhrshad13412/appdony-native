package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import java.io.File

@Composable
fun DownloadsScreen() {
    val context = LocalContext.current
    val vm: DownloadsViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DownloadsViewModel(context) as T
            }
        }
    )
    val downloads by vm.downloads.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("دانلودها", color = AppdonyTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هیچ فایلی دانلود نشده است.", color = AppdonyTextMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads) { item ->
                    DownloadCard(
                        item = item,
                        onDelete = { 
                            val file = File(item.filePath)
                            if (file.exists()) file.delete()
                            vm.loadDownloads() 
                        },
                        onInstall = {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            }
                        },
                        onShare = {
                            val file = File(item.filePath)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.android.package-archive"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCard(item: DownloadItem, onDelete: () -> Unit, onInstall: () -> Unit, onShare: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = AppdonyCardBg), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.title, color = AppdonyTextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text("حجم: ${item.size}", color = AppdonyTextMuted, fontSize = 12.sp)
            
            if (item.status == DownloadManager.STATUS_RUNNING) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { item.progress / 100f }, modifier = Modifier.fillMaxWidth())
            }
            
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                    Button(onClick = onInstall, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text("نصب")
                    }
                    OutlinedButton(onClick = onShare, shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                    }
                }
                OutlinedButton(onClick = onDelete, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}
