package com.appdony.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.UserData
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.model.primaryCategoryName
import com.appdony.app.data.preferences.FavoritesManager
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.BlogCardSkeleton
import com.appdony.app.ui.components.CommentsSection
import com.appdony.app.ui.components.DetailSkeleton
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.RatingBar
import com.appdony.app.ui.components.ShimmerBox
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.AppInfo
import com.appdony.app.util.AssistantBus
import com.appdony.app.util.ContentBlock
import com.appdony.app.util.DownloadHelper
import com.appdony.app.util.DownloadPhase
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.util.DownloadLink
import com.appdony.app.util.TelegramLinks
import com.appdony.app.util.TelegramRedirect
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(postId: Int, isBlog: Boolean, onBack: () -> Unit, onSimilarClick: (Int) -> Unit) {
    val vm: DetailViewModel = viewModel(
        key = "detail_${postId}_$isBlog",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DetailViewModel(postId, isBlog) as T
            }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val favoritesManager = remember { FavoritesManager(context) }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nonce by sessionManager.nonce.collectAsStateWithLifecycle(initialValue = "")
    val favorites by favoritesManager.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    var showLoginSheet by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }

    if (showLoginSheet) {
        LoginSheet(onDismiss = { showLoginSheet = false }, onLoginSuccess = { user: UserData ->
            scope.launch {
                sessionManager.saveSession(user.id, user.username, user.name, user.email, user.avatarUrl() ?: "", user.nonce)
                showLoginSheet = false
            }
        })
    }

    val shareAction = {
        if (state is DetailUiState.Success) {
            val post = (state as DetailUiState.Success).post
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "مشاهده در اپدونی\nhttps://appdony.ir/?p=${post.id}")
            }
            context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is DetailUiState.Loading -> if (isBlog) BlogCardSkeleton() else DetailSkeleton()
            is DetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = AppdonyTextMuted)
            }
            is DetailUiState.Success -> {
                DetailContent(
                    post = s.post, info = s.info, similar = s.similar, contentBlocks = s.contentBlocks,
                    isLoggedIn = isLoggedIn, nonce = nonce, isBlog = isBlog, userRating = userRating,
                    onRatingChanged = { if (isLoggedIn) userRating = it else showLoginSheet = true },
                    onSimilarClick = onSimilarClick,
                    onDownload = { link ->
                        val tgFallback = s.info.downloadLinks.firstOrNull { TelegramLinks.isTelegram(it.url) }?.url
                        scope.launch { startAppDownload(context, link, s.post, tgFallback) }
                    },
                    onNeedLogin = { showLoginSheet = true }
                )
            }
        }

        Box(
            modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = Color.White)
        }
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { shareAction() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Share, "اشتراک‌گذاری", tint = Color.White) }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xCC000000))
                    .clickable { scope.launch { favoritesManager.toggle(postId) } },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (favorites.contains(postId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    "علاقه‌مندی",
                    tint = if (favorites.contains(postId)) Color(0xFFFF5252) else Color.White
                )
            }
        }
    }
}

private suspend fun startAppDownload(context: Context, link: DownloadLink, post: Post, tgFallback: String? = null) {
    if (link.url.isBlank()) {
        AppBanner.show(BannerKind.WARNING, "این لینک فعلاً در دسترس نیست")
        return
    }
    // لینک تلگرام: مستقیم با اپ تلگرام باز می‌شه، نه با وب و نه وارد صف دانلود
    if (TelegramLinks.isTelegram(link.url)) {
        if (TelegramLinks.open(context, link.url)) {
            AppBanner.show(BannerKind.SUCCESS, "تلگرام باز شد؛ فایل رو از همون‌جا بگیر", 3500L)
        } else {
            AppBanner.show(BannerKind.WARNING, "نتونستم تلگرام رو باز کنم", 4000L)
        }
        return
    }
    AppBanner.show(BannerKind.CHECKING, "در حال بررسی لینک...", 15000L)
    AssistantBus.downloadPhase(DownloadPhase.CHECKING)
    DownloadHelper.startDownload(context, link.url, post.cleanTitle(), post.featuredImageUrl())
        .onSuccess {
            AssistantBus.downloadPhase(DownloadPhase.READY)
            AppBanner.show(BannerKind.SUCCESS, "فایل پیدا شد و دانلود شروع شد؛ از بخش «دانلودها» دنبالش کن", 4000L)
        }
        .onFailure { e ->
            if (e is TelegramRedirect) {
                // لینکی که اسمش «دانلود مستقیم» بود ولی آخرش به تلگرام می‌رسه
                AssistantBus.downloadPhase(DownloadPhase.READY)
                if (TelegramLinks.open(context, e.url)) {
                    AppBanner.show(BannerKind.SUCCESS, "تلگرام باز شد؛ فایل رو از همون‌جا بگیر", 3500L)
                } else {
                    AppBanner.show(BannerKind.WARNING, "نتونستم تلگرام رو باز کنم", 4000L)
                }
            } else if (tgFallback != null && tgFallback != link.url) {
                // لینک مستقیم خراب بود؛ لینک تلگرام همین پست رو باز می‌کنیم (مثل دکمه‌ی اول)
                AssistantBus.downloadPhase(DownloadPhase.READY)
                if (TelegramLinks.open(context, tgFallback)) {
                    AppBanner.show(BannerKind.SUCCESS, "لینک مستقیم خراب بود؛ تلگرام باز شد، فایل رو از همون‌جا بگیر", 4000L)
                } else {
                    AssistantBus.downloadPhase(DownloadPhase.FAILED)
                    AppBanner.show(BannerKind.WARNING, "نتونستم تلگرام رو باز کنم", 4000L)
                }
            } else {
                AssistantBus.downloadPhase(DownloadPhase.FAILED)
                // لینک خراب وارد صف دانلود نمی‌شه؛ فقط دلیلش رو نشون می‌دیم
                AppBanner.show(BannerKind.ERROR, e.message ?: "این لینک کار نمی‌کنه", 4500L)
            }
        }
}

private fun baseName(url: String): String =
    url.substringBefore('?').substringAfterLast('/').replace(Regex("-\\d+x\\d+(?=\\.)"), "").lowercase()

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = AppdonyTextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun InfoTable(rows: List<Pair<String, String>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppdonyCardBg)
    ) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = AppdonyTextMuted, fontSize = 13.sp, modifier = Modifier.weight(0.4f))
                Text(
                    text = value,
                    color = AppdonyTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.Start
                )
            }
            if (index != rows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DownloadSection(info: AppInfo, onDownload: (DownloadLink) -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (info.downloadLinks.isEmpty() && !info.linksChecked) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("در حال بررسی لینک نصب...", color = AppdonyTextMuted, fontSize = 13.sp)
            }
        } else if (info.downloadLinks.isEmpty()) {
            val failed = info.linkError != null
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (failed) "خطا در اتصال" else "لینک نصب موجود نیست",
                        color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    info.linkError ?: "سایت برای این مورد لینک نصب نذاشته.",
                    color = AppdonyTextMuted, fontSize = 13.sp
                )
                TextButton(onClick = {
                    val tech = info.linkDebug.ifBlank {
                        info.allLinks.joinToString("\n").ifBlank { "اطلاعاتی نبود" }
                    }
                    clipboard.setText(AnnotatedString(tech))
                    AppBanner.show(BannerKind.SUCCESS, "اطلاعات فنی کپی شد")
                }) { Text("کپی اطلاعات فنی", fontSize = 12.sp) }
            }
        } else {
            info.downloadLinks.forEachIndexed { index, link ->
                val locked = link.url.isBlank()
                val isPrimary = index == 0 && !locked
                Button(
                    onClick = { onDownload(link) },
                    enabled = !locked,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPrimary) primary else AppdonyCardBg,
                        disabledContainerColor = AppdonyCardBg.copy(alpha = 0.6f)
                    )
                ) {
                    Icon(
                        if (locked) Icons.Default.Lock else Icons.Default.Download, null,
                        tint = when {
                            locked -> AppdonyTextMuted
                            isPrimary -> Color.White
                            else -> AppdonyTextPrimary
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = link.title,
                        color = when {
                            locked -> AppdonyTextMuted
                            isPrimary -> Color.White
                            else -> AppdonyTextPrimary
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (link.size != null && !locked) {
                        Spacer(Modifier.width(8.dp))
                        Text(link.size, color = if (isPrimary) Color.White.copy(alpha = 0.8f) else AppdonyTextMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    post: Post, info: AppInfo, similar: List<Post>, contentBlocks: List<ContentBlock>,
    isLoggedIn: Boolean, nonce: String, isBlog: Boolean, userRating: Int,
    onRatingChanged: (Int) -> Unit, onSimilarClick: (Int) -> Unit,
    on
