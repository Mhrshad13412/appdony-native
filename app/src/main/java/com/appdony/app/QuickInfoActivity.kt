package com.appdony.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanExcerpt
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.ui.theme.AppdonyTheme
import com.appdony.app.util.AppInfo
import com.appdony.app.util.HtmlParser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * پنجره‌ی شناور «نصب سریع»: با زدن لینک appdony.ir (از مرورگر، تلگرام و ...)
 * خودکار از بالا به پایین میاد و خلاصه‌ی اپ رو نشون می‌ده؛ بقیه‌ی صفحه‌ی قبلی پشتش دیده می‌شه.
 */
class QuickInfoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val slug = intent?.data?.lastPathSegment?.takeIf { it.isNotBlank() }
        if (slug == null) {
            openMain(null)
            finish()
            return
        }
        setContent {
            QuickInfoRoot(
                slug = slug,
                onClose = { finish() },
                onOpen = { route ->
                    openMain(route)
                    finish()
                }
            )
        }
    }

    private fun openMain(route: String?) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra("route", route)
        )
    }
}

private sealed interface QiState {
    data object Loading : QiState
    data class Ready(val post: Post, val info: AppInfo) : QiState
    data object Failed : QiState
}

private suspend fun fetchPost(slug: String): Post? = withContext(Dispatchers.IO) {
    runCatching {
        val url = "https://appdony.ir/wp-json/wp/v2/posts?slug=${URLEncoder.encode(slug, "UTF-8")}&_embed=1"
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            val body = r.body?.string() ?: return@use null
            val type = object : TypeToken<List<Post>>() {}.type
            Gson().fromJson<List<Post>>(body, type).firstOrNull()
        }
    }.getOrNull()
}

@Composable
private fun QuickInfoRoot(slug: String, onClose: () -> Unit, onOpen: (String?) -> Unit) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val ts by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val color = ThemeManager.themeColors.getOrElse(ts.colorIndex) { ThemeManager.themeColors[0] }

    var state by remember { mutableStateOf<QiState>(QiState.Loading) }
    var show by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { show = true }
    LaunchedEffect(slug) {
        val p = fetchPost(slug)
        state = if (p == null) {
            QiState.Failed
        } else {
            QiState.Ready(p, withContext(Dispatchers.Default) { HtmlParser.parsePostContent(p.content.rendered) })
        }
    }
    // لینک پست نبود یا خطا شد: مستقیم اپ باز می‌شه
    LaunchedEffect(state) { if (state is QiState.Failed) onOpen(null) }
    LaunchedEffect(closing) {
        if (closing) {
            show = false
            delay(260)
            onClose()
        }
    }

    AppdonyTheme(darkTheme = ts.mode == ThemeMode.DARK, primaryColor = color) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val scrim by animateFloatAsState(if (show) 0.45f else 0f, tween(300), label = "scrim")
            var dragSum by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = scrim))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { closing = true }
            ) {
                AnimatedVisibility(
                    visible = show,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = slideInVertically(spring(0.75f, Spring.StiffnessMediumLow)) { -it } + fadeIn(tween(200)),
                    exit = slideOutVertically(tween(220)) { -it } + fadeOut(tween(180))
                ) {
                    Surface(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onVerticalDrag = { _, dy -> dragSum += dy },
                                    onDragEnd = {
                                        if (dragSum < -60f) closing = true
                                        dragSum = 0f
                                    }
                                )
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { },
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 24.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    ) {
                        when (val s = state) {
                            is QiState.Ready -> InfoCard(
                                post = s.post,
                                info = s.info,
                                onOpen = { onOpen("detail/${s.post.id}?isBlog=false") },
                                onLater = { closing = true }
                            )
                            else -> Row(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("در حال دریافت اطلاعات...", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(post: Post, info: AppInfo, onOpen: () -> Unit, onLater: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val category = post.embedded?.terms?.flatten()?.firstOrNull { it.taxonomy == "category" }?.name

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.featuredImageUrl(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(onSurface.copy(alpha = 0.08f))
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.cleanTitle(),
                    color = onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    post.cleanExcerpt(),
                    color = onSurface.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("حجم", info.size ?: "—", Modifier.weight(1f))
            StatTile("نسخه", info.version ?: "—", Modifier.weight(1f))
            StatTile("دسته", category ?: "—", Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onOpen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("مشاهده و دانلود", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp)
            ) { Text("بعداً") }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(onSurface.copy(alpha = 0.06f))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            color = onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, color = onSurface.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}
