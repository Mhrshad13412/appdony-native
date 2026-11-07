package com.appdony.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.model.PostMemory
import com.appdony.app.data.model.TopChartEntry
import com.appdony.app.data.model.TopChartsData
import com.appdony.app.data.model.cardCategory
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.theme.AppdonyTextMuted
import java.util.Locale

/**
 * بلوک «برترین‌ها»: ۱۰ تا از بیشترین نصب و ۱۰ تا از بیشترین امتیاز.
 * ردیف‌ها همون AppRowItem ـن (کلاسیک: کارت مات؛ شیشه‌ای: شیشه‌ی منوی پایین) و کلیدِ دو حالتی هم با glassTrack/glassKnob.
 * [data] = null یعنی هنوز لود نشده (اسکلت)؛ اگه هر دو لیست خالی بود بلوک نمایش داده نمی‌شه.
 */
@Composable
fun TopChartsBlock(
    data: TopChartsData?,
    onPostClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    if (data != null && data.isEmpty) return

    Column(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        SectionHeader(title = "برترین‌ها")

        if (data == null) {
            repeat(3) { TopChartRowSkeleton() }
        } else {
            TopChartsTabs(selected = tab, onSelect = { tab = it })
            Spacer(modifier = Modifier.height(8.dp))
            Crossfade(targetState = tab, label = "top_charts_list") { t ->
                val list = if (t == 0) data.installed else data.rated
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (list.isEmpty()) {
                        Text(
                            text = "موردی برای نمایش نیست",
                            color = AppdonyTextMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        )
                    }
                    list.forEachIndexed { index, entry ->
                        key(entry.post.id) {
                            TopChartRow(rank = index + 1, entry = entry, onPostClick = onPostClick)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TopChartsTabs(selected: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val titles = listOf("بیشترین نصب", "بیشترین امتیاز")
    val shape = RoundedCornerShape(18.dp)
    val selectedTint = glassSelectedIconTint()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(48.dp)
            .glassTrack(shape)
    ) {
        val cell = maxWidth / titles.size
        // آفست توی RTL از راست حساب می‌شه، یعنی هم‌جهت با ترتیب گزینه‌ها (مثل PillSelector)
        val x by animateDpAsState(
            targetValue = cell * selected,
            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
            label = "top_charts_knob"
        )
        Box(
            modifier = Modifier
                .offset(x = x)
                .size(width = cell, height = 48.dp)
                .padding(4.dp)
                .glassKnob(RoundedCornerShape(14.dp))
        )
        Row(modifier = Modifier.fillMaxSize()) {
            titles.forEachIndexed { i, title ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (i == selected) selectedTint else scheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TopChartRow(rank: Int, entry: TopChartEntry, onPostClick: (Int) -> Unit) {
    val post = entry.post
    // پست برای باز شدن سریع صفحه‌ی جزئیات تو حافظه نگه داشته می‌شه (مثل PostCard)
    remember(post.id) { PostMemory.put(post) }
    val title = remember(post.id) { post.cleanTitle() }
    val image = remember(post.id) { try { post.featuredImageUrl() } catch (e: Exception) { null } }
    // مثل سایت: «★ 4.9 · دسته»
    val subtitle = remember(post.id, entry.rating) {
        listOfNotNull(
            entry.rating?.let { "★ " + String.format(Locale.US, "%.1f", it) },
            post.cardCategory()
        ).joinToString(" · ").ifBlank { "رایگان" }
    }
    AppRowItem(
        rank = rank,
        title = title,
        imageUrl = image,
        onClick = { onPostClick(post.id) },
        subtitle = subtitle
    )
}

@Composable
private fun TopChartRowSkeleton() {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).height(76.dp)) {
        ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 16)
    }
}
