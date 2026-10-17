package com.appdony.app.ui.screens

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.AppInfo
import com.appdony.app.util.DownloadHelper
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(
    postId: Int,
    onBack: () -> Unit,
    onSimilarClick: (Int) -> Unit
) {
    val vm: DetailViewModel = viewModel(
        key = "detail_$postId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return DetailViewModel(postId) as T
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

    val isFavorite = favorites.contains(postId)

    var showLoginSheet by remember { mutableStateOf(false) }

    if (showLoginSheet) {
        LoginSheet(
            onDismiss = { showLoginSheet = false },
            onLoginSuccess = { user ->
                scope.launch {
                    sessionManager.saveSession(
                        userId = user.id,
                        username = user.username,
                        displayName = user.displayName(),
                        email = user.email,
                        avatarUrl = user.avatarUrl() ?: "",
                        nonce = user.nonce
                    )
                    showLoginSheet = false
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is DetailUiState.Loading -> {
                DetailSkeleton()
            }
            is DetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = AppdonyTextMuted)
                }
            }
            is DetailUiState.Success -> {
                DetailContent(
                    post = s.post,
                    info = s.info,
                    similar = s.similar,
                    isLoggedIn = isLoggedIn,
                    nonce = nonce,
                    onSimilarClick = onSimilarClick,
                    onDownload = { url, title ->
                        val fileName = DownloadHelper.buildFileName(title, url)
                        DownloadHelper.startDownload(context, url, fileName)
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "دانلود ${s.post.cleanTitle()}\nhttps://appdony.ir/?p=${s.post.id}")
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                    },
                    onNeedLogin = { showLoginSheet = true }
                )
            }
        }

        Box(
            modifier = Modifier
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "بازگشت",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xCC000000))
                .clickable {
                    scope.launch { favoritesManager.toggle(postId) }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "علاقه‌مندی",
                tint = if (isFavorite) Color(0xFFFF5252) else Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun DetailContent(
    post: Post,
    info: AppInfo,
    similar: List<Post>,
    isLoggedIn: Boolean,
    nonce: String,
    onSimilarClick: (Int) -> Unit,
    onDownload: (String, String) -> Unit,
    onShare: () -> Unit,
    onNeedLogin: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            ) {
                AsyncImage(
                    model = post.featuredImageUrl(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                            )
                        )
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AppdonyCardBg)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    val img = post.featuredImageUrl()
                    if (!img.isNullOrBlank()) {
                        AsyncImage(
                            model = img,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Android, null, tint = AppdonyTextMuted, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.cleanTitle(),
                        color = AppdonyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = post.primaryCategoryName(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item {
            if (info.version != null || info.size != null || info.androidReq != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    info.version?.let { InfoChip(label = "نسخه", value = it) }
                    info.size?.let { InfoChip(label = "حجم", value = it) }
                    info.androidReq?.let { InfoChip(label = "اندروید", value = it.take(15)) }
                }
            }
        }

        if (info.downloadLinks.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    info.downloadLinks.forEachIndexed { index, link ->
                        Button(
                            onClick = { onDownload(link.url, post.cleanTitle()) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (index == 0) MaterialTheme.colorScheme.primary else AppdonyCardBg
                            )
                        ) {
                            Icon(Icons.Default.Download, null, tint = if (index == 0) Color.White else AppdonyTextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = link.title.ifBlank { "دانلود" },
                                color = if (index == 0) Color.White else AppdonyTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppdonyCardBg)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لینک دانلود سالمی در این صفحه پیدا نشد",
                        color = AppdonyTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (info.screenshots.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("تصاویر", color = AppdonyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(info.screenshots) { url ->
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(280.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AppdonyCardBg)
                        ) {
                            AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("توضیحات", color = AppdonyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(text = info.cleanDescription.take(3000), color = AppdonyTextMuted, fontSize = 13.sp, lineHeight = 22.sp)
            }
        }

        if (similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                Text("برنامه‌های مشابه", color = AppdonyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(similar, key = { it.id }) { p ->
                        AppCard(title = p.cleanTitle(), imageUrl = p.featuredImageUrl(), onClick = { onSimilarClick(p.id) })
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(24.dp))
            CommentsSection(
                postId = post.id,
                isLoggedIn = isLoggedIn,
                nonce = nonce,
                onNeedLogin = onNeedLogin
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AppdonyCardBg)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(text = label, color = AppdonyTextMuted, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(text = value, color = AppdonyTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
