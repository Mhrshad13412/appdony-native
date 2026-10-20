package com.appdony.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
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
import com.appdony.app.ui.components.RatingBar
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
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
    var showLoginSheet by remember { mutableStateOf(false) }
    var userRating by remember { mutableIntStateOf(0) }

    if (showLoginSheet) {
        LoginSheet(
            onDismiss = { showLoginSheet = false },
            onLoginSuccess = { user ->
                scope.launch {
                    sessionManager.saveSession(user.id, user.username, user.displayName(), user.email, user.avatarUrl() ?: "", user.nonce)
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
                    isLoggedIn = isLoggedIn,
                    nonce = nonce,
                    isBlog = isBlog,
                    userRating = userRating,
                    onRatingChanged = { rating ->
                        if (isLoggedIn) userRating = rating else showLoginSheet = true
                    },
                    onSimilarClick = onSimilarClick,
                    onDownload = { url, title ->
                        scope.launch {
                            DownloadHelper.startDownload(context, url, DownloadHelper.buildFileName(title, url))
                        }
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

        // دکمه بازگشت
        Box(
            modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape)
                .background(Color(0xCC000000)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }

        // دکمه علاقه‌مندی
        Box(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clip(CircleShape)
                .background(Color(0xCC000000)).clickable { scope.launch { favoritesManager.toggle(postId) } },
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ۱. عکس بزرگ
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
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
                                listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                            )
                        )
                )
            }
        }

        // ۲. باکس تایتل (همونی که خالی شده بود)
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
                AsyncImage(
                    model = post.featuredImageUrl(),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.cleanTitle(),
                        color = AppdonyTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = post.primaryCategoryName(),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // 🚀 ۳. بخش بازی و برنامه (غیر وبلاگ)
        if (!isBlog) {
            // ستاره‌ها
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppdonyCardBg)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("به این برنامه امتیاز دهید", color = AppdonyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    RatingBar(rating = userRating, onRatingChanged = onRatingChanged)
                    if (userRating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ممنون از امتیاز شما! ⭐", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }

            // نسخه
            item {
                if (info.version != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppdonyCardBg)
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("نسخه", color = AppdonyTextMuted, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(info.version, color = AppdonyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // دکمه دانلود
            item {
                val url = info.downloadUrl
                if (!url.isNullOrBlank()) {
                    Button(
                        onClick = { onDownload(url, post.cleanTitle()) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("دانلود", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // اشتراک‌گذاری
            item {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("اشتراک‌گذاری")
                }
            }
        }

        // 📝 ۴. متن / توضیحات
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isBlog) "متن مقاله" else "توضیحات",
                color = AppdonyTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = info.description.ifBlank { "توضیحاتی ثبت نشده است." },
                color = AppdonyTextMuted,
                fontSize = 14.sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // مشابه‌ها
        if (!isBlog && similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "مشابه‌ها",
                    color = AppdonyTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(10.dp))
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(similar, key = { it.id }) { p ->
                        AppCard(
                            post = p,
                            onClick = { onSimilarClick(p.id) },
                            modifier = Modifier.width(160.dp)
                        )
                    }
                }
            }
        }

        // نظرات
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
