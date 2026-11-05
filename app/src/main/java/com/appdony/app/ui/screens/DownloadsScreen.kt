package com.appdony.app.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.appdony.app.data.model.gameCardName
import com.appdony.app.ui.components.ShimmerBox
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.ui.components.DownloadCardSkeleton
import com.appdony.app.ui.components.glassBorder
import com.appdony.app.ui.components.glassStroke
import com.appdony.app.ui.components.LocalBottomBarClearance
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.DownloadHelper
import java.io.File
import java.util.Locale
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

private val StatusGreen = Color(0xFF34C77B)
private val StatusYellow = Color(0xFFFFC53D)

// ⚠️ مهم — لود اولویت‌دار صفحه‌ی دانلودها؛ تحت هیچ عنوان حذف یا ساده نشه ⚠️
// ۱) اسکلت کارت‌ها تا لیست از حافظه‌ی گوشی برسه (بدون هیچ تأخیر ساختگی)
// ۲) قاب و اسم تمیز و وضعیت هر دانلود همون لحظه (اسم با فیلتر gameCardName؛ همون فیلتر کارت‌ها)
// ۳) آیکون آخر از همه و کم‌کیفیت (size 160 + RGB565) با لودینگ خودش
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
                        onPause = { vm.pause(item) },
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
            .glassBorder(1.5.dp, color, shape)
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

// نوار پیشرفتِ مربعیِ گرد‌گوشه دور آیکون (progress == null یعنی نامشخص)
@Composable
private fun SquircleProgress(progress: Float?, color: Color, track: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sq")
    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin"
    )
    Canvas(modifier = modifier) {
        val stroke = 3.dp.toPx()
        val half = stroke / 2f
        val rect = RoundRect(
            left = half, top = half, right = size.width - half, bottom = size.height - half,
            cornerRadius = CornerRadius(18.dp.toPx())
        )
        val path = Path().apply { addRoundRect(rect) }
        val measure = PathMeasure().apply { setPath(path, false) }
        val len = measure.length
        val style = Stroke(width = stroke, cap = StrokeCap.Round)
        drawPath(path, track, style = style)
        val seg = Path()
        if (progress != null) {
            val p = progress.coerceIn(0f, 1f)
            if (p > 0f) {
                measure.getSegment(0f, len * p, seg, true)
                drawPath(seg, color, style = style)
            }
        } else {
            val from = len * spin
            val to = from + len * 0.28f
            if (to <= len) {
                measure.getSegment(from, to, seg, true)
            } else {
                measure.getSegment(from, len, seg, true)
                measure.getSegment(0f, to - len, seg, true)
            }
            drawPath(seg, color, style = style)
        }
    }
}

// آیکون برنامه با نوار پیشرفت مربعیِ گرد‌گوشه دورش
@Composable
private fun IconWithRing(item: DownloadItem, done: Boolean, failed: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Box(modifier = Modifier.size(62.dp), contentAlignment = Alignment.Center) {
        when {
            done -> SquircleProgress(1f, StatusGreen, StatusGreen.copy(alpha = 0.18f), Modifier.fillMaxSize())
            failed -> SquircleProgress(
                if (item.total > 0) item.progress / 100f else 0f,
                StatusYellow, StatusYellow.copy(alpha = 0.22f), Modifier.fillMaxSize()
            )
            item.total > 0 && item.status == DownloadManager.STATUS_RUNNING -> SquircleProgress(
                item.progress / 100f, primary, primary.copy(alpha = 0.18f), Modifier.fillMaxSize()
            )
            else -> SquircleProgress(null, primary, primary.copy(alpha = 0.18f), Modifier.fillMaxSize())
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (!item.iconUrl.isNullOrBlank()) {
                val context = LocalContext.current
                val request = remember(item.iconUrl) {
                    ImageRequest.Builder(context).data(item.iconUrl).size(160).allowRgb565(true).crossfade(200).build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0) },
                    error = { },
                    success = { SubcomposeAsyncImageContent() }
                )
            } else {
                ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0)
            }
        }
    }
}

@Composable
private fun DownloadCard(
    item: DownloadItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onPause: () -> Unit,
    onRetry: () -> Unit
) {
    val done = item.status == DownloadManager.STATUS_SUCCESSFUL
    val failed = item.status == DownloadManager.STATUS_FAILED
    val paused = item.status == DownloadManager.STATUS_PAUSED
    val primary = MaterialTheme.colorScheme.primary
    val statusColor = when {
        done -> StatusGreen
        failed -> StatusYellow
        else -> AppdonyTextMuted
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = glassStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconWithRing(item, done, failed || paused)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // اسم تمیز: «دانلود روبیکا نسخه 4.1.1 برای اندروید» → «روبیکا»
                val shownTitle = remember(item.title) { item.title.gameCardName() }
                Text(
                    text = shownTitle,
                    color = AppdonyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
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
                Spacer(Modifier.height(8.dp))
                when {
                    done -> {
                        val isApk = item.fileName.lowercase().endsWith(".apk")
                        PillButton(if (isApk) "نصب" else "باز کردن", null, primary, onOpen)
                    }
                    failed -> PillButton("تلاش دوباره", Icons.Default.Refresh, StatusYellow, onRetry)
                    paused -> PillButton("ادامه", Icons.Default.PlayArrow, primary, onRetry)
                    else -> PillButton("توقف", Icons.Default.Pause, AppdonyTextMuted, onPause)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = AppdonyTextMuted)
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
    DownloadManager.STATUS_PAUSED -> {
        val sizes = if (item.total > 0) " • ${ltr("${item.progress}%")} از ${ltr(formatSize(item.total))}" else ""
        (item.message ?: "متوقف شد") + sizes
    }
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
