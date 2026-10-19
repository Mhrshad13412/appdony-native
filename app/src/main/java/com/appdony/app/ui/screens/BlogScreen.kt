package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanExcerpt
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.BlogCardSkeleton
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun BlogScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit,
    viewModel: BlogViewModel = viewModel()
) {
    val context = LocalContext.current
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val errorMsg by viewModel.errorMsg.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    // 👇 تشخیص اینکه کاربر به انتهای لیست رسیده یا نه
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) false
            else {
                val lastVisibleItem = visibleItems.last()
                lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
            }
        }
    }

    // وقتی به انتهای لیست رسید، صفحه بعد رو لود کن
    LaunchedEffect(isAtBottom) {
        if (isAtBottom && posts.isNotEmpty()) {
            viewModel.loadMore()
        }
    }

    when {
        isLoading && posts.isEmpty() -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) { items(4) { BlogCardSkeleton() } }
        }

        errorMsg != null && posts.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg!!, color = AppdonyTextMuted)
            }
        }

        else -> {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("وبلاگ", color = AppdonyTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("آخرین مطالب و اخبار", color = AppdonyTextMuted, fontSize = 13.sp)
                    }
                }

                if (posts.isEmpty() && !isLoading) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("محتوایی برای نمایش نیست", color = AppdonyTextMuted)
                        }
                    }
                } else {
                    items(posts, key = { it.id }) { post ->
                        BlogPostCard(post = post, context = context, onClick = { onPostClick(post.id) })
                    }
                }

                // 👇 لودینگ پایین صفحه وقتی داره دیتای جدید می‌گیره
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlogPostCard(post: Post, context: android.content.Context, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp)).background(AppdonyCardBg).clickable { onClick() }.padding(bottom = 14.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            val img = post.featuredImageUrl()
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(img).crossfade(true).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
                    contentDescription = post.cleanTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            val catName = post.embedded?.terms?.firstOrNull()?.firstOrNull()?.name
            if (!catName.isNullOrBlank()) { Text(catName, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(4.dp)) }
            Text(post.cleanTitle(), color = AppdonyTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 22.sp)
            Spacer(Modifier.height(6.dp))
            val excerpt = post.cleanExcerpt()
            if (excerpt.isNotBlank()) { Text(excerpt.take(120), color = AppdonyTextMuted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp); Spacer(Modifier.height(6.dp)) }
            Text(post.date.take(10), color = AppdonyTextMuted, fontSize = 11.sp)
        }
    }
}
