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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.AppRowItem
import com.appdony.app.ui.components.SectionHeader
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun HomeScreen(
    onPostClick: (Int) -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is HomeUiState.Loading -> LoadingView()
        is HomeUiState.Error -> ErrorView(s.message) { viewModel.load() }
        is HomeUiState.Success -> HomeContent(s.posts, onPostClick)
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
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(text = "تلاش دوباره")
            }
        }
    }
}

@Composable
private fun HomeContent(posts: List<Post>, onPostClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppdonyBg),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
    ) {
        item {
            SectionHeader(title = "تازه‌ها")
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(posts.take(10), key = { it.id }) { post ->
                    AppCard(
                        title = post.cleanTitle(),
                        imageUrl = post.featuredImageUrl(),
                        onClick = { onPostClick(post.id) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            SectionHeader(title = "برترین‌ها")
        }
        itemsIndexed(
            items = posts.take(10),
            key = { _, post -> "top_${post.id}" }
        ) { index, post ->
            AppRowItem(
                rank = index + 1,
                title = post.cleanTitle(),
                imageUrl = post.featuredImageUrl(),
                onClick = { onPostClick(post.id) }
            )
        }
    }
}
