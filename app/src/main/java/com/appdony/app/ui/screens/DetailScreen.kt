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
import com.appdony.app.util.AppInfo
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
        // تصویر بزرگ بالای صفحه (مشترک)
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

        // عنوان و اطلاعات (مشترک)
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
                // ... (اینجا کد اصلی خودت برای نمایش عنوان و آیکون هست، دست نخورده)
            }
        }

        // 🚀 بخش مخصوص بازی و برنامه (غیر وبلاگ)
        if (!isBlog) {
            // سیستم امتیازدهی ستاره‌ای
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

            // اطلاعات نسخه، حجم و اندروید
            item {
                if (info.version != null || info.size != null || info.androidReq != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        info.version?.let { InfoChip("نسخه", it) }
                        info.size?.let { InfoChip("حجم", it) }
                        info.androidReq?.let { InfoChip("اندروید", it.take(10)) }
                    }
                }
            }

            // دکمه‌های دانلود
            item {
                if (info.downloadLinks.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        info.downloadLinks.forEachIndexed { index, link ->
                            Button(
                                onClick = { onDownload(link.url, post.cleanTitle()) },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (index == 0) MaterialTheme.colorScheme.primary else AppdonyCardBg
                                )
                            ) {
                                Icon(Icons.Default.Download, null, tint = if (index == 0) Color.White else AppdonyTextPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    link.title.ifBlank { "دانلود" },
                                    color = if (index == 0) Color.White else AppdonyTextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // دکمه اشتراک‌گذاری (فقط برای بازی و برنامه)
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

        // 📝 متن مقاله / توضیحات (مشترک برای هر دو حالت)
        item {
            Spacer(Modifier.height(24.dp))
            Text(
                text = if (isBlog) "متن مقاله" else "توضیحات",
                color = AppdonyTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = info.cleanDescription.take(5000),
                color = AppdonyTextMuted,
                fontSize = 14.sp,
                lineHeight = 28.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // بخش مشابه‌ها (فقط برای بازی و برنامه)
        if (!isBlog && similar.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "برنامه‌های مشابه",
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
                            title = p.cleanTitle(),
                            imageUrl = p.featuredImageUrl(),
                            onClick = { onSimilarClick(p.id) }
                        )
                    }
                }
            }
        }

        // نظرات (مشترک برای هر دو حالت)
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(label, color = AppdonyTextMuted, fontSize = 12.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = AppdonyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
