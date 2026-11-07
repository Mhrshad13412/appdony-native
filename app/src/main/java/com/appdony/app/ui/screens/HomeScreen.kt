package com.appdony.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.PostCard
import com.appdony.app.ui.components.rememberPostMeta
import com.appdony.app.data.model.isNew
import com.appdony.app.ui.components.BannerSkeleton
import com.appdony.app.ui.components.CardSkeleton
import com.appdony.app.ui.components.LocalBottomBarClearance
import com.appdony.app.ui.components.SectionHeader
import com.appdony.app.ui.components.SectionRowSkeleton
import com.appdony.app.ui.components.SectionTitleSkeleton
import com.appdony.app.ui.components.TopChartsBlock
import com.appdony.app.ui.theme.AppdonyTextMuted
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
    // ⚠️ مهم — ViewModel هر تب به اکتیویتی وصله (نه به ورودی ناوبری)؛ پشته‌ی ناوبری فقط «بازی‌ها» + یک تب دیگه رو نگه می‌داره
    // و اگه ViewModel به ورودی ناوبری وصل باشه، هر بار که از تب دیگه‌ای میای «برنامه‌ها»، همه‌چیز از صفر لود می‌شه.
    // با این روش، تب برنامه‌ها هم مثل بازی‌ها داده‌ش رو نگه می‌داره و اسکلت/لودینگ دوباره نمیاد.
    viewModel: HomeViewModel = homeTabViewModel(isGamesTab)
) {
    val plus by viewModel.plus.collectAsStateWithLifecycle()
    // بتا خاموشه مگه کاربر از تنظیمات روشنش کنه؛ بلوک اپدونی پلاس فقط با بتا میاد
    val homeContext = androidx.compose.ui.platform.LocalContext.current
    val beta by androidx.compose.runtime.remember { com.appdony.app.data.preferences.SettingsManager(homeContext) }
        .beta.collectAsStateWithLifecycle(initialValue = false)
    val topCharts by viewModel.topCharts.collectAsStateWithLifecycle()
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

    // ⚠️ مهم — لود اولویت‌دار صفحه‌ی اصلی؛ تحت هیچ عنوان حذف یا ساده نشه ⚠️
    // ۱) اسکلت ردیف‌ها ۲) عنوان ردیف‌ها (همیشه فوری) ۳) هر ردیف فقط وقتی نزدیک صفحه شد لود می‌شه (HomeViewModel.loadSection)
    // ۴) هر کارت با قاب و متن فوری میاد و اندروید/نسخه و عکسش لودینگ جدا دارن (AppCard.kt)
    // هیچ تأخیر ساختگی‌ای نیست؛ همه‌چیز به محض آماده شدن داده نشون داده می‌شه.
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // پایین لیست جا می‌ذاریم تا آخرین ردیف زیر منوی شیشه‌ای نمونه
        contentPadding = PaddingValues(bottom = 24.dp + LocalBottomBarClearance.current)
    ) {
        if (beta) {
            item(key = "plus") {
                PlusBlock(posts = plus, onPostClick = onPostClick)
            }
        }

        if (isLoading) {
            items(1, key = { "skeleton_$it" }) {
                Column {
                    SectionTitleSkeleton()
                    SectionRowSkeleton()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        // اولین بلوک (پربازدیدترین‌ها)
        items(sections.take(1), key = { it.section.id }) { state ->
            HomeSectionRow(state = state, viewModel = viewModel, onPostClick = onPostClick, onCategoryClick = onCategoryClick)
        }

        // «برترین‌ها»: ۱۰ تا از بیشترین نصب و بیشترین امتیاز، درست بعد از اولین بلوک (پربازدیدترین‌ها) و قبل از بقیه‌ی ردیف‌ها
        item(key = "top_charts") {
            TopChartsBlock(data = topCharts, onPostClick = onPostClick)
        }

        if (isLoading) {
            items(2, key = { "skeleton_${it + 1}" }) {
                Column {
                    SectionTitleSkeleton()
                    SectionRowSkeleton()
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        items(sections.drop(1), key = { it.section.id }) { state ->
            HomeSectionRow(state = state, viewModel = viewModel, onPostClick = onPostClick, onCategoryClick = onCategoryClick)
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

/** یک ردیف صفحه‌ی اصلی؛ وقتی به صفحه نزدیک شد داده‌ش گرفته می‌شه */
@Composable
private fun HomeSectionRow(
    state: SectionState,
    viewModel: HomeViewModel,
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit
) {
    // وقتی این ردیف به صفحه نزدیک شد، داده‌ش گرفته می‌شه
    LaunchedEffect(state.section.id) {
        viewModel.loadSection(state.section.id)
    }
    val onMore: () -> Unit = {
        // ردیف‌های ویژه دسته‌ی واقعی ندارن؛ دسته‌ی اصلی تب باز می‌شه
        val target = if (state.section.id < 0) viewModel.rootCategoryId else state.section.id
        if (target != null) onCategoryClick(target, state.section.title)
    }
    if (state.plusStyle) {
        PlusSimpleSection(state = state, onPostClick = onPostClick, onMoreClick = onMore)
    } else {
        CategorySection(state = state, onPostClick = onPostClick, onMoreClick = onMore)
    }
}

/** ViewModel تب (بازی‌ها/برنامه‌ها) که با اکتیویتی زنده می‌مونه، نه با ورودی ناوبری */
@Composable
private fun homeTabViewModel(isGamesTab: Boolean): HomeViewModel {
    var ctx: Context = LocalContext.current
    while (ctx is ContextWrapper && ctx !is ComponentActivity) ctx = ctx.baseContext
    val key = if (isGamesTab) "home_games" else "home_apps"
    return if (ctx is ComponentActivity) {
        viewModel(viewModelStoreOwner = ctx, key = key)
    } else {
        viewModel(key = key)
    }
}

// ───────────── بلوک «اپدونی پلاس» ─────────────

@Composable
private fun PlusBlock(posts: List<Post>?, onPostClick: (Int) -> Unit) {
    if (posts != null && posts.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        SectionHeader(title = "اپدونی پلاس")

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
                    FadeIn(key = "plus-${post.id}", index = index) {
                        PlusCard(post = post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun PlusCard(post: Post, onClick: () -> Unit, simple: Boolean = false) {
    val icon = post.safeImage()
    val category = post.embedded?.terms?.firstOrNull()?.firstOrNull()?.name
    // اسکرین‌شات از توی متن پست پیدا می‌شه (پشت صحنه، تا اسکرول سنگین نشه)
    val screenshot by produceState<String?>(initialValue = null, key1 = post.id) {
        value = withContext(Dispatchers.Default) { post.firstScreenshot(icon) }
    }
    val meta = rememberPostMeta(post)
    FeaturedGameCard(
        title = post.cleanTitle(),
        category = category,
        screenshotUrl = screenshot,
        iconUrl = icon,
        onClick = onClick,
        isNew = post.isNew(),
        androidReq = meta.androidReq,
        version = meta.version,
        simple = simple
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

// ───────────── ردیف‌های «پلاس ساده» ─────────────
// همون کارت اپدونی پلاس (اسکرین‌شات بزرگ + آیکن + اسم) ولی به رنگ تم و بدون نور/برق/نقطه؛
// عنوان و دکمه‌ی «مشاهده همه» هم مثل ردیف‌های معمولیه

@Composable
private fun PlusSimpleSection(
    state: SectionState,
    onPostClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    val posts = state.posts

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = state.section.title, onMoreClick = onMoreClick)

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
                    FadeIn(key = "${state.section.id}-plus-${post.id}", index = index) {
                        PlusCard(post = post, onClick = { onPostClick(post.id) }, simple = true)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ───────────── ردیف‌های دسته‌بندی ─────────────

@Composable
private fun CategorySection(
    state: SectionState,
    onPostClick: (Int) -> Unit,
    onMoreClick: () -> Unit
) {
    val posts = state.posts

    Column(modifier = Modifier.fillMaxWidth()) {
        // عنوان همیشه فوراً نشون داده می‌شه
        SectionHeader(title = state.section.title, onMoreClick = onMoreClick)

        if (posts == null) {
            LazyRow(
                userScrollEnabled = false,
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(4) { CardSkeleton() }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(posts, key = { _, post -> post.id }) { index, post ->
                    FadeIn(key = "${state.section.id}-${post.id}", index = index) {
                        PostCard(post = post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

// ───────────── کمکی‌ها ─────────────

// ظاهر شدن نرم کارت‌ها؛ بدون هیچ تأخیر ساختگی: هر کارت همون لحظه‌ای که داده‌ش رسید شروع به محو شدن می‌کنه
// (پارامتر index فقط برای سازگاری با جاهای صدا زدن مونده)
// کارتی که قبلاً یک بار ظاهر شده، موقع برگشتن به تب دوباره محو/ظاهر نمی‌شه (مثل تب بازی‌ها که همیشه می‌مونه)
private val shownFades = HashSet<Any>()

@Composable
private fun FadeIn(key: Any, @Suppress("UNUSED_PARAMETER") index: Int, content: @Composable () -> Unit) {
    var visible by rememberSaveable(key) { mutableStateOf(key in shownFades) }

    LaunchedEffect(key) {
        visible = true
        shownFades.add(key)
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "fade_in"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = (1f - alpha) * 16f
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
