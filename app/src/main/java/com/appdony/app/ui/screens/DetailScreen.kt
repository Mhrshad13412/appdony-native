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
import com.appdony.app.util.ContentBlock
import com.appdony.app.util.DownloadHelper
import com.appdony.app.util.DownloadLink
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
                    onDownload = { link -> scope.launch { startAppDownload(context, link, s.post) } },
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

private suspend fun startAppDownload(context: Context, link: DownloadLink, post: Post) {
    Toast.makeText(context, "در حال آماده‌سازی لینک دانلود...", Toast.LENGTH_SHORT).show()
    DownloadHelper.startDownload(context, link.url, post.cleanTitle(), post.featuredImageUrl())
        .onSuccess {
            Toast.makeText(context, "دانلود شروع شد ✓ از بخش «دانلودها» دنبالش کن", Toast.LENGTH_LONG).show()
        }
        .onFailure {
            // لینک مستقیم فایل پیدا نشد؛ صفحه‌ی دانلود خود سایت رو توی مرورگر باز می‌کنیم
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link.url))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Toast.makeText(context, "صفحه‌ی دانلود توی مرورگر باز شد", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, it.message ?: "خطا در دریافت لینک", Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, "کپی شد", Toast.LENGTH_SHORT).show()
                }) { Text("کپی اطلاعات فنی", fontSize = 12.sp) }
            }
        } else {
            info.downloadLinks.forEachIndexed { index, link ->
                Button(
                    onClick = { onDownload(link) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (index == 0) primary else AppdonyCardBg)
                ) {
                    Icon(Icons.Default.Download, null, tint = if (index == 0) Color.White else AppdonyTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = link.title,
                        color = if (index == 0) Color.White else AppdonyTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (link.size != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(link.size, color = if (index == 0) Color.White.copy(alpha = 0.8f) else AppdonyTextMuted, fontSize = 12.sp)
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
    onDownload: (DownloadLink) -> Unit, onNeedLogin: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = AppdonyTextPrimary
    val tocItems = contentBlocks.filterIsInstance<ContentBlock.Header>().filter { it.level == 2 }
    val featured = post.featuredImageUrl()

    // برای بازی/برنامه عکس‌ها توی گالری نشون داده می‌شن نه توی متن
    val description = if (isBlog) contentBlocks else contentBlocks.filter { it !is ContentBlock.Image }
    val shots = remember(info, featured) {
        val featuredName = featured?.let { baseName(it) }
        info.screenshots.filter { baseName(it) != featuredName }
    }
    val tableRows = remember(info, post) {
        buildList {
            info.version?.let { add("نسخه" to it) }
            info.size?.let { add("حجم" to it) }
            info.androidReq?.let { add("اندروید مورد نیاز" to it) }
            info.developer?.let { add("سازنده" to it) }
            add("دسته‌بندی" to post.primaryCategoryName())
            post.date?.let { add("تاریخ انتشار" to it.substringBefore('T')) }
        }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().height(if (isBlog) 220.dp else 280.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            ) {
                AsyncImage(model = featured, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)))
                    )
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (isBlog) {
                    Text(post.primaryCategoryName(), color = primaryColor, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    Spacer(Modifier.height(8.dp))
                    Text(post.cleanTitle(), color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, lineHeight = 32.sp)
                    Spacer(Modifier.height(16.dp))
                } else {
                    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(AppdonyCardBg).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = featured, contentDescription = null,
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(post.cleanTitle(), color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(post.primaryCategoryName(), color = primaryColor, fontSize = 13.sp)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("امتیاز دهید: ", color = AppdonyTextMuted, fontSize = 13.sp)
                            Spacer(Modifier.width(8.dp))
                            RatingBar(rating = userRating, onRatingChanged = onRatingChanged)
                        }
                    }
                }
            }
        }

        if (isBlog && tocItems.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp)).border(1.dp, primaryColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .background(AppdonyCardBg).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.List, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("فهرست مطالب", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    tocItems.forEach { header ->
                        Text(
                            "• ${header.text}", color = textColor, fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth()
                                .clickable { scope.launch { listState.animateScrollToItem(contentBlocks.indexOf(header) + 3) } }
                                .padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (!isBlog) {
            // جدول مشخصات
            item { SectionTitle("مشخصات") }
            item { InfoTable(tableRows) }

            // دانلود
            item { SectionTitle("دانلود") }
            item { DownloadSection(info = info, onDownload = onDownload) }

            // تصاویر
            if (shots.isNotEmpty()) {
                item { SectionTitle("تصاویر") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(shots, key = { it }) { url ->
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.size(width = 260.dp, height = 180.dp).clip(RoundedCornerShape(14.dp)).background(AppdonyCardBg),
                                loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0) },
                                error = { Box(Modifier.fillMaxSize().background(AppdonyCardBg)) },
                                success = { SubcomposeAsyncImageContent() }
                            )
                        }
                    }
                }
            }

            if (description.isNotEmpty()) item { SectionTitle("توضیحات") }
        }

        itemsIndexed(description) { _, block ->
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                when (block) {
                    is ContentBlock.Header -> {
                        Column {
                            Spacer(Modifier.height(16.dp))
                            Text(block.text, color = textColor, fontSize = if (block.level == 2) 20.sp else 18.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    is ContentBlock.Paragraph -> {
                        Column {
                            Text(block.text, color = AppdonyTextMuted, fontSize = 15.sp, lineHeight = 28.sp)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    is ContentBlock.Image -> {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            SubcomposeAsyncImage(
                                model = block.url,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)),
                                loading = { ShimmerBox(modifier = Modifier.fillMaxWidth().height(180.dp), cornerRadius = 16) },
                                error = { Box(Modifier.fillMaxWidth().height(1.dp)) },
                                success = { SubcomposeAsyncImageContent() }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    is ContentBlock.Quote -> {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).background(primaryColor.copy(alpha = 0.1f))) {
                                Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(primaryColor))
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatQuote, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(block.text, color = textColor, fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 24.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    is ContentBlock.ListBlock -> {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            block.items.forEachIndexed { i, item ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Text(if (block.isOrdered) "${i + 1}." else "•", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    Text(item, color = AppdonyTextMuted, fontSize = 15.sp, lineHeight = 26.sp)
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        if (!isBlog && similar.isNotEmpty()) {
            item { SectionTitle("مشابه‌ها") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(similar, key = { it.id }) { p ->
                        AppCard(title = p.cleanTitle(), imageUrl = p.featuredImageUrl() ?: "", onClick = { onSimilarClick(p.id) })
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            CommentsSection(postId = post.id, isLoggedIn = isLoggedIn, nonce = nonce, onNeedLogin = onNeedLogin)
        }
    }
}
