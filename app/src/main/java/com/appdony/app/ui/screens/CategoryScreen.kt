package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.CardSkeleton
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted

@Composable
fun CategoryScreen(
    categoryId: Int,
    categoryName: String,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val vm: CategoryViewModel = viewModel(
        key = "cat_${categoryId}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoryViewModel(categoryId, categoryName, context) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()
    val isLoadingMore by vm.isLoadingMore.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()

    // 👇 تشخیص اینکه کاربر به ۵ کارت آخر رسیده یا نه
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = gridState.layoutInfo.totalItemsCount
            if (lastVisibleItem == null || totalItems == 0) false
            else lastVisibleItem.index >= totalItems - 5
        }
    }

    // 👇 وقتی رسید، دیتای بعدی رو بگیر
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            vm.loadMore()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // نوار بالا
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = MaterialTheme.colorScheme.onBackground)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        when (val s = state) {
            is CategoryUiState.Loading -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(12) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CardSkeleton() }
                    }
                }
            }
            is CategoryUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(s.message, color = AppdonyTextMuted) }
            }
            is CategoryUiState.Success -> {
                if (s.posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("محتوایی نیست", color = AppdonyTextMuted) }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(s.posts, key = { it.id }) { post ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                AppCard(title = post.cleanTitle(), imageUrl = post.featuredImageUrl(), onClick = { onPostClick(post.id) })
                            }
                        }

                        // 👇 لودینگ ته لیست وقتی داره صفحه بعد رو می‌گیره
                        if (isLoadingMore) {
                            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
