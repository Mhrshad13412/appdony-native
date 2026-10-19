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
import androidx.compose.material.icons.filled.Android
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
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import com.appdony.app.util.AppInfo
import com.appdony.app.util.DownloadHelper
import kotlinx.coroutines.launch

@Composable
fun DetailScreen(postId: Int, onBack: () -> Unit, onSimilarClick: (Int) -> Unit) {
    val vm: DetailViewModel = viewModel(key = "detail_$postId", factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(postId) as T
        }
    })
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val favoritesManager = remember { FavoritesManager(context) }
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val nonce by sessionManager.nonce.collectAsStateWithLifecycle(initialValue = "")
    val favorites by favoritesManager.favoriteIds.collectAsStateWithLifecycle(initialValue = emptySet())
    var showLoginSheet by remember { mutableStateOf(false) }

    if (showLoginSheet) {
        LoginSheet(onDismiss = { showLoginSheet = false }, onLoginSuccess = { user ->
            scope.launch {
                sessionManager.saveSession(user.id, user.username, user.displayName(), user.email, user.avatarUrl() ?: "", user.nonce)
                showLoginSheet = false
            }
        })
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            is DetailUiState.Loading -> DetailSkeleton()
            is DetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = AppdonyTextMuted) }
            is DetailUiState.Success -> {
                DetailContent(
                    post = s.post, info = s.info, similar = s.similar, isLoggedIn = isLoggedIn, nonce = nonce,
                    onSimilarClick = onSimilarClick,
                    onDownload = { url, title -> 
                        // 👇 فراخوانی تابع دانلود با scope
                        scope.launch { DownloadHelper.startDownload(context, url, DownloadHelper.buildFileName(title, url)) }
                    },
                    onShare = {
                        val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "دانلود ${s.post.cleanTitle()}\nhttps://appdony.ir/?p=${s.post.id}") }
                        context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                    },
                    onNeedLogin = { showLoginSheet = true }
                )
            }
        }
        Box(modifier = Modifier.padding(12.dp).size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { onBack() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = Color.White)
        }
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(40.dp).clip(CircleShape).background(Color(0xCC000000)).clickable { scope.launch { favoritesManager.toggle(postId) } }, contentAlignment = Alignment.Center) {
            Icon(if (favorites.contains(postId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "علاقه‌مندی", tint = if (favorites.contains(postId)) Color(0xFFFF5252) else Color.White)
        }
    }
}

@Composable
private fun DetailContent(post: Post, info: AppInfo, similar: List<Post>, isLoggedIn: Boolean, nonce: String, onSimilarClick: (Int) -> Unit, onDownload: (String, String) -> Unit, onShare: () -> Unit, onNeedLogin: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))) {
                AsyncImage(model = post.featuredImageUrl(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)))))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 16.dp).clip(RoundedCornerShape(20.dp)).background(AppdonyCardBg).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
                    val img = post.featuredImageUrl()
                    if (!img.isNullOrBlank()) AsyncImage(model = img, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else Icon(Icons.Default.Android, contentDescription = null, tint = AppdonyTextMuted, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(post.cleanTitle(), color = AppdonyTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(post.primaryCategoryName(), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        item {
            if (info.version != null || info.size != null || info.androidReq != null) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    info.version?.let { InfoChip("نسخه", it) }
                    info.size?.let { InfoChip("حجم", it) }
                    info.androidReq?.let { InfoChip("اندروید", it.take(10)) }
                }
            }
        }
        item {
            if (info.downloadLinks.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    info.downloadLinks.forEachIndexed { index, link ->
                        Button(onClick = { onDownload(link.url, post.cleanTitle()) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (index == 0) MaterialTheme.colorScheme.primary else AppdonyCardBg)) {
                            Icon(Icons.Default.Download, null, tint = if (index == 0) Color.White else AppdonyTextPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(link.title.ifBlank { "دانلود" }, color = if (index == 0) Color.White else AppdonyTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(24.dp))
            Text("توضیحات", color = AppdonyTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(8.dp))
            Text(info.cleanDescription.take(3000), color = AppdonyTextMuted, fontSize = 14.sp, lineHeight = 24.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
        item {
            Spacer(Modifier.height(24.dp))
            CommentsSection(postId = post.id, isLoggedIn = isLoggedIn, nonce = nonce, onNeedLogin = onNeedLogin)
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(AppdonyCardBg).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(label, color = AppdonyTextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = AppdonyTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
