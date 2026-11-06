package com.appdony.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.api.EditorApi
import com.appdony.app.data.api.MetaApi
import com.appdony.app.data.model.AppInfo
import com.appdony.app.data.model.BlockEditing
import com.appdony.app.data.model.BlockGroup
import com.appdony.app.data.model.BlockParser
import com.appdony.app.data.model.BlockSerializer
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.EditorBlock
import com.appdony.app.data.model.InlineFormat
import com.appdony.app.data.model.PostSettings
import com.appdony.app.data.model.SeoInfo
import com.appdony.app.data.model.WpCategory
import com.appdony.app.ui.components.AppBanner
import com.appdony.app.ui.components.BannerKind
import com.appdony.app.ui.components.EditorLoadSkeleton
import com.appdony.app.ui.components.FloatingDialog
import com.appdony.app.ui.components.glassKnob
import com.appdony.app.ui.components.glassSelectedIconTint
import com.appdony.app.ui.components.glassTrack
import com.appdony.app.ui.components.isGlassDark
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import kotlin.math.pow

/** ۶ بلوک ثابت کشوی پایین (هیچ شمارشی روی گوشی ذخیره نمی‌شه) */
private val QUICK_BLOCKS = listOf(
    BlockType.PARAGRAPH, BlockType.HEADING, BlockType.IMAGE,
    BlockType.LIST, BlockType.BUTTON, BlockType.QUOTE
)

private val DATE_FORMAT = Regex("""^\d{4}-\d{2}-\d{2}([ T]\d{2}:\d{2}(:\d{2})?)?$""")

/** 2026-11-05 10:30 → 2026-11-05T10:30:00 (فرمت REST)؛ نامعتبر = null */
private fun isoDate(raw: String): String? {
    val t = raw.trim()
    if (!DATE_FORMAT.matches(t)) return null
    var r = t.replace(' ', 'T')
    if (!r.contains('T')) r += "T00:00:00" else if (r.length == 16) r += ":00"
    return r
}

// ───────────────────────── وضعیت ادیتور (با چرخش صفحه از بین نمی‌ره، روی گوشی ذخیره نمی‌شه) ─────────────────────────

class EditorViewModel : ViewModel() {
    val blocks = mutableStateListOf<EditorBlock>()
    var title by mutableStateOf("")
    var focusedId by mutableStateOf<Long?>(null)
    var pendingFocusId by mutableStateOf<Long?>(null)
    var pendingListFocus by mutableStateOf<Pair<Long, Int>?>(null)
    var scrollToId by mutableStateOf<Long?>(null)
    private var nextId = 1L

    // قالب‌بندی داخل متن: انتخابِ فعلی، و بازه‌ای که دکمه‌ی لینک روش زده شده
    var fmtSel by mutableStateOf<FormatSel?>(null)
    var linkTarget by mutableStateOf<FormatSel?>(null)

    // ذخیره روی سایت: بعد از اولین ذخیره (یا باز شدن مطلب موجود) شناسه و لینک مطلب مشخص می‌شه
    // و ذخیره‌های بعدی همون مطلب رو به‌روز می‌کنن
    var postId by mutableStateOf<Long?>(null)
    var link by mutableStateOf<String?>(null)
    var status by mutableStateOf("draft")
    var saving by mutableStateOf(false)

    // باز کردن مطلب موجود از سایت
    var loading by mutableStateOf(false)
    var loadError by mutableStateOf<String?>(null)

    // تصویر شاخص
    var featuredId by mutableStateOf<Long?>(null)
    var featuredUrl by mutableStateOf<String?>(null)
    var uploading by mutableStateOf(false)

    // پنل‌های کناری
    var appInfo by mutableStateOf(AppInfo())
    var seo by mutableStateOf(SeoInfo())
    var settings by mutableStateOf(PostSettings())
    var categories by mutableStateOf<List<WpCategory>>(emptyList())
    var categoriesLoaded by mutableStateOf(false)
    var loadingCategories by mutableStateOf(false)

    private var cleanSnapshot = ""

    init {
        val first = EditorBlock(nextId++, BlockType.PARAGRAPH)
        blocks.add(first)
        focusedId = first.id
        cleanSnapshot = snapshot()
    }

    private fun snapshot(): String = buildString {
        append(title).append('\u0001').append(featuredId ?: 0L)
        append('\u0001').append(appInfo).append('\u0001').append(seo).append('\u0001').append(settings)
        blocks.forEach {
            append('\u0001').append(it.type.name).append('|').append(it.text).append('|').append(it.extra).append('|').append(it.spans)
        }
    }

    /** آیا چیزی نوشته شده که هنوز ذخیره نشده؟ */
    val isDirty: Boolean
        get() = snapshot() != cleanSnapshot

    // ───────────── باز کردن مطلب موجود ─────────────

    /**
     * مطلب [id] رو از سایت می‌گیره (عنوان، محتوا → بلوک‌ها، وضعیت، تصویر شاخص، تنظیمات و متاهای playclone/Yoast)
     * و توی ادیتور می‌ذاره. اگه چیزی نشد، [loadError] پر می‌شه و دوباره امتحان ممکنه.
     */
    fun loadExisting(context: Context, kind: String, id: Long) {
        if (loading || postId != null) return
        loading = true
        loadError = null
        val app = context.applicationContext
        viewModelScope.launch {
            val postResult = EditorApi.fetch(app, kind, id)
            val remote = postResult.getOrNull()
            if (remote == null) {
                loadError = postResult.exceptionOrNull()?.message ?: "مطلب گرفته نشد"
                loading = false
                return@launch
            }
            // بدون متاها اجازه‌ی ویرایش نمی‌دیم؛ وگرنه ذخیره می‌تونست فیلدهای برنامه رو خالی کنه
            val metaResult = MetaApi.load(id)
            val meta = metaResult.getOrNull()
            if (meta == null) {
                loadError = metaResult.exceptionOrNull()?.message ?: "اطلاعات برنامه گرفته نشد"
                loading = false
                return@launch
            }

            title = remote.title
            blocks.clear()
            BlockParser.parse(remote.content).forEach { blocks.add(it.copy(id = nextId++)) }
            if (blocks.isEmpty()) blocks.add(EditorBlock(nextId++, BlockType.PARAGRAPH))
            focusedId = null
            pendingFocusId = null
            pendingListFocus = null

            status = remote.status
            link = remote.link.ifBlank { null }
            featuredId = remote.featuredId
            featuredUrl = remote.featuredUrl
            settings = PostSettings(
                slug = remote.slug,
                date = remote.date,
                excerpt = remote.excerpt,
                commentsOpen = remote.commentsOpen,
                categoryIds = remote.categoryIds
            )
            appInfo = meta.first
            seo = meta.second
            postId = remote.id
            cleanSnapshot = snapshot()
            loading = false
        }
    }

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
        when (type.editing) {
            BlockEditing.LIST -> pendingListFocus = block.id to 0
            BlockEditing.PLACEHOLDER, BlockEditing.DIVIDER -> Unit
            else -> pendingFocusId = block.id
        }
        scrollToId = block.id
    }

    fun updateText(id: Long, text: String) {
        val i = blocks.indexOfFirst { it.id == id }
        if (i >= 0 && blocks[i].text != text) {
            val old = blocks[i]
            // قالب‌بندی‌ها (بولد، لینک و ...) با تایپ و پاک کردن جابه‌جا می‌شن
            blocks[i] = old.copy(text = text, spans = InlineFormat.shift(old.text, text, old.spans))
        }
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
        val shifted = InlineFormat.shift(blocks[i].text, raw, blocks[i].spans)
        blocks[i] = blocks[i].copy(text = before, spans = InlineFormat.slice(shifted, 0, cut))
        val next = EditorBlock(
            nextId++,
            BlockType.PARAGRAPH,
            text = after,
            spans = InlineFormat.slice(shifted, cut + 1, raw.length)
        )
        blocks.add(i + 1, next)
        focusedId = next.id
        pendingFocusId = next.id
        scrollToId = next.id
    }

    // ───────────── فهرست: هر خط یک مورد ─────────────

    private fun listItems(id: Long): MutableList<String>? {
        val i = blocks.indexOfFirst { it.id == id }
        return if (i < 0) null else blocks[i].text.split('\n').toMutableList()
    }

    fun setListItem(id: Long, index: Int, value: String) {
        val items = listItems(id) ?: return
        if (index !in items.indices) return
        items[index] = value
        updateText(id, items.joinToString("\n"))
    }

    fun splitListItem(id: Long, index: Int, before: String, after: String) {
        val items = listItems(id) ?: return
        if (index !in items.indices) return
        items[index] = before
        items.add(index + 1, after)
        updateText(id, items.joinToString("\n"))
        pendingListFocus = id to index + 1
    }

    /** بک‌اسپیس روی مورد خالی: مورد پاک می‌شه و فوکوس می‌ره مورد قبلی (اگه تک‌مورد بود، خود بلوک پاک می‌شه) */
    fun removeListItem(id: Long, index: Int) {
        val items = listItems(id) ?: return
        if (items.size <= 1) {
            removeEmptyAndFocusPrevious(id)
            return
        }
        if (index !in items.indices) return
        items.removeAt(index)
        updateText(id, items.joinToString("\n"))
        pendingListFocus = id to (index - 1).coerceAtLeast(0)
    }

    /** اینتر روی مورد خالیِ آخر: از فهرست بیرون میای و یه پاراگراف تازه شروع می‌شه */
    fun exitList(id: Long) {
        val items = listItems(id) ?: return
        val i = blocks.indexOfFirst { it.id == id }
        if (items.size > 1) items.removeAt(items.lastIndex)
        updateText(id, items.joinToString("\n"))
        val next = EditorBlock(nextId++, BlockType.PARAGRAPH)
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
        val iso = if (settings.date.isBlank()) null else isoDate(settings.date)
        if (settings.date.isNotBlank() && iso == null) {
            AppBanner.show(BannerKind.WARNING, "تاریخ باید مثل 2026-11-05 10:30 باشه", 4500L)
            return
        }

        val extra = JsonObject()
        if (settings.slug.isNotBlank()) extra.addProperty("slug", settings.slug.trim())
        if (iso != null) extra.addProperty("date", iso)
        if (settings.excerpt.isNotBlank() || postId != null) extra.addProperty("excerpt", settings.excerpt)
        extra.addProperty("comment_status", if (settings.commentsOpen) "open" else "closed")
        if (kind != "blog" && (settings.categoryIds.isNotEmpty() || postId != null)) {
            val ids = JsonArray()
            settings.categoryIds.forEach { ids.add(it) }
            extra.add("categories", ids)
        }

        val serialized = BlockSerializer.serialize(blocks.toList())
        val snap = snapshot()
        val app = context.applicationContext
        val info = appInfo
        val seoNow = seo
        val withMeta = info != AppInfo() || seoNow != SeoInfo()

        saving = true
        viewModelScope.launch {
            val result = EditorApi.save(app, kind, postId, title.trim(), serialized.html, wanted, featuredId, extra)
            val post = result.getOrNull()
            var metaError: String? = null
            if (post != null && withMeta) {
                metaError = MetaApi.save(post.id, info, seoNow).exceptionOrNull()?.message
            }
            saving = false

            result.onSuccess { saved ->
                postId = saved.id
                link = saved.link.ifBlank { null }
                status = saved.status
                cleanSnapshot = snap
                val done = when (saved.status) {
                    "publish" -> "منتشر شد"
                    "pending" -> "برای بازبینی ذخیره شد"
                    "future" -> "زمان‌بندی شد"
                    else -> "پیش‌نویس ذخیره شد"
                }
                val notes = ArrayList<String>()
                if (metaError != null) notes.add(metaError)
                val skipped = serialized.skipped.distinct()
                if (skipped.isNotEmpty()) notes.add("بلوک‌های ${skipped.joinToString("، ")} هنوز ذخیره نمی‌شن")
                if (saved.note != null) notes.add(saved.note)
                if (notes.isEmpty()) {
                    AppBanner.show(BannerKind.SUCCESS, done)
                } else {
                    AppBanner.show(BannerKind.WARNING, "$done؛ ${notes.joinToString("؛ ")}", 5500L)
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

    // ───────────── دسته‌ها (فقط توی حافظه‌ی همین صفحه) ─────────────

    fun loadCategories(context: Context) {
        if (categoriesLoaded || loadingCategories) return
        loadingCategories = true
        val app = context.applicationContext
        viewModelScope.launch {
            EditorApi.categories(app).onSuccess {
                categories = it
                categoriesLoaded = true
            }.onFailure {
                AppBanner.show(BannerKind.ERROR, it.message ?: "دسته‌ها گرفته نشد", 4500L)
            }
            loadingCategories = false
        }
    }
}

// ───────────────────────── صفحه‌ی ادیتور ─────────────────────────

/**
 * ایجاد یا ویرایش نوشته / مطلب وبلاگ. [kind] = "post" یا "blog"؛ [postId] بزرگ‌تر از صفر = ویرایش مطلب موجود.
 * بالا: نوار آیکن‌ها + کشوی «همه‌ی بلوک‌ها»؛ پایین: کشوی «۶ بلوک»؛ لبه‌ی راست: قرص عمودی پنل‌ها.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(kind: String, postId: Long = 0L, onBack: () -> Unit) {
    // منوی انتخاب متنِ سیستم (ترجمه، خواندن با صدای بلند، ...) با منوی خودمون عوض می‌شه.
    // توی Compose 1.9 هر دو provider منو باید با provider خودمون پر بشن؛ اگه یکیشون null باشه
    // Compose هر دو رو با نسخه‌ی پیش‌فرض سیستم عوض می‌کنه (جزئیات توی EditorTextToolbar.kt).
    val toolbar = remember { EditorTextToolbar() }
    ProvideEditorTextToolbar(toolbar) {
        EditorScreenContent(kind = kind, postId = postId, onBack = onBack, toolbar = toolbar)
    }
}

@Composable
private fun EditorScreenContent(kind: String, postId: Long, onBack: () -> Unit, toolbar: EditorTextToolbar) {
    val isBlog = kind == "blog"
    val ctx = LocalContext.current
    val vm: EditorViewModel = viewModel(key = "editor_${kind}_$postId")
    val scheme = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    var topOpen by rememberSaveable { mutableStateOf(false) }
    var bottomOpen by rememberSaveable { mutableStateOf(true) }
    var preview by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var panel by rememberSaveable { mutableStateOf<SidePanel?>(null) }
    var showOutline by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // مطلب موجود: تا از سایت نیومده، فقط صفحه‌ی بارگذاری دیده می‌شه
    val awaiting = postId > 0L && vm.postId == null
    LaunchedEffect(postId) {
        if (postId > 0L) vm.loadExisting(ctx, kind, postId)
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) vm.uploadFeatured(ctx, uri)
    }

    val requestClose: () -> Unit = { if (vm.isDirty) confirmExit = true else onBack() }
    val insertBlock: (BlockType) -> Unit = { type ->
        vm.insert(type)
        topOpen = false
    }

    BackHandler {
        when {
            awaiting -> onBack()
            preview -> preview = false
            panel != null -> panel = null
            menuOpen -> menuOpen = false
            topOpen -> topOpen = false
            else -> requestClose()
        }
    }

    // بلوک تازه یا جابه‌جاشده: صفحه تا اونجا اسکرول می‌شه (ردیف اول لیست خودِ عنوانه)
    val scrollTarget = vm.scrollToId
    LaunchedEffect(scrollTarget) {
        if (scrollTarget != null) {
            val index = vm.blocks.indexOfFirst { it.id == scrollTarget }
            if (index >= 0) listState.animateScrollToItem(index + 1)
            vm.scrollToId = null
        }
    }

    val saved = vm.postId != null
    val statusLabel = when (vm.status) {
        "publish" -> "منتشرشده"
        "pending" -> "در انتظار بازبینی"
        "future" -> "زمان‌بندی‌شده"
        "private" -> "خصوصی"
        else -> "پیش‌نویس"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                    if (preview) {
                        topOpen = false
                        panel = null
                        menuOpen = false
                    }
                },
                onSave = { showSave = true },
                onSettings = {
                    menuOpen = false
                    panel = SidePanel.SETTINGS
                },
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "title") {
                    TitleField(
                        value = vm.title,
                        hint = if (isBlog) "عنوان مطلب وبلاگ…" else "عنوان نوشته…",
                        readOnly = preview,
                        onChange = { vm.title = it.replace("\n", " ") }
                    )
                }
                val featured = vm.featuredUrl
                if (preview && featured != null) {
                    item(key = "featured_preview") {
                        AsyncImage(
                            model = featured,
                            contentDescription = "تصویر شاخص",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                }
                itemsIndexed(vm.blocks, key = { _, block -> block.id }) { index, block ->
                    BlockCard(block = block, index = index, count = vm.blocks.size, vm = vm, preview = preview)
                }
            }

            // کشوی پایین: ۶ بلوک ثابت
            if (!preview) {
                QuickBar(
                    blocks = QUICK_BLOCKS,
                    open = bottomOpen,
                    onToggle = { bottomOpen = !bottomOpen },
                    onPick = insertBlock
                )
            }
        }

        if (!preview) {
            // پرده‌ی پشت پنل: زدن روش پنل رو می‌بنده
            AnimatedVisibility(
                visible = panel != null,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(200))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { panel = null }
                )
            }

            // پنل: وسط صفحه؛ از راست میاد و به راست برمی‌گرده
            SidePanelHost(
                panel = panel,
                vm = vm,
                isBlog = isBlog,
                onPickImage = { pickImage.launch("image/*") },
                onClose = { panel = null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 14.dp)
            )

            // قرص عمودی: لبه‌ی راست، وسط صفحه؛ وقتی پنل بازه به راست می‌ره و برمی‌گرده
            AnimatedVisibility(
                visible = panel == null,
                modifier = Modifier.align(Alignment.CenterStart),
                enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(200)),
                exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(160))
            ) {
                SideDock(
                    menuOpen = menuOpen,
                    onHandle = { menuOpen = !menuOpen },
                    onPick = { item ->
                        panel = item
                        menuOpen = false
                    },
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }

        if (awaiting) {
            LoadOverlay(
                error = vm.loadError,
                onRetry = { vm.loadExisting(ctx, kind, postId) },
                onBack = onBack
            )
        }

        // منوی انتخاب متن: بولد، کج، خط‌خورده، لینک، کپی و ...
        EditorTextToolbarHost(toolbar = toolbar, vm = vm)
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

/** صفحه‌ی گرفتن مطلب از سایت: اسکلتِ نورانی تا بیاد؛ اگه نشد پیام + «تلاش دوباره» و «بازگشت» */
@Composable
private fun LoadOverlay(error: String?, onRetry: () -> Unit, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { },
        contentAlignment = Alignment.Center
    ) {
        if (error == null) {
            EditorLoadSkeleton()
        } else {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = error, color = scheme.error, fontSize = 14.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onRetry) { Text("تلاش دوباره") }
                    TextButton(onClick = onBack) { Text("بازگشت") }
                }
            }
        }
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

/**
 * سایه‌ی نرم با نور از بالا (مثل کاشی‌های منوی پایین): دور شکل و زیرش پخش می‌شه ولی بالای شکل تقریباً خالیه.
 * داخل خودِ شکل کشیده نمی‌شه تا شیشه تیره نشه. [offsetY] هرچی بیشتر، سایه بیشتر زیر شکل میفته و کمتر بالاش.
 * [color].alpha غلظت نزدیک لبه‌ست و با افت درجه‌ی دوم محو می‌شه. باید قبل از clip گذاشته بشه.
 */
private fun Modifier.topLitShadow(
    shape: Shape,
    color: Color,
    spread: Dp,
    offsetY: Dp,
    layers: Int = 14,
    falloff: Float = 2.2f
): Modifier = this.drawBehind {
    // وقتی کشو بسته‌ست و اندازه‌اش صفره، سایه‌ای کشیده نمی‌شه
    if (size.minDimension < 2f) return@drawBehind
    val path = when (val o = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Generic -> o.path
        is Outline.Rounded -> Path().apply { addRoundRect(o.roundRect) }
        is Outline.Rectangle -> Path().apply { addRect(o.rect) }
    }
    val spreadPx = spread.toPx()
    clipPath(path, ClipOp.Difference) {
        translate(top = offsetY.toPx()) {
            for (i in 1..layers) {
                val reach = spreadPx * i / layers
                val share = (1f - (i - 1f) / layers).pow(falloff) - (1f - i.toFloat() / layers).pow(falloff)
                drawPath(
                    path = path,
                    color = color.copy(alpha = color.alpha * share),
                    style = Stroke(width = reach * 2f, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** کشوی پایین: ۶ بلوک ثابت */
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
        // سایه روی خودِ AnimatedVisibility می‌شینه تا موقع باز و بسته شدن کشو بریده نشه
        AnimatedVisibility(
            visible = open,
            modifier = Modifier
                .padding(top = if (open) 6.dp else 0.dp)
                .topLitShadow(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.Black.copy(alpha = if (isGlassDark()) 0.45f else 0.18f),
                    spread = 16.dp,
                    offsetY = 9.dp
                ),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
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

// ───────────────────────── عنوان ─────────────────────────

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
