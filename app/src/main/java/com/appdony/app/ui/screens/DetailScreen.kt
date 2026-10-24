package com.appdony.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.model.primaryCategoryName
import com.appdony.app.data.preferences.FavoritesManager
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.CommentsSection
import com.appdony.app.ui.components.DetailSkeleton
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.RatingBar
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.AppInfo
import com.appdony.app.util.ContentBlock
import com.appdony.app.util.DownloadHelper
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    postId: Int,
    isBlog: Boolean,
    onBack: () -> Unit,
    onSimilarClick: (Int) -> Unit
) {
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
        LoginSheet(
            onDismiss = { showLoginSheet = false },
            onLoginSuccess = { user ->
                scope.launch {
                    sessionManager.saveSession(user.id, user.username, user.name, user.email, user.avatarUrl() ?: "", user.nonce)
                    showLoginSheet = false
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is DetailUiState.Loading -> DetailSkeleton()
            is DetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = AppdonyTextMuted)
            }
            is DetailUiState.Success -> {
                DetailContent(
                    post = s.post,
                    info = s.info,
                    similar = s.similar,
                    contentBlocks = s.contentBlocks,
                    isLoggedIn = isLoggedIn,
                    nonce = nonce,
                    isBlog = isBlog,
                    userRating = userRating,
                    onRatingChanged = { rating -> if (isLoggedIn) userRating = rating else showLoginSheet = true },
                    onSimilarClick = onSimilarClick,
                    onDownload = { url, title ->
                        scope.launch { DownloadHelper.startDownload(context, url, DownloadHelper.buildFileName(title, url)) }
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "مشاهده در اپدونی\nhttps://appdony.ir/?p=${s.post.id}")
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                    },
                    onNeedLogin = { showLoginSheet = true }
                )
            }
        }

        Box(
            modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }

        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { scope.launch { favoritesManager.toggle(postId) } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (favorites.contains(postId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "علاقه‌مندی",
                tint = if (favorites.contains(postId)) Color(0xFFFF5252) else Color.White
            )
        }
    }
}

@Composable
private fun DetailContent(
    post: Post,
    info: AppInfo,
    similar: List<Post>,
    contentBlocks: List<ContentBlock>,
    isLoggedIn: Boolean,
    nonce: String,
    isBlog: Boolean,
    userRating: Int,
    onRatingChanged: (Int) -> Unit,
    onSimilarClick: (Int) -> Unit,
    onDownload: (String, String) -> Unit,
    onShare: () -> Unit,
    onNeedLogin: () -> Unit
) {
    val listState = rememberLazyListState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val textColor = AppdonyTextPrimary

    val tocItems = contentBlocks.filterIsInstance<ContentBlock.Header>().filter { it.level == 2 }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(if (isBlog) 220.dp else 280.dp).clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
                AsyncImage(model = post.featuredImageUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)))))
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (isBlog) {
                    Text(post.primaryCategoryName(), color = primaryColor, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(post.cleanTitle(), color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, lineHeight = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(AppdonyCardBg).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(model = post.featuredImageUrl(), contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(post.cleanTitle(), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(post.primaryCategoryName(), color = primaryColor, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        if (isBlog && tocItems.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(AppdonyCardBg).padding(16.dp)) {
                    Text("فهرست مطالب", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    tocItems.forEach { header ->
                        Text(
                            text = "• ${header.text}",
                            color = textColor,
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (!isBlog) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(AppdonyCardBg).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("به این برنامه امتیاز دهید", color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingBar(rating = userRating, onRatingChanged = onRatingChanged)
                }
            }
            item {
                if (info.downloadLinks.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        info.downloadLinks.forEachIndexed { index, link ->
                            Button(onClick = { onDownload(link.url, post.cleanTitle()) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (index == 0) primaryColor else AppdonyCardBg)) {
                                Icon(Icons.Default.Download, null, tint = if (index == 0) Color.White else textColor)
                                Spacer(Modifier.width(8.dp))
                                Text(link.title.ifBlank { "دانلود" }, color = if (index == 0) Color.White else textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(24.dp)).background(AppdonyCardBg).padding(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    contentBlocks.forEach { block ->
                        when (block) {
                            is ContentBlock.Header -> {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(block.text, color = textColor, fontSize = if (block.level == 2) 20.sp else 18.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            is ContentBlock.Paragraph -> {
                                Text(block.text, color = AppdonyTextMuted, fontSize = 15.sp, lineHeight = 28.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            is ContentBlock.Image -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                AsyncImage(model = block.url, contentDescription = null, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.FillWidth)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            is ContentBlock.Quote -> {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(8.dp)).background(primaryColor.copy(alpha = 0.1f))) {
                                    Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(primaryColor))
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.FormatQuote, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(block.text, color = textColor, fontSize = 14.sp, fontStyle = FontStyle.Italic, lineHeight = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            is ContentBlock.ListBlock -> {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    block.items.forEachIndexed { index, item ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Text(if (block.isOrdered) "${index + 1}." else "•", color = primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(item, color = AppdonyTextMuted, fontSize = 15.sp, lineHeight = 26.sp)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اشتراک‌گذاری")
            }
        }

        if (!isBlog && similar.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text("مشابه‌ها", color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(modifier = Modifier.height(10.dp))
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(similar, key = { it.id }) { p ->
                        AppCard(title = p.cleanTitle(), imageUrl = p.featuredImageUrl() ?: "", onClick = { onSimilarClick(p.id) }, modifier = Modifier.width(160.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            CommentsSection(postId = post.id, isLoggedIn = isLoggedIn, nonce = nonce, onNeedLogin = onNeedLogin)
        }
    }
}
