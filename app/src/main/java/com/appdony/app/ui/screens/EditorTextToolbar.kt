@file:OptIn(ExperimentalFoundationApi::class)

package com.appdony.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuKeys
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuSession
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuDropdownProvider
import androidx.compose.foundation.text.contextmenu.provider.LocalTextContextMenuToolbarProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuDataProvider
import androidx.compose.foundation.text.contextmenu.provider.TextContextMenuProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.StrikethroughS
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.appdony.app.data.model.InlineFormat
import com.appdony.app.data.model.SpanKind
import kotlinx.coroutines.awaitCancellation

/**
 * منوی انتخاب متنِ ادیتور. جای منوی سیستم (که «ترجمه»، «خواندن با صدای بلند» و ... داشت)
 * منوی خودمونه با بولد، کج، خط‌خورده، لینک و بریدن/کپی/چسباندن/انتخاب همه.
 *
 * توی Compose 1.9 فیلد متن اول سراغ provider جدید منو (LocalTextContextMenuToolbarProvider) می‌ره و
 * LocalTextToolbar فقط جایگزینه. برای همین ریشه‌ی ادیتور با [ProvideEditorTextToolbar] هر دو provider
 * (toolbar و dropdown) رو با provider خودمون پر می‌کنه؛ اگه یکیشون null بمونه Compose هر دو رو
 * با نسخه‌ی پیش‌فرض سیستم عوض می‌کنه.
 */
class EditorTextToolbar : TextToolbar {

    /** [rect] بازه‌ی متن انتخاب‌شده توی مختصات پنجره‌ست */
    class Request(
        val rect: Rect,
        val onCopy: (() -> Unit)?,
        val onPaste: (() -> Unit)?,
        val onCut: (() -> Unit)?,
        val onSelectAll: (() -> Unit)?
    )

    var request by mutableStateOf<Request?>(null)

    /** وقتی true باشه، به‌جای دکمه‌ها یه اینپوت برای نوشتن آدرس لینک دیده می‌شه */
    var linkMode by mutableStateOf(false)

    /** یه نقطه‌ی مختصات سالم توی صفحه؛ برای تبدیل جای انتخاب به مختصات پنجره */
    internal var anchor: LayoutCoordinates? = null

    override val status: TextToolbarStatus
        get() = if (request != null) TextToolbarStatus.Shown else TextToolbarStatus.Hidden

    /** مسیر قدیمی (LocalTextToolbar)؛ فیلدهای ادیتور با provider جدید کار می‌کنن ولی اینجا هم سالم نگه داشته شده */
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (linkMode) return
        request = Request(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
    }

    override fun hide() {
        // موقع نوشتن آدرس لینک، فیلد متن فوکوسش رو از دست می‌ده و سیستم hide می‌زنه؛ اینپوت باید بمونه
        if (!linkMode) request = null
    }

    fun close() {
        linkMode = false
        request = null
    }

    /** جای متن انتخاب‌شده رو از مختصات خودش به مختصات پنجره می‌بره */
    private fun windowRect(data: TextContextMenuDataProvider): Rect {
        val a = anchor?.takeIf { it.isAttached } ?: return Rect.Zero
        val local = data.contentBounds(a)
        return Rect(a.localToWindow(local.topLeft), a.localToWindow(local.bottomRight))
    }

    /** آیتم‌های بریدن/کپی/چسباندن/انتخاب همه رو از داده‌ی Compose برمی‌داره؛ بقیه (ترجمه و ...) نادیده گرفته می‌شن */
    internal fun present(data: TextContextMenuDataProvider): Request {
        val items = data.data().components.filterIsInstance<TextContextMenuItem>()
        val session = object : TextContextMenuSession {
            override fun close() {
                request = null
            }
        }

        fun action(key: Any): (() -> Unit)? {
            val item = items.firstOrNull { it.key == key } ?: return null
            return { item.onClick.invoke(session) }
        }

        val made = Request(
            rect = windowRect(data),
            onCopy = action(TextContextMenuKeys.CopyKey),
            onPaste = action(TextContextMenuKeys.PasteKey),
            onCut = action(TextContextMenuKeys.CutKey),
            onSelectAll = action(TextContextMenuKeys.SelectAllKey)
        )
        request = made
        return made
    }

    /** وقتی Compose منو رو می‌بنده؛ فقط اگه هنوز همون منوی خودش باشه و اینپوت لینک باز نباشه پاک می‌شه */
    internal fun release(made: Request) {
        if (request === made && !linkMode) request = null
    }
}

/** provider منوی انتخاب متن که به‌جای منوی سیستم، منوی ادیتور رو نشون می‌ده */
internal class EditorTextContextMenuProvider(
    private val toolbar: EditorTextToolbar
) : TextContextMenuProvider {
    override suspend fun showTextContextMenu(dataProvider: TextContextMenuDataProvider): Nothing {
        // موقع نوشتن لینک، اینپوت باید بمونه و منوی تازه جاش رو نگیره
        if (toolbar.linkMode) awaitCancellation()
        val made = toolbar.present(dataProvider)
        try {
            awaitCancellation()
        } finally {
            toolbar.release(made)
        }
    }
}

/** ریشه‌ی ادیتور رو با منوی خودمون می‌پوشونه؛ هر دو provider باید پر باشن، وگرنه سیستم پیش‌فرضش رو می‌ذاره */
@Composable
internal fun ProvideEditorTextToolbar(toolbar: EditorTextToolbar, content: @Composable () -> Unit) {
    val provider = remember(toolbar) { EditorTextContextMenuProvider(toolbar) }
    CompositionLocalProvider(
        LocalTextToolbar provides toolbar,
        LocalTextContextMenuToolbarProvider provides provider,
        LocalTextContextMenuDropdownProvider provides provider,
        content = content
    )
}

/** منو درست بالای متن انتخاب‌شده می‌شینه؛ اگه جا نبود، پایینش. [rect] توی مختصات پنجره‌ست */
private class ToolbarPositioner(
    private val rect: Rect,
    private val gap: Int,
    private val bottomInset: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val margin = gap
        val x = (rect.center.x.toInt() - popupContentSize.width / 2)
            .coerceIn(margin, maxOf(margin, windowSize.width - popupContentSize.width - margin))
        val above = rect.top.toInt() - popupContentSize.height - gap
        val below = rect.bottom.toInt() + gap * 4
        val y = if (above >= margin) above else below
        val maxY = maxOf(0, windowSize.height - bottomInset - popupContentSize.height - margin)
        return IntOffset(x, y.coerceIn(0, maxY))
    }
}

/** منو یا اینپوت لینک؛ باید داخل ریشه‌ی صفحه‌ی ادیتور گذاشته بشه */
@Composable
internal fun EditorTextToolbarHost(toolbar: EditorTextToolbar, vm: EditorViewModel) {
    // یه مربع ۱ دی‌پی نامرئی فقط برای اینکه مختصات پنجره دستمون باشه
    Box(
        modifier = Modifier
            .size(1.dp)
            .onGloballyPositioned { toolbar.anchor = it }
    )
    DisposableEffect(toolbar) { onDispose { toolbar.anchor = null } }

    val request = toolbar.request ?: return
    val gap = with(LocalDensity.current) { 10.dp.roundToPx() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    val positioner = remember(request.rect, gap, imeBottom) { ToolbarPositioner(request.rect, gap, imeBottom) }
    if (toolbar.linkMode) {
        LinkPopup(toolbar = toolbar, vm = vm, positioner = positioner)
    } else {
        MenuPopup(toolbar = toolbar, request = request, vm = vm, positioner = positioner)
    }
}

@Composable
private fun ToolbarSurface(content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = scheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.10f)),
        content = content
    )
}

@Composable
private fun MenuPopup(
    toolbar: EditorTextToolbar,
    request: EditorTextToolbar.Request,
    vm: EditorViewModel,
    positioner: PopupPositionProvider
) {
    val scheme = MaterialTheme.colorScheme
    val sel = vm.fmtSel
    val block = sel?.let { s -> vm.blocks.firstOrNull { it.id == s.blockId } }
    val canFormat = sel != null && block != null && sel.end > sel.start && block.type.supportsInline
    val hasClipboard = request.onCut != null || request.onCopy != null ||
        request.onPaste != null || request.onSelectAll != null
    val maxWidth = LocalConfiguration.current.screenWidthDp.dp - 20.dp

    fun active(kind: SpanKind): Boolean =
        canFormat && InlineFormat.covers(block!!.spans, kind, sel!!.start, sel.end)

    Popup(
        popupPositionProvider = positioner,
        onDismissRequest = null,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        ToolbarSurface {
            Row(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canFormat) {
                    ToolButton(Icons.Outlined.FormatBold, "پررنگ", active = active(SpanKind.BOLD)) {
                        vm.toggleFormat(SpanKind.BOLD)
                    }
                    ToolButton(Icons.Outlined.FormatItalic, "کج", active = active(SpanKind.ITALIC)) {
                        vm.toggleFormat(SpanKind.ITALIC)
                    }
                    ToolButton(Icons.Outlined.StrikethroughS, "خط‌خورده", active = active(SpanKind.STRIKE)) {
                        vm.toggleFormat(SpanKind.STRIKE)
                    }
                    ToolButton(Icons.Outlined.Link, "لینک", active = active(SpanKind.LINK)) {
                        vm.linkTarget = vm.fmtSel
                        toolbar.linkMode = true
                    }
                    if (hasClipboard) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 5.dp)
                                .width(1.dp)
                                .height(22.dp)
                                .background(scheme.onSurface.copy(alpha = 0.14f))
                        )
                    }
                }
                request.onCut?.let { ToolButton(Icons.Outlined.ContentCut, "بریدن", onClick = it) }
                request.onCopy?.let { ToolButton(Icons.Outlined.ContentCopy, "کپی", onClick = it) }
                request.onPaste?.let { ToolButton(Icons.Outlined.ContentPaste, "چسباندن", onClick = it) }
                request.onSelectAll?.let { ToolButton(Icons.Outlined.SelectAll, "انتخاب همه", onClick = it) }
            }
        }
    }
}

@Composable
private fun LinkPopup(
    toolbar: EditorTextToolbar,
    vm: EditorViewModel,
    positioner: PopupPositionProvider
) {
    val scheme = MaterialTheme.colorScheme
    val target = vm.linkTarget
    val block = target?.let { t -> vm.blocks.firstOrNull { it.id == t.blockId } }
    val existing = if (target != null && block != null) {
        InlineFormat.linkAt(block.spans, target.start, target.end)
    } else {
        null
    }
    var url by remember { mutableStateOf(existing?.url.orEmpty()) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val width = minOf(360.dp, LocalConfiguration.current.screenWidthDp.dp - 20.dp)
    val canSave = url.isNotBlank() || existing != null

    val finish: () -> Unit = {
        vm.linkTarget = null
        toolbar.close()
    }
    val save: () -> Unit = {
        vm.applyLink(url)
        finish()
    }

    val inputStyle = TextStyle(
        color = scheme.onSurface,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        textDirection = TextDirection.Ltr
    )

    Popup(
        popupPositionProvider = positioner,
        onDismissRequest = finish,
        properties = PopupProperties(focusable = true, clippingEnabled = false)
    ) {
        ToolbarSurface {
            Row(
                modifier = Modifier
                    .width(width)
                    .padding(start = 8.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Link,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.onSurface.copy(alpha = 0.07f))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = url,
                        onValueChange = { url = it.replace("\n", "") },
                        singleLine = true,
                        textStyle = inputStyle,
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (canSave) save() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (url.isEmpty()) {
                                    Text(
                                        text = "https://…",
                                        style = inputStyle.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.55f))
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                Spacer(Modifier.width(6.dp))
                if (existing != null) {
                    ToolButton(Icons.Outlined.LinkOff, "برداشتن لینک", tint = scheme.error) {
                        url = ""
                        save()
                    }
                    Spacer(Modifier.width(2.dp))
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .alpha(if (canSave) 1f else 0.45f)
                        .clip(CircleShape)
                        .background(scheme.primary)
                        .clickable(enabled = canSave, onClick = save),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Save,
                        contentDescription = "ذخیره لینک",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    desc: String,
    active: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 40.dp,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) scheme.primary.copy(alpha = 0.22f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (active) scheme.primary else tint,
            modifier = Modifier.size(21.dp)
        )
    }
}
