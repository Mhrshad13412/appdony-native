package com.appdony.app.ui.screens

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun CategoryScreen(
    categoryId: Int,
    categoryName: String,
    onBack: () -> Unit,
    onPostClick: (Int) -> Unit
) {
    val vm: CategoryViewModel = viewModel(
        key = "cat_${categoryId}",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CategoryViewModel(categoryId, categoryName) as T
            }
        }
    )

    val state by vm.state.collectAsStateWithLifecycle()
    val title by vm.title.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg)
    ) {
        // نوار بالا
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = AppdonyTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = AppdonyTextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        when (val s = state) {
            is CategoryUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppdonyGreen)
                }
            }
            is CategoryUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = AppdonyTextMuted)
                }
            }
            is CategoryUiState.Success -> {
                if (s.posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("محتوایی نیست", color = AppdonyTextMuted)
                    }
                } else {
                    val gridState = rememberLazyGridState()

                    // لود صفحه بعد وقتی کاربر به آخر رسید
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            val total = gridState.layoutInfo.totalItemsCount
                            lastVisible >= total - 6
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        if (shouldLoadMore) vm.loadMore()
                    }

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
                                AppCard(
                                    title = post.cleanTitle(),
                                    imageUrl = post.featuredImageUrl(),
                                    onClick = { onPostClick(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
