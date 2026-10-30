package com.appdony.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppCard
import com.appdony.app.ui.components.BannerSkeleton
import com.appdony.app.ui.components.CardSkeleton
import com.appdony.app.ui.components.SectionHeader
import com.appdony.app.ui.components.SectionRowSkeleton
import com.appdony.app.ui.components.SectionTitleSkeleton
import com.appdony.app.ui.components.ShimmerBox
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.produceState
import com.appdony.app.ui.components.FeaturedGameCard
import com.appdony.app.util.HtmlParser

@Composable
fun HomeScreen(
    isGamesTab: Boolean,
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit = { _, _ -> },
    viewModel: HomeViewModel = viewModel()
) {
    val featured by viewModel.featured.collectAsStateWithLifecycle()
    val plus by viewModel.plus.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()

    LaunchedEffect(isGamesTab) {
        viewModel.load(isGamesTab)
    }

    if (hasError && sections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("دریافت اطلاعات ممکن نشد. اینترنت را بررسی کن.", color = AppdonyTextMuted, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.load(isGamesTab, force = true) }) { Text("تلاش دوباره") }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item(key = "featured") {
            FeaturedBlock(title = "ویژه", keyPrefix = "featured", posts = featured, onPostClick = onPostClick)
        }

        item(key = "plus") {
            FeaturedBlock(title = "اپدونی پلاس", keyPrefix = "plus", posts = plus, onPostClick = onPostClick)
        }

        if (isLoading) {
            items(3, key = { "skeleton_$it" }) {
                Column {
                    SectionTitleSkeleton()
                    SectionRowSkeleton()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        items(sections, key = { it.section.id }) { state ->
            // وقتی این ردیف به صفحه نزدیک شد، داده‌ش گرفته می‌شه
            LaunchedEffect(state.section.id) {
                viewModel.loadSection(state.section.id)
            }
            CategorySection(
                state = state,
                onPostClick = onPostClick,
                onMoreClick = { onCategoryClick(state.section.id, state.section.title) }
            )
        }

        if (!isLoading && sections.isEmpty()) {
            item(key = "empty") {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("محتوایی برای نمایش نیست", color = AppdonyTextMuted)
                }
            }
        }
    }
}

// ───────────── بلوک «ویژه» و «اپدونی پلاس» ─────────────

@Composable
private fun FeaturedBlock(title: String, keyPrefix: String, posts: List<Post>?, onPostClick: (Int) -> Unit) {
    if (posts != null && posts.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SectionHeader(title = title)

        if (posts == null) {
            LazyRow(
                userScrollEnabled = false,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(2) { BannerSkeleton() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                    FadeIn(key = "$keyPrefix-${post.id}", index = index) {
                        FeaturedCard(post = post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun FeaturedCard(post: Post, onClick: () -> Unit) {
    val icon = post.safeImage()
    val category = post.embedded?.terms?.firstOrNull()?.firstOrNull()?.name
    // اسکرین‌شات از توی متن پست پیدا می‌شه (پشت صحنه، تا اسکرول سنگین نشه)
    val screenshot by produceState<String?>(initialValue = null, key1 = post.id) {
        value = withContext(Dispatchers.Default) { post.firstScreenshot(icon) }
    }
    FeaturedGameCard(
        title = post.cleanTitle(),
        category = category,
        screenshotUrl = screenshot,
        iconUrl = icon,
        onClick = onClick
    )
}

private fun shotName(url: String): String =
    url.substringBefore('?').substringAfterLast('/').replace(Regex("-\\d+x\\d+(?=\\.)"), "").lowercase()

/** اولین عکس متن پست که همون تصویر شاخص (آیکن) نباشه */
private fun Post.firstScreenshot(featured: String?): String? = try {
    val featuredName = featured?.let { shotName(it) }
    HtmlParser.parsePostContent(content.rendered).screenshots.firstOrNull { shotName(it) != featuredName }
} catch (e: Exception) {
    null
}

// ───────────── ردیف‌های دسته‌بندی ─────────────

@Composable
private fun CategorySection(
    state: SectionState,
    onPostClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    val posts = state.posts

    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        // عنوان همیشه فوراً نشون داده می‌شه
        SectionHeader(title = state.section.title, onMoreClick = onMoreClick)

        if (posts == null) {
            LazyRow(
                userScrollEnabled = false,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) { CardSkeleton() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                    FadeIn(key = "${state.section.id}-${post.id}", index = index) {
                        AppCard(
                            title = post.cleanTitle(),
                            imageUrl = post.safeImage(),
                            onClick = { onPostClick(post.id) }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// ───────────── کمکی‌ها ─────────────

// ظاهر شدن تدریجی کارت‌ها (یکی‌یکی)
@Composable
private fun FadeIn(key: Any, index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable(key) { mutableStateOf(false) }

    LaunchedEffect(key) {
        if (!visible) {
            delay(index.coerceAtMost(6) * 70L)
            visible = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "fade_in"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = (1f - alpha) * 30f
        }
    ) {
        content()
    }
}

// درخواست‌های لیست فیلد content ندارن؛ این تابع از کرش جلوگیری می‌کنه
private fun Post.safeImage(): String? {
    return try {
        featuredImageUrl()
    } catch (e: Exception) {
        null
    }
}
