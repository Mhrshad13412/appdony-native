package com.appdony.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.AppRowItem

@Composable
fun HomeScreen(
    isGamesTab: Boolean,
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit = { _, _ -> }, // ✅ امضا با AppNavGraph هماهنگ شد
    viewModel: HomeViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val searchQuery = if (isGamesTab) "بازی" else "برنامه"

    LaunchedEffect(isGamesTab) {
        viewModel.loadInitialPosts(searchQuery)
    }

    // جلوگیری از آیدی تکراری (وقتی صفحه‌ی بعد پست تکراری برگردونه)
    val uniquePosts = remember(posts) { posts.distinctBy { it.id } }
    val featured = remember(uniquePosts) { uniquePosts.take(8) }
    val rest = remember(uniquePosts) { uniquePosts.drop(8) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMorePosts()
        }
    }

    when {
        isLoading && uniquePosts.isEmpty() -> HomeSkeleton()

        uniquePosts.isEmpty() -> EmptyHome(onRetry = { viewModel.loadInitialPosts(searchQuery) })

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "ویژه",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(featured, key = { it.id }) { post ->
                        AppCard(
                            title = post.cleanTitle(),
                            imageUrl = post.featuredImageUrl() ?: "",
                            onClick = { onPostClick(post.id) }
                        )
                    }
                }
            }

            if (rest.isNotEmpty()) {
                item {
                    Text(
                        text = "جدیدترین‌ها",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                items(rest, key = { it.id }) { post ->
                    AppRowItem(
                        rank = 0,
                        title = post.cleanTitle(),
                        imageUrl = post.featuredImageUrl(),
                        onClick = { onPostClick(post.id) },
                        showRank = false
                    )
                }
            }

            if (isLoadingMore) {
                item { LoadingMoreSkeleton() }
            }
        }
    }
}

@Composable
private fun EmptyHome(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "نتیجه‌ای دریافت نشد. اتصال اینترنت را بررسی کن.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text("تلاش دوباره") }
        }
    }
}

// ───────────── اسکلتون لودینگ (نور عرضی مثل یوتیوب) ─────────────

@Composable
private fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "home_shimmer")
    val x by transition.animateFloat(
        initialValue = -500f,
        targetValue = 1300f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "home_shimmer_x"
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    val shine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f).compositeOver(base)
    return Brush.linearGradient(
        colors = listOf(base, shine, base),
        start = Offset(x, 0f),
        end = Offset(x + 400f, 0f)
    )
}

@Composable
private fun Placeholder(brush: Brush, modifier: Modifier, radius: Dp = 8.dp) {
    Box(modifier = modifier.clip(RoundedCornerShape(radius)).background(brush))
}

@Composable
private fun HomeSkeleton() {
    val brush = rememberShimmerBrush()
    Column(modifier = Modifier.fillMaxSize()) {
        Placeholder(
            brush = brush,
            modifier = Modifier.padding(16.dp).width(70.dp).height(22.dp),
            radius = 6.dp
        )
        LazyRow(
            userScrollEnabled = false,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(4) {
                Column(modifier = Modifier.width(100.dp)) {
                    Placeholder(brush, Modifier.size(100.dp), 16.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Placeholder(brush, Modifier.fillMaxWidth().height(12.dp), 4.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Placeholder(brush, Modifier.width(60.dp).height(10.dp), 4.dp)
                }
            }
        }
        Placeholder(
            brush = brush,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                .width(110.dp).height(22.dp),
            radius = 6.dp
        )
        repeat(6) { RowSkeleton(brush) }
    }
}

@Composable
private fun RowSkeleton(brush: Brush) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Placeholder(brush, Modifier.size(56.dp), 14.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Placeholder(brush, Modifier.fillMaxWidth(0.75f).height(14.dp), 4.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Placeholder(brush, Modifier.width(60.dp).height(12.dp), 4.dp)
        }
    }
}

@Composable
private fun LoadingMoreSkeleton() {
    val brush = rememberShimmerBrush()
    Column {
        RowSkeleton(brush)
        RowSkeleton(brush)
    }
}
