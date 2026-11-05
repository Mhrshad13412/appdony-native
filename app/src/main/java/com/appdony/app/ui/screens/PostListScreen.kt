package com.appdony.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.api.EditorApi
import com.appdony.app.data.api.PostRow
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.ui.components.glassKnob
import com.appdony.app.ui.components.glassSelectedIconTint
import com.appdony.app.ui.components.glassTrack
import kotlinx.coroutines.launch

/** لیست مطالب فقط توی حافظه‌ی همین صفحه می‌مونه؛ چیزی روی گوشی ذخیره نمی‌شه */
class PostListViewModel : ViewModel() {
    var rows by mutableStateOf<List<PostRow>>(emptyList())
    var loading by mutableStateOf(false)
    var loadingMore by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var hasMore by mutableStateOf(false)
    var scopeAll by mutableStateOf(false)
    var loadedOnce by mutableStateOf(false)
    private var page = 1

    /** لیست رو از اول می‌گیره؛ تا جواب نیومده همون لیست قبلی دیده می‌شه */
    fun refresh(context: Context, kind: String) {
        if (loading) return
        loading = true
        error = null
        val app = context.applicationContext
        viewModelScope.launch {
            EditorApi.list(app, kind, 1).onSuccess {
                rows = it.rows
                hasMore = it.hasMore
                scopeAll = it.all
                page = 1
                loadedOnce = true
            }.onFailure {
                error = it.message ?: "لیست گرفته نشد"
            }
            loading = false
        }
    }

    fun more(context: Context, kind: String) {
        if (loading || loadingMore || !hasMore) return
        loadingMore = true
        val app = context.applicationContext
        viewModelScope.launch {
            EditorApi.list(app, kind, page + 1).onSuccess { next ->
                val known = rows.map { it.id }.toSet()
                rows = rows + next.rows.filter { it.id !in known }
                hasMore = next.hasMore
                page += 1
            }.onFailure {
                hasMore = false
                AppBanner.show(BannerKind.ERROR, it.message ?: "ادامه‌ی لیست گرفته نشد", 4500L)
            }
            loadingMore = false
        }
    }
}

/**
 * صفحه‌ی «نوشته‌ها» / «بلاگ‌ها». مدیر و ویرایشگر همه‌ی مطالب رو می‌بینن، نویسنده فقط مطالب خودش رو.
 * زدن روی هر مورد اون رو توی ادیتور باز می‌کنه و دکمه‌ی «جدید» ادیتور خالی.
 */
@Composable
fun PostListScreen(
    kind: String,
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit
) {
    val isBlog = kind == "blog"
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val vm: PostListViewModel = viewModel(key = "posts_$kind")
    val listState = rememberLazyListState()

    // هر بار که صفحه باز می‌شه (از جمله برگشت از ادیتور) لیست تازه گرفته می‌شه
    LaunchedEffect(kind) { vm.refresh(ctx, kind) }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, vm.rows.size) {
        if (nearEnd && vm.rows.isNotEmpty()) vm.more(ctx, kind)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // نوار بالا: بازگشت، عنوان، تازه‌سازی و «جدید»
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "بازگشت", tint = scheme.onSurface)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = if (isBlog) "بلاگ‌ها" else "نوشته‌ها",
                    color = scheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (vm.loadedOnce) {
                    Text(
                        text = if (vm.scopeAll) "همه‌ی مطالب سایت" else "مطالب خودت",
                        color = scheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(enabled = !vm.loading) { vm.refresh(ctx, kind) },
                contentAlignment = Alignment.Center
            ) {
                if (vm.loading && vm.loadedOnce) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = scheme.primary)
                } else {
                    Icon(Icons.Outlined.Refresh, contentDescription = "تازه‌سازی", tint = scheme.onSurface)
                }
            }
            Spacer(Modifier.width(4.dp))
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .glassKnob(RoundedCornerShape(20.dp))
                    .clickable(onClick = onNew)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = glassSelectedIconTint(), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("جدید", color = glassSelectedIconTint(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            val error = vm.error
            when {
                !vm.loadedOnce && error == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = scheme.primary)
                }

                !vm.loadedOnce && error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = error, color = scheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { vm.refresh(ctx, kind) }) { Text("تلاش دوباره") }
                    }
                }

                vm.rows.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (vm.scopeAll) "هنوز مطلبی وجود نداره" else "هنوز چیزی ننوشتی",
                            color = scheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "با دکمه‌ی «جدید» اولین مطلب رو بساز",
                            color = scheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vm.rows, key = { it.id }) { row ->
                            PostRowCard(row = row, onClick = { onOpen(row.id) })
                        }
                        if (vm.loadingMore) {
                            item(key = "more") {
                                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = scheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostRowCard(row: PostRow, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassTrack(shape)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f)),
            contentAlignment = Alignment.Center
        ) {
            if (row.thumb != null) {
                AsyncImage(
                    model = row.thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.title,
                color = scheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(row.status)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = row.date.replace('T', ' ').take(16),
                    color = scheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = scheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusChip(status: String) {
    val scheme = MaterialTheme.colorScheme
    val (label, color) = when (status) {
        "publish" -> "منتشرشده" to Color(0xFF2E9E6B)
        "future" -> "زمان‌بندی‌شده" to Color(0xFF4C8DF6)
        "pending" -> "در انتظار بازبینی" to Color(0xFFE0A030)
        "private" -> "خصوصی" to Color(0xFF9B6BD6)
        else -> "پیش‌نویس" to scheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
