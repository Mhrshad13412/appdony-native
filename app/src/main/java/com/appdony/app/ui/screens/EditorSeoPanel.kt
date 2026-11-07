package com.appdony.app.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.InlineFormat
import com.appdony.app.data.model.InlineSpan
import com.appdony.app.data.model.SpanKind
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.seo.SeoAi
import com.appdony.app.data.seo.SeoAnalyzer
import com.appdony.app.data.seo.SeoApply
import com.appdony.app.data.seo.SeoCheck
import com.appdony.app.data.seo.SeoContext
import com.appdony.app.data.seo.SeoField
import com.appdony.app.data.seo.SeoInput
import com.appdony.app.data.seo.SeoLevel
import com.appdony.app.data.seo.SeoProposal
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.util.EditorAiContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SeoRed = Color(0xFFE5484D)
private val SeoOrange = Color(0xFFF5A623)
private val SeoGreen = Color(0xFF30A46C)

// کلید دکمه‌ی «بهینه‌سازی کامل»؛ برای اینکه فقط همون دکمه حالت «در حال کار» نشون بده
private const val KEY_ALL = "all"

private fun levelColor(level: SeoLevel): Color = when (level) {
    SeoLevel.PROBLEM -> SeoRed
    SeoLevel.IMPROVE -> SeoOrange
    SeoLevel.GOOD -> SeoGreen
}

/** کپی غیرقابل‌تغییر از وضعیت ادیتور برای آنالیز */
internal fun seoInputOf(vm: EditorViewModel): SeoInput = SeoInput(
    title = vm.title,
    blocks = vm.blocks.toList(),
    keyword = vm.seo.focusKeyword,
    seoTitle = vm.seo.title,
    metaDescription = vm.seo.description,
    slug = vm.settings.slug,
    hasFeatured = vm.featuredId != null || vm.featuredUrl != null
)

// ───────────────────────── پل ادیتور ← چت هوش مصنوعی ─────────────────────────

/**
 * تا وقتی ادیتور بازه، خلاصه‌ی نوشته (عنوان، کلمه‌ی کلیدی، مشکلات سئو، ابتدای متن) رو
 * به چت هوش مصنوعی می‌ده؛ چت شناور و بیرون برنامه هم همین رو می‌بینن.
 */
@Composable
internal fun EditorAiBridge(vm: EditorViewModel, isBlog: Boolean) {
    LaunchedEffect(vm, isBlog) {
        snapshotFlow { seoInputOf(vm) }.collectLatest { input ->
            delay(700)
            val summary = withContext(Dispatchers.Default) {
                SeoContext.build(isBlog, input, SeoAnalyzer.analyze(input))
            }
            EditorAiContext.update(summary)
        }
    }
    DisposableEffect(Unit) {
        onDispose { EditorAiContext.pause() }
    }
}

// ───────────────────────── وضعیت پنل (با بستن پنل از بین نمی‌ره) ─────────────────────────

internal data class RelatedLink(val title: String, val url: String)

internal class SeoPanelModel : ViewModel() {
    var busy by mutableStateOf(false)
        private set
    // کدوم دکمه درخواست رو زده؛ فقط همون دکمه «صبر کن» نشون می‌ده، بقیه فقط تا آخر کار قفل می‌مونن
    var activeKey by mutableStateOf<String?>(null)
        private set
    var proposals by mutableStateOf<List<SeoProposal>>(emptyList())
        private set
    var picked by mutableStateOf<Set<Int>>(emptySet())
    var related by mutableStateOf<List<RelatedLink>>(emptyList())
        private set
    var relatedBusy by mutableStateOf(false)
        private set

    fun propose(context: Context, input: SeoInput, fields: Set<SeoField>, source: String? = null) {
        if (busy || relatedBusy) return
        busy = true
        activeKey = source
        proposals = emptyList()
        related = emptyList()
        val app = context.applicationContext
        viewModelScope.launch {
            val result = SeoAi.propose(app, input, fields)
            busy = false
            activeKey = null
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    AppBanner.show(BannerKind.WARNING, "پیشنهاد تازه‌ای نیومد؛ دوباره امتحان کن")
                } else {
                    proposals = list
                    picked = list.indices.toSet()
                }
            }.onFailure {
                AppBanner.show(BannerKind.ERROR, it.message ?: "هوش مصنوعی جواب نداد", 4500L)
            }
        }
    }

    fun clearProposals() {
        proposals = emptyList()
        picked = emptySet()
    }

    fun loadRelated(input: SeoInput, selfUrl: String?, source: String? = null) {
        if (relatedBusy || busy) return
        relatedBusy = true
        activeKey = source
        proposals = emptyList()
        viewModelScope.launch {
            val query = input.keyword.ifBlank { input.title }.trim()
            val found = if (query.isBlank()) emptyList() else fetchRelated(query, selfUrl)
            related = found
            relatedBusy = false
            activeKey = null
            if (found.isEmpty()) {
                AppBanner.show(BannerKind.WARNING, "مطلب مرتبطی پیدا نشد؛ اول کلمه‌ی کلیدی یا عنوان رو بنویس")
            }
        }
    }

    fun clearRelated() {
        related = emptyList()
    }

    private suspend fun fetchRelated(query: String, selfUrl: String?): List<RelatedLink> =
        try {
            ApiClient.api
                .searchPosts(query = query.take(60), perPage = 8, embed = "false", fields = "id,title,link")
                .mapNotNull { post ->
                    val link = post.link
                    if (link.isNullOrBlank() || link == selfUrl) null else RelatedLink(post.cleanTitle(), link)
                }
                .distinctBy { it.url }
                .take(5)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
}

// ───────────────────────── اعمال تغییرات روی ادیتور ─────────────────────────

private fun applyProposals(vm: EditorViewModel, list: List<SeoProposal>): Int {
    var count = 0
    for (p in list) {
        when (p.field) {
            SeoField.KEYWORD -> vm.seo = vm.seo.copy(focusKeyword = p.newValue)
            SeoField.TITLE -> vm.seo = vm.seo.copy(title = p.newValue)
            SeoField.META -> vm.seo = vm.seo.copy(description = p.newValue)
            SeoField.INTRO, SeoField.HEADING -> {
                val id = p.blockId ?: continue
                val block = vm.blocks.firstOrNull { it.id == id } ?: continue
                val text = if (p.apply == SeoApply.PREPEND) p.newValue + " " + block.text else p.newValue
                vm.updateText(id, text)
            }
            SeoField.ALT -> {
                val id = p.blockId ?: continue
                vm.updateExtra(id, p.newValue)
            }
        }
        count++
    }
    return count
}

/** یه جمله‌ی «بیشتر بخوانید» با لینک به مطلب مرتبط، آخر آخرین پاراگراف اضافه می‌شه */
private fun addInternalLink(vm: EditorViewModel, link: RelatedLink) {
    var target = vm.blocks.lastOrNull { it.type == BlockType.PARAGRAPH }
    if (target == null) {
        vm.insert(BlockType.PARAGRAPH)
        target = vm.blocks.firstOrNull { it.id == vm.focusedId }
    }
    val block = target ?: return
    val index = vm.blocks.indexOfFirst { it.id == block.id }
    if (index < 0) return

    val label = "بیشتر بخوانید: "
    val prefix = if (block.text.isBlank()) "" else block.text.trimEnd() + " "
    val newText = prefix + label + link.title
    val start = prefix.length + label.length
    val clean = InlineFormat.clip(block.spans, newText.length)
    vm.blocks[index] = block.copy(
        text = newText,
        spans = InlineFormat.setLink(clean, start, newText.length, link.url)
    )
    AppBanner.show(BannerKind.SUCCESS, "لینک داخلی اضافه شد")
}

// ───────────────────────── پنل سئو ─────────────────────────

@Composable
internal fun SeoPanel(vm: EditorViewModel) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val model: SeoPanelModel = viewModel(key = "seo_panel_${System.identityHashCode(vm)}")

    val input = seoInputOf(vm)
    val checks = remember(input) { SeoAnalyzer.analyze(input) }
    val problems = checks.filter { it.level == SeoLevel.PROBLEM }
    val improves = checks.filter { it.level == SeoLevel.IMPROVE }
    val goods = checks.filter { it.level == SeoLevel.GOOD }

    var openProblems by remember { mutableStateOf(true) }
    var openImproves by remember { mutableStateOf(true) }
    var openGood by remember { mutableStateOf(false) }

    val s = vm.seo

    fun optimizeAll() {
        val fields = checks.filter { it.level != SeoLevel.GOOD }.mapNotNull { it.aiField }.toSet()
        if (fields.isEmpty()) {
            AppBanner.show(BannerKind.SUCCESS, "چیزی برای اصلاح با هوش مصنوعی نمونده 👌")
        } else {
            model.propose(ctx, input, fields, KEY_ALL)
        }
    }

    AiButton(
        label = if (model.activeKey == KEY_ALL) "هوش مصنوعی داره سئو رو بررسی می‌کنه…" else "بهینه‌سازی کامل سئو با هوش مصنوعی",
        busy = model.activeKey == KEY_ALL,
        enabled = !model.busy && !model.relatedBusy,
        onClick = { optimizeAll() }
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PChip("✨ کلمه‌ی کلیدی", false) { model.propose(ctx, input, setOf(SeoField.KEYWORD), "chip:keyword") }
        PChip("✨ عنوان سئو", false) { model.propose(ctx, input, setOf(SeoField.TITLE), "chip:title") }
        PChip("✨ توضیحات متا", false) { model.propose(ctx, input, setOf(SeoField.META), "chip:meta") }
    }

    // پیشنهادهای هوش مصنوعی (با تیک؛ تا تأیید نکنی اعمال نمی‌شه)
    if (model.proposals.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("پیشنهادهای هوش مصنوعی", color = scheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            model.proposals.forEachIndexed { i, p ->
                ProposalRow(p, checked = i in model.picked) {
                    model.picked = if (i in model.picked) model.picked - i else model.picked + i
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PChip("اعمال انتخاب‌شده‌ها", true) {
                    val chosen = model.proposals.filterIndexed { i, _ -> i in model.picked }
                    val n = applyProposals(vm, chosen)
                    model.clearProposals()
                    if (n > 0) AppBanner.show(BannerKind.SUCCESS, "$n مورد اعمال شد؛ فراموش نکن ذخیره کنی")
                }
                PChip("رد کردن", false) { model.clearProposals() }
            }
        }
    }

    // پیشنهاد لینک داخلی
    if (model.relatedBusy) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text("دارم مطلب مرتبط پیدا می‌کنم…", color = scheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
    if (model.related.isNotEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.primary.copy(alpha = 0.08f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Link, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("مطالب مرتبط اپدونی (بزن تا لینکش اضافه بشه)", color = scheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            model.related.forEach { link ->
                Text(
                    text = link.title,
                    color = scheme.onSurface,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { addInternalLink(vm, link) }
                        .background(scheme.onSurface.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            PChip("بستن", false) { model.clearRelated() }
        }
    }

    // فیلدها
    PField("کلمه‌ی کلیدی کانونی", s.focusKeyword, { vm.seo = vm.seo.copy(focusKeyword = it) })
    PField("عنوان سئو", s.title, { vm.seo = vm.seo.copy(title = it) })
    Text(
        "${s.title.length} / 60 نویسه",
        color = lengthColor(s.title.length, 30, 60),
        fontSize = 11.sp
    )
    PField("توضیحات متا", s.description, { vm.seo = vm.seo.copy(description = it) }, lines = 3)
    Text(
        "${s.description.length} / 160 نویسه",
        color = lengthColor(s.description.length, 120, 160),
        fontSize = 11.sp
    )

    // پیش‌نمایش گوگل
    val firstPara = vm.blocks.firstOrNull { it.type == BlockType.PARAGRAPH && it.text.isNotBlank() }?.text.orEmpty()
    SerpPreview(
        title = s.title.ifBlank { vm.title.ifBlank { "عنوان نوشته" } },
        url = if (vm.settings.slug.isBlank()) SeoAnalyzer.SITE_HOST else "${SeoAnalyzer.SITE_HOST} › ${vm.settings.slug}",
        desc = s.description.ifBlank { firstPara.take(155) }
    )

    // خلاصه‌ی نتیجه
    SummaryRow(problems.size, improves.size, goods.size)

    // گروه‌ها
    if (problems.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupHeader("مشکلات", problems.size, SeoRed, openProblems) { openProblems = !openProblems }
            if (openProblems) problems.forEach { c ->
                CheckRow(
                    check = c,
                    busy = model.busy || model.relatedBusy,
                    activeKey = model.activeKey,
                    onFix = { field, key -> model.propose(ctx, input, setOf(field), key) },
                    onLinks = { key -> model.loadRelated(input, vm.link, key) }
                )
            }
        }
    }
    if (improves.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupHeader("بهبودها", improves.size, SeoOrange, openImproves) { openImproves = !openImproves }
            if (openImproves) improves.forEach { c ->
                CheckRow(
                    check = c,
                    busy = model.busy || model.relatedBusy,
                    activeKey = model.activeKey,
                    onFix = { field, key -> model.propose(ctx, input, setOf(field), key) },
                    onLinks = { key -> model.loadRelated(input, vm.link, key) }
                )
            }
        }
    }
    if (goods.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            GroupHeader("نتایج خوب", goods.size, SeoGreen, openGood) { openGood = !openGood }
            if (openGood) goods.forEach { c ->
                CheckRow(check = c, busy = true, activeKey = null, onFix = { _, _ -> }, onLinks = {})
            }
        }
    }
}

private fun lengthColor(len: Int, min: Int, max: Int): Color = when {
    len == 0 -> Color.Unspecified
    len in min..max -> SeoGreen
    else -> SeoOrange
}

// ───────────────────────── اجزای ظاهری ─────────────────────────

@Composable
private fun AiButton(label: String, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.primary.copy(alpha = if (enabled) 0.20f else 0.10f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, color = scheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProposalRow(p: SeoProposal, checked: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (checked) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(p.label, color = scheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (p.oldValue.isNotBlank()) {
                Text(
                    text = p.oldValue,
                    color = scheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.LineThrough,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(p.newValue, color = scheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
            if (p.field == SeoField.TITLE || p.field == SeoField.META) {
                val max = if (p.field == SeoField.TITLE) 60 else 160
                Text("${p.newValue.length} / $max نویسه", color = scheme.onSurfaceVariant, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SerpPreview(title: String, url: String, desc: String) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text("پیش‌نمایش گوگل", color = scheme.onSurfaceVariant, fontSize = 10.sp)
        Text(
            text = url,
            color = SeoGreen,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(textDirection = TextDirection.Ltr)
        )
        Text(
            text = title,
            color = scheme.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (desc.isNotBlank()) {
            Text(
                text = desc,
                color = scheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SummaryRow(problems: Int, improves: Int, goods: Int) {
    val scheme = MaterialTheme.colorScheme
    val (label, color) = when {
        problems == 0 && improves == 0 -> "سئو عالیه 👌" to SeoGreen
        problems == 0 -> "سئو قابل‌قبوله" to SeoOrange
        else -> "سئو نیاز به کار داره" to SeoRed
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "$problems مشکل · $improves بهبود · $goods خوب",
            color = scheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun GroupHeader(title: String, count: Int, color: Color, open: Boolean, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$title ($count)",
            color = scheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun CheckRow(
    check: SeoCheck,
    busy: Boolean,
    activeKey: String?,
    onFix: (SeoField, String) -> Unit,
    onLinks: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    // هر دکمه کلید خودش رو داره تا موقع کار فقط همون دکمه «صبر کن» بشه، نه همه‌ی دکمه‌های «درستش کن»
    val fixKey = "${check.title}:fix"
    val linksKey = "${check.title}:links"
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(levelColor(check.level))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(check.title, color = scheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(check.message, color = scheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 18.sp)
            val field = check.aiField
            if (check.level != SeoLevel.GOOD && (field != null || check.suggestLinks)) {
                Spacer(Modifier.padding(top = 2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (field != null) {
                        PChip(if (activeKey == fixKey) "⏳ صبر کن…" else "✨ درستش کن", !busy) { if (!busy) onFix(field, fixKey) }
                    }
                    if (check.suggestLinks) {
                        PChip(if (activeKey == linksKey) "⏳ صبر کن…" else "🔗 پیشنهاد لینک", !busy) { if (!busy) onLinks(linksKey) }
                    }
                }
            }
        }
    }
}
