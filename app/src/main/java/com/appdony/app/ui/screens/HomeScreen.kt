package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.BannerCard
import com.appdony.app.ui.components.SectionHeader
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted

@Composable
fun FeedScreen(
    feedType: String,
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit
) {
    val vm: HomeViewModel = viewModel(
        key = "feed_$feedType",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(feedType = feedType) as T
            }
        }
    )

    val loading by vm.initialLoading.collectAsStateWithLifecycle()
    val errorMsg by vm.error.collectAsStateWithLifecycle()
    val plusPosts by vm.plusPosts.collectAsStateWithLifecycle()
    val featuredList by vm.featured.collectAsStateWithLifecycle()
    val categoriesList by vm.categories.collectAsStateWithLifecycle()
    val sectionStates by vm.sections.collectAsStateWithLifecycle()

    when {
        loading -> LoadingView()
        errorMsg != null -> ErrorView(errorMsg!!) { vm.loadInitial() }
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppdonyBg),
            contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ═══ بلاک بالایی: اپدونی پلاس ═══
            val topPosts = if (plusPosts.isNotEmpty()) plusPosts else featuredList
            val topTitle = if (plusPosts.isNotEmpty()) "اپدونی پلاس" else "ویژه"

            if (topPosts.isNotEmpty()) {
                item(key = "top_title") {
                    SectionHeader(title = topTitle)
                }
                item(key = "top_row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topPosts, key = { "top_${it.id}" }) { p ->
                            BannerCard(
                                title = p.cleanTitle(),
                                imageUrl = p.featuredImageUrl(),
                                onClick = { onPostClick(p.id) }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ═══ سکشن‌های دسته‌بندی ═══
            items(categoriesList, key = { "cat_${it.id}" }) { cat ->
                val sectionState = sectionStates[cat.id] ?: SectionState.NotLoaded

                LaunchedEffect(cat.id) {
                    if (sectionState is SectionState.NotLoaded) {
                        vm.loadSection(cat.id)
                    }
                }

                SectionBlock(
                    title = cat.name,
                    state = sectionState,
                    onPostClick = onPostClick,
                    onMoreClick = { onCategoryClick(cat.id, cat.name) }
                )
            }
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    state: SectionState,
    onPostClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        SectionHeader(title = title, onMoreClick = onMoreClick)

        when (state) {
            is SectionState.NotLoaded, is SectionState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppdonyGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            is SectionState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = AppdonyTextMuted, fontSize = 12.sp)
                }
            }
            is SectionState.Loaded -> {
                if (state.posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("محتوایی نیست", color = AppdonyTextMuted, fontSize = 12.sp)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.posts, key = { it.id }) { p ->
                            AppCard(
                                title = p.cleanTitle(),
                                imageUrl = p.featuredImageUrl(),
                                onClick = { onPostClick(p.id) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppdonyGreen, modifier = Modifier.size(44.dp))
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = AppdonyTextMuted, fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("  تلاش دوباره")
            }
        }
    }
}
