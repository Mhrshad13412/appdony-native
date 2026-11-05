package com.appdony.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.api.EditorApi
import com.appdony.app.data.model.BlockEditing
import com.appdony.app.data.model.BlockGroup
import com.appdony.app.data.model.BlockSerializer
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.BlockUsage
import com.appdony.app.data.model.EditorBlock
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.ui.components.FloatingDialog
import com.appdony.app.ui.components.glassKnob
import com.appdony.app.ui.components.glassSelectedIconTint
import com.appdony.app.ui.components.glassTrack
import kotlinx.coroutines.launch

// ───────────────────────── وضعیت ادیتور (با چرخش صفحه از بین نمی‌ره) ─────────────────────────

class EditorViewModel : ViewModel() {
    val blocks = mutableStateListOf<EditorBlock>()
    var title by mutableStateOf("")
    var focusedId by mutableStateOf<Long?>(null)
    var pendingFocusId by mutableStateOf<Long?>(null)
    var scrollToId by mutableStateOf<Long?>(null)
    private var nextId = 1L

    // ذخیره روی سایت: بعد از اولین ذخیره شناسه و لینک مطلب مشخص می‌شه و ذخیره‌های بعدی همون مطلب رو به‌روز می‌کنن
    var postId by mutableStateOf<Long?>(null)
    var link by mutableStateOf<String?>(null)
    var status by mutableStateOf("draft")
    var saving by mutableStateOf(false)

    // تصویر شاخص
    var featuredId by mutableStateOf<Long?>(null)
    var featuredUrl by mutableStateOf<String?>(null)
    var uploading by mutableStateOf(false)

    private var cleanSnapshot = ""

    init {
        val first = EditorBlock(nextId++, BlockType.PARAGRAPH)
        blocks.add(first)
        focusedId = first.id
        cleanSnapshot = snapshot()
    }

    private fun snapshot(): String = buildString {
        append(title).append('\u0001').append(featuredId ?: 0L)
        blocks.forEach {
            append('\u0001').append(it.type.name).append('|').append(it.text).append('|').append(it.extra)
        }
    }

    /** آیا چیزی نوشته شده که هنوز ذخیره نشده؟ */
    val isDirty: Boolean
        get() = snapshot() != cleanSnapshot

    /** بلوک تازه بعد از بلوک انتخاب‌شده می‌شینه؛ اگه اون یه پاراگراف خالی بود، جاش عوض می‌شه (مثل وردپرس) */
    fun insert(type: BlockType) {
        val block = EditorBlock(
            id = nextId++,
            type = type,
            extra = if (type == BlockType.HEADING) "2" else ""
        )
        val at = blocks.indexOfFirst { it.id == focusedId }
        if (at >= 0 && blocks[at].type == BlockType.PARAGRAPH && blocks[at].text.isEmpty()) {
            blocks[at] = block
        } else if (at >= 0) {
            blocks.add(at + 1, block)
        } else {
            blocks.add(block)
        }
        focusedId = block.id
        if (type.editing != BlockEditing.PLACEHOLDER && type.editing != BlockEditing.DIVIDER) {
            pendingFocusId = block.id
        }
        scrollToId = block.id
    }

    fun updateText(id: Long, text: String) {
        val i = blocks.indexOfFirst { it.id == id }
        if (i >= 0 && blocks[i].text != text) blocks[i] = blocks[i].copy(text = text)
    }

    fun updateExtra(id: Long, extra: String) {
        val i = blocks.indexOfFirst { it.id == id }
        if (i >= 0 && blocks[i].extra != extra) blocks[i] = blocks[i].copy(extra = extra)
    }

    /** اینتر توی پاراگراف/سرنویس: بقیه‌ی متن می‌ره توی یه پاراگراف تازه (مثل وردپرس) */
    fun splitAtNewline(id: Long, raw: String) {
        val i = blocks.indexOfFirst { it.id == id }
        if (i < 0) return
        val cut = raw.indexOf('\n')
        if (cut < 0) {
            updateText(id, raw)
            return
        }
        val before = raw.substring(0, cut)
        val after = raw.substring(cut + 1).replace("\n", " ")
        blocks[i] = blocks[i].copy(text = before)
        val next = EditorBlock(nextId++, BlockType.PARAGRAPH, text = after)
        blocks.add(i + 1, next)
        focusedId = next.id
        pendingFocusId = next.id
        scrollToId = next.id
    }

    fun move(id: Long, delta: Int) {
        val i = blocks.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j >= blocks.size) return
        val item = blocks.removeAt(i)
        blocks.add(j, item)
        scrollToId = id
    }

    fun remove(id: Long) {
        val i = blocks.indexOfFirst { it.id == id }
        if (i < 0) return
        if (blocks.size == 1) {
            val fresh = EditorBlock(nextId++, BlockType.PARAGRAPH)
            blocks[0] = fresh
            focusedId = fresh.id
            return
        }
        blocks.removeAt(i)
        focusedId = blocks.getOrNull((i - 1).coerceAtLeast(0))?.id
    }

    /** بک‌اسپیس روی بلوک خالی: بلوک پاک می‌شه و فوکوس می‌ره بلوک قبلی */
    fun removeEmptyAndFocusPrevious(id: Long) {
        if (blocks.size <= 1) return
        val i = blocks.indexOfFirst { it.id == id }
        if (i < 0) return
        blocks.removeAt(i)
        val neighbour = blocks.getOrNull((i - 1).coerceAtLeast(0))
        focusedId = neighbour?.id
        pendingFocusId = neighbour?.id
    }

    fun clearAll() {
        blocks.clear()
        val fresh = EditorBlock(nextId++, BlockType.PARAGRAPH)
        blocks.add(fresh)
        focusedId = fresh.id
    }

    fun toPlainText(): String {
        val body = blocks.filter { it.text.isNotBlank() }.joinToString("\n\n") { it.text }
        return if (title.isBlank()) body else "$title\n\n$body"
    }

    // ───────────── ذخیره‌ی واقعی روی سایت ─────────────

    /** [wanted]: draft یا publish. اگه اجازه‌ی انتشار نباشه، سرور «در انتظار بازبینی» ذخیره می‌کنه. */
    fun save(context: Context, kind: String, wanted: String) {
        if (saving) return
        if (uploading) {
            AppBanner.show(BannerKind.WARNING, "صبر کن تا آپلود تصویر تموم بشه")
            return
        }
        if (title.isBlank()) {
            AppBanner.show(BannerKind.WARNING, "اول یه عنوان بنویس")
            return
        }
        val serialized = BlockSerializer.serialize(blocks.toList())
        val snap = snapshot()
        val app = context.applicationContext
        saving = true
        viewModelScope.launch {
            val result = EditorApi.save(app, kind, postId, title.trim(), serialized.html, wanted, featuredId)
            saving = false
            result.onSuccess { saved ->
                postId = saved.id
                link = saved.link.ifBlank { null }
                status = saved.status
                cleanSnapshot = snap
                val done = when (saved.status) {
                    "publish" -> "منتشر شد"
                    "pending" -> "برای بازبینی ذخیره شد"
                    else -> "پیش‌نویس ذخیره شد"
                }
                val skipped = serialized.skipped.distinct()
                when {
                    skipped.isNotEmpty() -> AppBanner.show(
                        BannerKind.WARNING,
                        "$done؛ بلوک‌های ${skipped.joinToString("، ")} هنوز ذخیره نمی‌شن",
                        5200L
                    )
                    saved.note != null -> AppBanner.show(BannerKind.WARNING, saved.note, 4500L)
                    else -> AppBanner.show(BannerKind.SUCCESS, done)
                }
            }.onFailure {
                AppBanner.show(BannerKind.ERROR, it.message ?: "ذخیره نشد", 4500L)
            }
        }
    }

    // ───────────── تصویر شاخص ─────────────

    fun uploadFeatured(context: Context, uri: Uri) {
        if (uploading) return
        val app = context.applicationContext
        val previousId = featuredId
        val previousUrl = featuredUrl
        featuredId = null
        featuredUrl = uri.toString() // پیش‌نمایش فوری از روی خودِ فایل
        uploading = true
        viewModelScope.launch {
            val result = EditorApi.uploadImage(app, uri)
            result.onSuccess {
                featuredId = it.id
                featuredUrl = it.url.ifBlank { uri.toString() }
            }.onFailure {
                featuredId = previousId
                featuredUrl = previousUrl
                AppBanner.show(BannerKind.ERROR, it.message ?: "آپلود نشد", 4500L)
            }
            uploading = false
        }
    }

    fun clearFeatured() {
        featuredId = null
        featuredUrl = null
    }
}

// ───────────────────────── صفحه‌ی ادیتور ─────────────────────────

/**
 * ایجاد نوشته / ایجاد مطلب وبلاگ. [kind] = "post" یا "blog".
 * بالا: نوار آیکن‌ها + کشوی «همه‌ی بلوک‌ها»؛ پایین: کشوی «۶ بلوک پرکاربرد».
 */
@Composable
fun EditorScreen(kind: String, onBack: () -> Unit) {
    val isBlog = kind == "blog"
    val ctx = LocalContext.current
    val vm: EditorViewModel = viewModel(key = "editor_$kind")
    val scheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    var topOpen by rememberSaveable { mutableStateOf(false) }
    var bottomOpen by rememberSaveable { mutableStateOf(true) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var showOutline by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val quickBlocks = remember { BlockUsage.topSix(ctx) }
    val listState = rememberLazyListState()

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.uploadFeatured(ctx, uri)
    }

    val requestClose: () -> Unit = { if (vm.isDirty) confirmExit = true else onBack() }
    val insertBlock: (BlockType) -> Unit = { type ->
        BlockUsage.record(ctx, type)
        vm.insert(type)
        topOpen = false
    }

    BackHandler {
        when {
            preview -> preview = false
            topOpen -> topOpen = false
            else -> requestClose()
        }
    }

    // بلوک تازه یا جابه‌جاشده: صفحه تا اونجا اسکرول می‌شه (ردیف اول لیست عنوانه و دومی تصویر شاخص)
    val scrollTarget = vm.scrollToId
    LaunchedEffect(scrollTarget) {
        if (scrollTarget != null) {
            val index = vm.blocks.indexOfFirst { it.id == scrollTarget }
            if (index >= 0) listState.animateScrollToItem(index + 2)
            vm.scrollToId = null
        }
    }

    val saved = vm.postId != null
    val statusLabel = when (vm.status) {
        "publish" -> "منتشرشده"
        "pending" -> "در انتظار بازبینی"
        else -> "پیش‌نویس"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .imePadding()
    ) {
        EditorTopBar(
            title = when {
                saved && isBlog -> "ویرایش مطلب وبلاگ"
                saved -> "ویرایش نوشته"
                isBlog -> "مطلب وبلاگ جدید"
                else -> "نوشته‌ی جدید"
            },
            subtitle = when {
                preview -> "حالت پیش‌نمایش"
                saved -> "${vm.blocks.size} بلوک · $statusLabel"
                else -> "${vm.blocks.size} بلوک"
            },
            preview = preview,
            saving = vm.saving,
            siteReady = vm.link != null,
            showMenu = showMenu,
            onMenuChange = { showMenu = it },
            onClose = requestClose,
            onOutline = { showOutline = true },
            onOpenSite = {
                val link = vm.link
                if (link != null) {
                    if (vm.status != "publish") {
                        AppBanner.show(BannerKind.WARNING, "هنوز منتشر نشده؛ فقط وقتی توی مرورگر وارد حساب باشی دیده می‌شه")
                    }
                    runCatching {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            },
            onTogglePreview = {
                preview = !preview
                if (preview) topOpen = false
            },
            onSave = { showSave = true },
            onSettings = { AppBanner.show(BannerKind.WARNING, "تنظیمات نوشته توی مرحله‌های بعد اضافه می‌شه") },
            onCopyAll = {
                clipboard.setText(AnnotatedString(vm.toPlainText()))
                AppBanner.show(BannerKind.SUCCESS, "متن کپی شد")
            },
            onClear = { vm.clearAll() }
        )

        // کشوی بالا: همه‌ی بلوک‌ها
        AnimatedVisibility(
            visible = topOpen && !preview,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AllBlocksPanel(onPick = insertBlock)
        }
        if (!preview) {
            DrawerHandle(open = topOpen, onTop = true, onClick = { topOpen = !topOpen })
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(key = "title") {
                TitleField(
                    value = vm.title,
                    hint = if (isBlog) "عنوان مطلب وبلاگ…" else "عنوان نوشته…",
                    readOnly = preview,
                    onChange = { vm.title = it.replace("\n", " ") }
                )
            }
            item(key = "featured") {
                FeaturedImageCard(vm = vm, preview = preview, onPick = { pickImage.launch("image/*") })
            }
            itemsIndexed(vm.blocks, key = { _, block -> block.id }) { index, block ->
                BlockCard(block = block, index = index, count = vm.blocks.size, vm = vm, preview = preview)
            }
        }

        // کشوی پایین: ۶ بلوک پرکاربرد
        if (!preview) {
            QuickBar(
                blocks = quickBlocks,
                open = bottomOpen,
                onToggle = { bottomOpen = !bottomOpen },
                onPick = insertBlock
            )
        }
    }

    if (showOutline) {
        FloatingDialog(onDismiss = { showOutline = false }) {
            Text(
                text = "فهرست بلوک‌ها",
                color = scheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            vm.blocks.forEachIndexed { index, block ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            showOutline = false
                            vm.focusedId = block.id
                            vm.scrollToId = block.id
                        }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(block.type.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${index + 1}. ${block.type.label}", color = scheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (block.text.isNotBlank()) {
                            Text(
                                text = block.text.replace("\n", " ").take(48),
                                color = scheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSave) {
        FloatingDialog(onDismiss = { showSave = false }) {
            Text(
                text = if (isBlog) "ذخیره‌ی مطلب وبلاگ" else "ذخیره‌ی نوشته",
                color = scheme.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (saved) "وضعیت فعلی روی سایت: $statusLabel" else "هنوز روی سایت ذخیره نشده",
                color = scheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            if (vm.status == "publish") {
                SaveOption(
                    label = "به‌روزرسانی مطلب منتشرشده",
                    hint = "تغییرات همین الان روی سایت دیده می‌شه",
                    primary = true
                ) {
                    showSave = false
                    vm.save(ctx, kind, "publish")
                }
                Spacer(Modifier.height(8.dp))
                SaveOption(
                    label = "برگرداندن به پیش‌نویس",
                    hint = "از سایت برداشته می‌شه",
                    primary = false
                ) {
                    showSave = false
                    vm.save(ctx, kind, "draft")
                }
            } else {
                SaveOption(
                    label = "انتشار",
                    hint = "اگه اجازه‌ی انتشار نداشته باشی، برای بازبینی ذخیره می‌شه",
                    primary = true
                ) {
                    showSave = false
                    vm.save(ctx, kind, "publish")
                }
                Spacer(Modifier.height(8.dp))
                SaveOption(
                    label = "ذخیره به‌عنوان پیش‌نویس",
                    hint = "فقط خودت و مدیران می‌بینن",
                    primary = false
                ) {
                    showSave = false
                    vm.save(ctx, kind, "draft")
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = { showSave = false }, modifier = Modifier.fillMaxWidth()) {
                Text("انصراف")
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("خروج از ادیتور؟") },
            text = { Text("تغییرات ذخیره‌نشده از بین می‌رن.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmExit = false
                    onBack()
                }) { Text("خروج") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("ادامه‌ی نوشتن") }
            }
        )
    }
}

@Composable
private fun SaveOption(label: String, hint: String, primary: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (primary) scheme.primary.copy(alpha = 0.18f) else scheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = if (primary) scheme.primary else scheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = hint, color = scheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

// ───────────────────────── نوار آیکن‌های بالا ─────────────────────────

/**
 * راست: بستن + عنوان. چپ: فهرست بلوک‌ها، باز کردن سایت (بعد از ذخیره فعال می‌شه)، پیش‌نمایش،
 * دکمه‌ی ذخیره/ساخت و منوی سه‌نقطه.
 */
@Composable
private fun EditorTopBar(
    title: String,
    subtitle: String,
    preview: Boolean,
    saving: Boolean,
    siteReady: Boolean,
    showMenu: Boolean,
    onMenuChange: (Boolean) -> Unit,
    onClose: () -> Unit,
    onOutline: () -> Unit,
    onOpenSite: () -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    onSettings: () -> Unit,
    onCopyAll: () -> Unit,
    onClear: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BarIcon(Icons.Outlined.Close, "بستن", onClick = onClose)
        Column(modifier = Modifier
            .weight(1f)
            .padding(horizontal = 6.dp)) {
            Text(
                text = title,
                color = scheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = subtitle, color = scheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
        }
        BarIcon(Icons.Outlined.FormatListBulleted, "فهرست بلوک‌ها", onClick = onOutline)
        // لینک نوشته بعد از اولین ذخیره ساخته می‌شه
        BarIcon(Icons.Outlined.OpenInNew, "باز کردن در سایت", enabled = siteReady, onClick = onOpenSite)
        BarIcon(
            icon = if (preview) Icons.Outlined.Edit else Icons.Outlined.Visibility,
            desc = if (preview) "برگشت به ویرایش" else "پیش‌نمایش",
            onClick = onTogglePreview
        )
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .glassKnob(CircleShape)
                .clickable(enabled = !saving, onClick = onSave),
            contentAlignment = Alignment.Center
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = glassSelectedIconTint()
                )
            } else {
                Icon(Icons.Outlined.Save, contentDescription = "ذخیره", tint = glassSelectedIconTint(), modifier = Modifier.size(20.dp))
            }
        }
        Box {
            BarIcon(Icons.Outlined.MoreVert, "گزینه‌ها", onClick = { onMenuChange(true) })
            DropdownMenu(expanded = showMenu, onDismissRequest = { onMenuChange(false) }) {
                DropdownMenuItem(
                    text = { Text("تنظیمات نوشته") },
                    leadingIcon = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    onClick = {
                        onMenuChange(false)
                        onSettings()
                    }
                )
                DropdownMenuItem(
                    text = { Text("کپی همه‌ی متن") },
                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                    onClick = {
                        onMenuChange(false)
                        onCopyAll()
                    }
                )
                DropdownMenuItem(
                    text = { Text("پاک‌کردن همه‌ی بلوک‌ها") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                    onClick = {
                        onMenuChange(false)
                        onClear()
                    }
                )
            }
        }
    }
}

@Composable
private fun BarIcon(
    icon: ImageVector,
    desc: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) tint else tint.copy(alpha = 0.32f),
            modifier = Modifier.size(22.dp)
        )
    }
}

// ───────────────────────── کشوهای بلوک ─────────────────────────

/** دسته‌ی کوچیکِ باز/بسته کردن کشو (مثل فلش وسط ادیتور تلگرام) */
@Composable
private fun DrawerHandle(open: Boolean, onTop: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val icon = if (onTop == open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(24.dp)
                .glassTrack(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = if (open) "بستن کشو" else "باز کردن کشو",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** کشوی بالا: همه‌ی بلوک‌ها به‌صورت دسته‌بندی‌شده */
@Composable
private fun AllBlocksPanel(onPick: (BlockType) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .glassTrack(RoundedCornerShape(20.dp))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(78.dp),
            modifier = Modifier.heightIn(max = 300.dp),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            BlockGroup.entries.forEach { group ->
                item(key = "group_${group.name}", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = group.label,
                        color = scheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                items(BlockType.entries.filter { it.group == group }, key = { it.name }) { type ->
                    BlockTile(type = type, onClick = { onPick(type) })
                }
            }
        }
    }
}

@Composable
private fun BlockTile(type: BlockType, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(type.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = type.label,
            color = scheme.onSurface,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** کشوی پایین: ۶ بلوک پرکاربرد (هرچی بیشتر استفاده کنی، جلوتر میاد) */
@Composable
private fun QuickBar(
    blocks: List<BlockType>,
    open: Boolean,
    onToggle: () -> Unit,
    onPick: (BlockType) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
    ) {
        DrawerHandle(open = open, onTop = false, onClick = onToggle)
        AnimatedVisibility(
            visible = open,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .glassTrack(RoundedCornerShape(22.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                blocks.forEach { type ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(type) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(type.icon, contentDescription = type.label, tint = scheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = type.label,
                            color = scheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── عنوان، تصویر شاخص و کارت بلوک‌ها ─────────────────────────

@Composable
private fun TitleField(value: String, hint: String, readOnly: Boolean, onChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.titleLarge.copy(
        color = scheme.onSurface,
        fontSize = 24.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
        textDirection = TextDirection.Content
    )
    BasicTextField(
        value = value,
        onValueChange = onChange,
        readOnly = readOnly,
        textStyle = style,
        cursorBrush = SolidColor(scheme.primary),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(text = hint, style = style.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.6f)))
                }
                inner()
            }
        }
    )
}

/** تصویر شاخص: انتخاب از گالری گوشی، آپلود روی سایت و اتصال به مطلب موقع ذخیره */
@Composable
private fun FeaturedImageCard(vm: EditorViewModel, preview: Boolean, onPick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val url = vm.featuredUrl
    if (preview && url == null) return
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, scheme.outline.copy(alpha = 0.5f), shape)
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = "تصویر شاخص",
                color = scheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (!preview && url != null && !vm.uploading) {
                MiniButton(Icons.Outlined.Edit, "تغییر تصویر", onClick = onPick)
                MiniButton(Icons.Outlined.Delete, "حذف تصویر", tint = scheme.error) { vm.clearFeatured() }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .clickable(enabled = !preview && url == null && !vm.uploading, onClick = onPick),
            contentAlignment = Alignment.Center
        ) {
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = "تصویر شاخص",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(text = "انتخاب تصویر شاخص", color = scheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "از گالری گوشی", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
            if (vm.uploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = scheme.primary)
                }
            }
        }
    }
}

@Composable
private fun BlockCard(block: EditorBlock, index: Int, count: Int, vm: EditorViewModel, preview: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val focused = !preview && vm.focusedId == block.id
    val shape = RoundedCornerShape(16.dp)
    val cardModifier = if (preview) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (focused) scheme.primary.copy(alpha = 0.06f) else Color.Transparent)
            .border(
                1.dp,
                if (focused) scheme.primary.copy(alpha = 0.55f) else scheme.outline.copy(alpha = 0.5f),
                shape
            )
            .clickable { vm.focusedId = block.id }
            .padding(12.dp)
    }
    Column(modifier = cardModifier) {
        if (focused) BlockHeader(block = block, index = index, count = count, vm = vm)
        BlockBody(block = block, vm = vm, preview = preview, focused = focused)
    }
}

@Composable
private fun BlockHeader(block: EditorBlock, index: Int, count: Int, vm: EditorViewModel) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(block.type.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = block.type.label,
            color = scheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        MiniButton(Icons.Outlined.ArrowUpward, "بالا", enabled = index > 0) { vm.move(block.id, -1) }
        MiniButton(Icons.Outlined.ArrowDownward, "پایین", enabled = index < count - 1) { vm.move(block.id, 1) }
        MiniButton(Icons.Outlined.Delete, "حذف", tint = scheme.error) { vm.remove(block.id) }
    }
}

@Composable
private fun MiniButton(
    icon: ImageVector,
    desc: String,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) tint else tint.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun LevelChips(current: String, onPick: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("2", "3", "4").forEach { level ->
            val selected = current.ifEmpty { "2" } == level
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) scheme.primary.copy(alpha = 0.22f) else scheme.onSurface.copy(alpha = 0.06f))
                    .clickable { onPick(level) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "H$level",
                    color = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun fieldStyle(block: EditorBlock): TextStyle {
    val scheme = MaterialTheme.colorScheme
    val base = MaterialTheme.typography.bodyLarge.copy(
        color = scheme.onSurface,
        fontSize = 15.sp,
        lineHeight = 27.sp,
        textDirection = TextDirection.Content
    )
    return when (block.type.editing) {
        BlockEditing.HEADING -> {
            val size = when (block.extra) {
                "3" -> 20.sp
                "4" -> 17.sp
                else -> 24.sp
            }
            base.copy(fontSize = size, lineHeight = size * 1.45f, fontWeight = FontWeight.Bold)
        }
        BlockEditing.QUOTE -> base.copy(fontStyle = FontStyle.Italic)
        BlockEditing.CODE -> base.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textDirection = TextDirection.Ltr
        )
        BlockEditing.URL -> base.copy(fontSize = 14.sp, lineHeight = 22.sp, textDirection = TextDirection.Ltr)
        else -> base
    }
}

private fun hintOf(type: BlockType): String = when (type) {
    BlockType.PARAGRAPH -> "بنویس یا از کشو یه بلوک تازه اضافه کن…"
    BlockType.HEADING -> "سرنویس…"
    BlockType.LIST, BlockType.ORDERED_LIST -> "هر خط یک مورد…"
    BlockType.QUOTE, BlockType.PULLQUOTE -> "نقل‌قول…"
    BlockType.VERSE -> "شعر…"
    BlockType.HTML -> "<div>…</div>"
    BlockType.SHORTCODE -> "[shortcode]"
    BlockType.CODE, BlockType.PREFORMATTED -> "کد…"
    else -> "https://…"
}

/** یک فیلد متنیِ بلوک؛ فوکوس، بک‌اسپیسِ بلوک خالی و راهنمای خالی بودن رو مدیریت می‌کنه */
@Composable
private fun BlockField(
    vm: EditorViewModel,
    block: EditorBlock,
    value: String,
    hint: String,
    style: TextStyle,
    readOnly: Boolean,
    modifier: Modifier = Modifier,
    main: Boolean = true,
    singleLine: Boolean = false,
    hintColor: Color = Color.Unspecified,
    onChange: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val requester = remember { FocusRequester() }
    if (main) {
        val pending = vm.pendingFocusId
        LaunchedEffect(pending) {
            if (pending == block.id) {
                withFrameNanos { }
                runCatching { requester.requestFocus() }
                vm.pendingFocusId = null
            }
        }
    }
    val placeholderColor = if (hintColor == Color.Unspecified) scheme.onSurfaceVariant.copy(alpha = 0.6f) else hintColor
    BasicTextField(
        value = value,
        onValueChange = onChange,
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(scheme.primary),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) vm.focusedId = block.id }
            .onPreviewKeyEvent { event ->
                if (main && event.type == KeyEventType.KeyDown && event.key == Key.Backspace && value.isEmpty()) {
                    vm.removeEmptyAndFocusPrevious(block.id)
                    true
                } else {
                    false
                }
            },
        decorationBox = { inner ->
            Box(modifier = Modifier.fillMaxWidth()) {
                if (value.isEmpty()) {
                    Text(text = hint, style = style.copy(color = placeholderColor))
                }
                inner()
            }
        }
    )
}

@Composable
private fun BlockBody(block: EditorBlock, vm: EditorViewModel, preview: Boolean, focused: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val style = fieldStyle(block)
    val type = block.type

    when (type.editing) {
        BlockEditing.PARAGRAPH, BlockEditing.HEADING -> {
            if (type.editing == BlockEditing.HEADING && focused) {
                LevelChips(current = block.extra) { vm.updateExtra(block.id, it) }
            }
            // اینتر توی پاراگراف و سرنویس یه پاراگراف تازه می‌سازه؛ شعر خط جدید می‌گیره
            val splitsOnEnter = type == BlockType.PARAGRAPH || type == BlockType.HEADING
            BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview) { new ->
                if (splitsOnEnter && new.contains('\n')) vm.splitAtNewline(block.id, new) else vm.updateText(block.id, new)
            }
        }

        BlockEditing.LIST -> {
            if (preview) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    block.text.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
                        Text(text = (if (type.ordered) "${i + 1}. " else "• ") + line, style = style)
                    }
                }
            } else {
                BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = false) {
                    vm.updateText(block.id, it)
                }
            }
        }

        BlockEditing.QUOTE -> {
            val barColor = scheme.primary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val barWidth = 3.dp.toPx()
                        val x = if (layoutDirection == LayoutDirection.Rtl) size.width - barWidth else 0f
                        drawRect(color = barColor, topLeft = Offset(x, 0f), size = Size(barWidth, size.height))
                    }
                    .padding(horizontal = 14.dp)
            ) {
                BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview) {
                    vm.updateText(block.id, it)
                }
            }
        }

        BlockEditing.CODE -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(scheme.onSurface.copy(alpha = 0.06f))
                    .padding(10.dp)
            ) {
                BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview) {
                    vm.updateText(block.id, it)
                }
            }
        }

        BlockEditing.BUTTON -> {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.primary)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    BlockField(
                        vm = vm,
                        block = block,
                        value = block.text,
                        hint = "متن دکمه",
                        style = style.copy(color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                        readOnly = preview,
                        singleLine = true,
                        hintColor = Color.White.copy(alpha = 0.7f)
                    ) { vm.updateText(block.id, it.replace("\n", " ")) }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.onSurface.copy(alpha = 0.06f))
                        .padding(10.dp)
                ) {
                    BlockField(
                        vm = vm,
                        block = block,
                        value = block.extra,
                        hint = "لینک دکمه (https://…)",
                        style = style.copy(fontSize = 13.sp, lineHeight = 20.sp, textDirection = TextDirection.Ltr),
                        readOnly = preview,
                        main = false,
                        singleLine = true
                    ) { vm.updateExtra(block.id, it.replace("\n", "")) }
                }
            }
        }

        BlockEditing.URL -> {
            BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview, singleLine = true) {
                vm.updateText(block.id, it.replace("\n", ""))
            }
        }

        BlockEditing.DIVIDER -> {
            when (type) {
                BlockType.SEPARATOR -> HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = scheme.onSurface.copy(alpha = 0.3f)
                )
                BlockType.MORE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = scheme.onSurface.copy(alpha = 0.25f))
                    Text(
                        text = "  ادامه مطلب  ",
                        color = scheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = scheme.onSurface.copy(alpha = 0.25f))
                }
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "فاصله", color = scheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }

        BlockEditing.PLACEHOLDER -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(type.icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(6.dp))
                Text(text = type.label, color = scheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ویرایش کامل این بلوک توی مرحله‌های بعد اضافه می‌شه",
                    color = scheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
