package com.appdony.app.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
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
import com.appdony.app.data.model.UserData
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.model.primaryCategoryName
import com.appdony.app.data.preferences.FavoritesManager
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.CommentsSection
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
            onLoginSuccess = { user: UserData ->
                scope.launch {
                    sessionManager.saveSession(
                        user.id, user.username, user.name,
                        user.email, user.avatarUrl() ?: "", user.nonce
                    )
                    showLoginSheet = false
                }
            }
        )
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
            is DetailUiState.Loading -> if (isBlog) BlogDetailSkeleton() else DetailSkeleton()
            is DetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(s.message, color = AppdonyTextMuted)
            }
            is DetailUiState.Success -> {
                DetailContent(
                    post = s.post, info = s.info, similar = s.similar, contentBlocks = s.contentBlocks,
                    isLoggedIn = isLoggedIn, nonce = nonce, isBlog = isBlog, userRating = userRating,
                    onRatingChanged = { rating -> if (isLoggedIn) userRating = rating else showLoginSheet = true },
                    onSimilarClick = onSimilarClick,
                    onDownload = { url, title -> scope.launch { DownloadHelper.startDownload(context, url, DownloadHelper.buildFileName(title, url)) } },
                    onNeedLogin = { showLoginSheet = true }
                )
            }
        }

        Box(
            modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape)
                .background(Color(0xCC000000)).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { shareAction() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = Color.White)
            }
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable {
                    scope.launch { favoritesManager.toggle(postId) }
                },
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
}

// بقیه کدهای DetailContent، DetailSkeleton و BlogDetailSkeleton بدون تغییر باقی می‌مانند (همان‌طور که قبلاً ارسال شد).
// فقط ایمپورت `Icons.Default.Download` و `UserData` به بالای فایل اضافه شده است.
