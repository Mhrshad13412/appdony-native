package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    onCategoryClick: (Int, String) -> Unit
) {
    val vm: HomeViewModel = viewModel(
        key = "feed_blog",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(feedType = "blog") as T
            }
        }
    )

    val loading by vm.initialLoading.collectAsStateWithLifecycle()
    val errorMsg by vm.error.collectAsStateWithLifecycle()
    val categoriesList by vm.categories.collectAsStateWithLifecycle()
    val sectionStates by vm.sections.collectAsStateWithLifecycle()

    when {
        loading -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(4) { BlogCardSkeleton() }
            }
        }
        errorMsg != null -> {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(errorMsg!!, color = AppdonyTextMuted)
            }
        }
        else -> {
            val allPosts = sectionStates.values
                .filterIsInstance<SectionState.Loaded>()
                .flatMap { it.posts }
                .distinctBy { it.id }

            if (allPosts.isEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(3) { BlogCardSkeleton() }
                }
                categoriesList.forEach { cat ->
                    LaunchedEffect(cat.id) {
                        vm.loadSection(cat.id)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = "وبلاگ",
                                color = AppdonyTextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "آخرین مطالب و اخبار",
                                color = AppdonyTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                    items(allPosts, key = { it.id }) { post ->
                        BlogPostCard(post = post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BlogPostCard(post: Post, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppdonyCardBg)
            .clickable { onClick() }
            .padding(bottom = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            val img = post.featuredImageUrl()
            if (!img.isNullOrBlank()) {
                AsyncImage(
                    model = img,
                    contentDescription = post.cleanTitle(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            val catName = post.embedded?.terms?.firstOrNull()?.firstOrNull()?.name
            if (!catName.isNullOrBlank()) {
                Text(
                    text = catName,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = post.cleanTitle(),
                color = AppdonyTextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(6.dp))

            val excerpt = post.cleanExcerpt()
            if (excerpt.isNotBlank()) {
                Text(
                    text = excerpt.take(120),
                    color = AppdonyTextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(6.dp))
            }

            Text(
                text = post.date.take(10),
                color = AppdonyTextMuted,
                fontSize = 11.sp
            )
        }
    }
}
