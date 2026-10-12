package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanExcerpt
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.BannerCard
import com.appdony.app.ui.components.CategoryRow
import com.appdony.app.ui.components.SectionHeader
import com.appdony.app.ui.components.SingleFeatureCard
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted

@Composable
fun HomeScreen(
    onPostClick: (Int) -> Unit = {},
    onCategoryClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is HomeUiState.Loading -> LoadingView()
        is HomeUiState.Error -> ErrorView(s.message) { viewModel.load() }
        is HomeUiState.Success -> HomeContent(
            state = s,
            onPostClick = onPostClick,
            onCategoryClick = onCategoryClick
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize().background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AppdonyGreen)
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(AppdonyBg),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, color = AppdonyTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = AppdonyGreen)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "تلاش دوباره")
            }
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int) -> Unit
) {
    val data = state.data
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppdonyBg),
        contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)
    ) {
        // ۱. بنر بزرگ (اولین پست)
        data.banner?.let { banner ->
            item {
                BannerCard(
                    title = banner.cleanTitle(),
                    imageUrl = banner.featuredImageUrl(),
                    onClick = { onPostClick(banner.id) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // ۲. کارت ویژه تک‌تایی
        data.singleFeature?.let { feature ->
            item {
                SingleFeatureCard(
                    title = feature.cleanTitle(),
                    description = feature.cleanExcerpt(),
                    imageUrl = feature.featuredImageUrl(),
                    onClick = { onPostClick(feature.id) }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        // ۳. سکشن‌های دسته‌بندی — همه افقی
        data.sections.forEach { section ->
            item(key = "section_${section.categoryId}") {
                CategoryRow(
                    title = section.title,
                    posts = section.posts,
                    onPostClick = onPostClick,
                    onMoreClick = { onCategoryClick(section.categoryId) }
                )
            }
        }

        // اگه هیچ سکشنی نبود
        if (data.sections.isEmpty() && data.banner == null) {
            item {
                Box(
                    modifier = Modifier.fillMaxSize().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "محتوایی برای نمایش نیست",
                        color = AppdonyTextMuted
                    )
                }
            }
        }
    }
}
