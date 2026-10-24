package com.appdony.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard

@Composable
fun HomeScreen(
    isGamesTab: Boolean, // تعیین میکنه الان تب بازی‌ها فعاله یا برنامه‌ها
    onPostClick: (Int) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val posts by viewModel.posts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    // هر وقت کاربر تب رو عوض کرد (بین بازی و برنامه)، دیتا رو از اول لود کن
    LaunchedEffect(isGamesTab) {
        val category = if (isGamesTab) "game" else "app"
        viewModel.loadInitialPosts(category)
    }

    // تشخیص اسکرول به پایین صفحه برای لود کردن صفحه بعدی (اسکرول بی‌نهایت)
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

    if (isLoading && posts.isEmpty()) {
        // اسکلت لودینگ اولیه
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // بخش ویژه (فقط ۵ پست اول رو به عنوان ویژه نشون بده)
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
                    items(posts.take(5)) { post ->
                        AppCard(
                            title = post.cleanTitle(),
                            imageUrl = post.featuredImageUrl() ?: "",
                            onClick = { onPostClick(post.id) },
                            modifier = Modifier.width(200.dp)
                        )
                    }
                }
            }

            // بقیه پست‌ها
            items(posts.drop(5)) { post ->
                // اینجا میتونی یه کارت افقی برای لیست اصلی بذاری
                AppCard(
                    title = post.cleanTitle(),
                    imageUrl = post.featuredImageUrl() ?: "",
                    onClick = { onPostClick(post.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // نشانگر لودینگ پایین صفحه (وقتی داره صفحه بعدی رو میاره)
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
