package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanExcerpt
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.BlogCardSkeleton
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlogScreen(onPostClick: (Int) -> Unit, onCategoryClick: (Int, String) -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        errorMsg = null
        try {
            val result = withContext(Dispatchers.IO) {
                val api = ApiClient.api
                val categories = api.getCategories(perPage = 100)

                val gamesParent = categories.firstOrNull { it.parent == 0 && (it.slug.contains("game", true) || it.name.contains("بازی")) }
                val appsParent = categories.firstOrNull { it.parent == 0 && (it.slug.contains("app", true) || it.name.contains("برنامه")) }

                val allPosts = api.getPosts(perPage = 40)

                val gamesIds = if (gamesParent != null) (listOf(gamesParent.id) + categories.filter { it.parent == gamesParent.id }.map { it.id }).toSet() else emptySet()
                val appsIds = if (appsParent != null) (listOf(appsParent.id) + categories.filter { it.parent == appsParent.id }.map { it.id }).toSet() else emptySet()

                allPosts.filter { post -> post.categories.none { it in gamesIds || it in appsIds } }
            }
            posts = result
        } catch (e: Exception) {
            errorMsg = e.message ?: "خطا در دریافت اطلاعات"
        } finally {
            loading = false
        }
    }

    when {
        loading -> LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(vertical = 8.dp)) { items(4) { BlogCardSkeleton() } }
        errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(errorMsg!!, color = AppdonyTextMuted) }
        else -> {
            LazyColumn(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(vertical = 8.dp)) {
                item { Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { Text("وبلاگ", color = AppdonyTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("آخرین مطالب و اخبار", color = AppdonyTextMuted, fontSize = 13.sp) } }
                if (posts.isEmpty()) {
                    item { Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { Text("محتوایی برای نمایش نیست", color = AppdonyTextMuted) } }
                } else {
                    items(posts, key = { it.id }) { post -> BlogPostCard(post = post, onClick = { onPostClick(post.id) }) }
                }
            }
        }
    }
}

@Composable
private fun BlogPostCard(post: Post, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(AppdonyCardBg).clickable { onClick() }.padding(bottom = 14.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))) {
            val img = post.featuredImageUrl()
            if (!img.isNullOrBlank()) AsyncImage(model = img, contentDescription = post.cleanTitle(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
