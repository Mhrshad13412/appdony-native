package com.appdony.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.appdony.app.data.model.BlockEditing
import com.appdony.app.data.model.BlockSerializer
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.EditorBlock
import com.appdony.app.data.model.ImageData
import com.appdony.app.data.model.InlineFormat

// ───────────────────────── بلوک آزاد (مثل وردپرس: بدون کادر و رنگ) ─────────────────────────

@Composable
internal fun BlockCard(block: EditorBlock, index: Int, count: Int, vm: EditorViewModel, preview: Boolean) {
    val focused = !preview && vm.focusedId == block.id
    var settingsOpen by remember(block.id) { mutableStateOf(false) }
    val showSettings = focused && settingsOpen && hasSettings(block.type)
    val cardModifier = if (preview) {
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { vm.focusedId = block.id }
            .padding(vertical = 2.dp)
    }
    Column(modifier = cardModifier) {
        if (focused) {
            BlockToolbar(
                block = block,
                index = index,
                count = count,
                vm = vm,
                settingsOpen = showSettings,
                onSettings = { settingsOpen = !settingsOpen }
            )
        }
        BlockBody(block = block, vm = vm, preview = preview, focused = focused)
        AnimatedVisibility(
            visible = showSettings,
            enter = expandVertically(tween(220)) + fadeIn(tween(180)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120))
        ) {
            BlockSettings(block = block, vm = vm)
        }
    }
}

/** بلوک‌هایی که آیکن تنظیمات دارن */
private fun hasSettings(type: BlockType): Boolean = when (type) {
    BlockType.IMAGE, BlockType.HEADING, BlockType.LIST, BlockType.ORDERED_LIST,
    BlockType.QUOTE, BlockType.PULLQUOTE -> true
    else -> false
}

private fun isRaw(block: EditorBlock): Boolean =
    block.type == BlockType.HTML && block.extra == BlockSerializer.RAW

/** نوار ابزار کوچیک و خنثی فقط برای بلوک انتخاب‌شده */
@Composable
private fun BlockToolbar(
    block: EditorBlock,
    index: Int,
    count: Int,
    vm: EditorViewModel,
    settingsOpen: Boolean,
    onSettings: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(block.type.icon, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = if (isRaw(block)) "بلوک خام (HTML سایت)" else block.type.label,
            color = scheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        if (hasSettings(block.type)) {
            MiniButton(
                Icons.Outlined.Tune,
                "تنظیمات بلوک",
                tint = if (settingsOpen) scheme.primary else scheme.onSurfaceVariant,
                onClick = onSettings
            )
        }
        MiniButton(Icons.Outlined.ArrowUpward, "بالا", enabled = index > 0) { vm.move(block.id, -1) }
        MiniButton(Icons.Outlined.ArrowDownward, "پایین", enabled = index < count - 1) { vm.move(block.id, 1) }
        MiniButton(Icons.Outlined.Delete, "حذف", tint = scheme.error) { vm.remove(block.id) }
    }
}

// ───────────────────────── تنظیمات هر بلوک ─────────────────────────

@Composable
private fun BlockSettings(block: EditorBlock, vm: EditorViewModel) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (block.type) {
            BlockType.IMAGE -> ImageSettings(block = block, vm = vm)

            BlockType.HEADING -> {
                SettingLabel("سطح سرنویس")
                LevelChips(current = block.extra) { vm.updateExtra(block.id, it) }
            }

            BlockType.LIST, BlockType.ORDERED_LIST -> {
                SettingLabel("نوع فهرست")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingChip("نقطه‌ای", block.type == BlockType.LIST) { vm.changeType(block.id, BlockType.LIST) }
                    SettingChip("شماره‌دار", block.type == BlockType.ORDERED_LIST) { vm.changeType(block.id, BlockType.ORDERED_LIST) }
                }
            }

            BlockType.QUOTE, BlockType.PULLQUOTE -> {
                SettingLabel("سبک نقل‌قول")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingChip("عادی", block.type == BlockType.QUOTE) { vm.changeType(block.id, BlockType.QUOTE) }
                    SettingChip("برجسته", block.type == BlockType.PULLQUOTE) { vm.changeType(block.id, BlockType.PULLQUOTE) }
                }
            }

            else -> Unit
        }
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.22f) else scheme.onSurface.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** متن جایگزین (alt)، تغییر و حذف تصویر */
@Composable
private fun ImageSettings(block: EditorBlock, vm: EditorViewModel) {
    val scheme = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadBlockImage(ctx, block.id, uri)
    }
    val hasImage = ImageData.url(block.text).isNotEmpty()

    SettingLabel("متن جایگزین (alt)")
    val altStyle = TextStyle(
        color = scheme.onSurface,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        textDirection = TextDirection.Content
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        BasicTextField(
            value = block.extra,
            onValueChange = { vm.updateExtra(block.id, it.replace("\n", " ")) },
            textStyle = altStyle,
            cursorBrush = SolidColor(scheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { if (it.isFocused) vm.focusedId = block.id },
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (block.extra.isEmpty()) {
                        Text(
                            text = "تصویر رو کوتاه و واضح توصیف کن",
                            style = altStyle.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                    }
                    inner()
                }
            }
        )
    }
    Text(
        text = "متن alt برای سئو و کاربرهای نابینا مهمه؛ تصویر شاخص و تصاویر داخل متن باید alt داشته باشن.",
        color = scheme.onSurfaceVariant,
        fontSize = 11.sp,
        lineHeight = 17.sp
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { picker.launch("image/*") }, enabled = !vm.uploading) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (hasImage) "تغییر تصویر" else "انتخاب تصویر")
        }
        if (hasImage) {
            TextButton(onClick = {
                vm.updateText(block.id, "")
                vm.updateExtra(block.id, "")
            }) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حذف تصویر", color = scheme.error)
            }
        }
    }
}

// ───────────────────────── بلوک تصویر و پیش‌نمایش بلوک خام ─────────────────────────

@Composable
private fun ImageBody(block: EditorBlock, vm: EditorViewModel, preview: Boolean, focused: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadBlockImage(ctx, block.id, uri)
    }
    val url = ImageData.url(block.text)
    if (url.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .clickable(enabled = !preview && !vm.uploading) { picker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (vm.uploading) {
                CircularProgressIndicator(color = scheme.primary, strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.height(6.dp))
                    Text(text = "انتخاب تصویر", color = scheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "از گالری گوشی", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AsyncImage(
                model = url,
                contentDescription = block.extra.ifBlank { null },
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.onSurface.copy(alpha = 0.06f))
            )
            if (focused && block.extra.isBlank()) {
                Text(
                    text = "متن جایگزین (alt) خالیه؛ از آیکن تنظیمات بالا پر کن",
                    color = scheme.error,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private val RAW_IMG = Regex("<img[^>]*?\\ssrc=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
private val RAW_COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)

/** پیش‌نمایش خوانا از بلوک خامِ سایت (تصویر + متن)؛ با زدن روی بلوک، کدش برای ویرایش باز می‌شه */
@Composable
private fun RawPreview(block: EditorBlock) {
    val scheme = MaterialTheme.colorScheme
    val image = RAW_IMG.find(block.text)?.groupValues?.get(1)?.replace("&amp;", "&")
    val text = remember(block.text) {
        HtmlCompat.fromHtml(block.text.replace(RAW_COMMENT, ""), HtmlCompat.FROM_HTML_MODE_COMPACT)
            .toString()
            .replace("\uFFFC", "")
            .trim()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.onSurface.copy(alpha = 0.05f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (image != null) {
            AsyncImage(
                model = image,
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                color = scheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = "بلوک خام سایت؛ برای ویرایش کد روش بزن",
            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 10.sp
        )
    }
}

@Composable
internal fun MiniButton(
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
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf("2", "3", "4").forEach { level ->
            val selected = current.ifEmpty { "2" } == level
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) scheme.onSurface.copy(alpha = 0.16f) else scheme.onSurface.copy(alpha = 0.06f))
                    .clickable { onPick(level) }
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "H$level",
                    color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
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
    BlockType.LIST, BlockType.ORDERED_LIST -> "مورد فهرست…"
    BlockType.QUOTE, BlockType.PULLQUOTE -> "نقل‌قول…"
    BlockType.VERSE -> "شعر…"
    BlockType.HTML -> "<div>…</div>"
    BlockType.SHORTCODE -> "[shortcode]"
    BlockType.CODE, BlockType.PREFORMATTED -> "کد…"
    else -> "https://…"
}

/** یک فیلد متنیِ بلوک؛ فوکوس، بک‌اسپیسِ بلوک خالی، راهنمای خالی بودن و قالب‌بندی (بولد، کج، خط‌خورده، لینک) رو مدیریت می‌کنه */
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
    formattable: Boolean = false,
    onChange: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val requester = remember { FocusRequester() }
    val toolbar = LocalTextToolbar.current as? EditorTextToolbar
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

    // متن + جای مکان‌نما و انتخاب؛ اگه متن از بیرون عوض شد (مثلاً اینتر)، با اون هماهنگ می‌شه
    var stored by remember { mutableStateOf(TextFieldValue(value)) }
    val shown = if (stored.text == value) {
        stored
    } else {
        TextFieldValue(value, TextRange(stored.selection.end.coerceIn(0, value.length)))
    }

    val transformation = if (formattable) {
        rememberSpanTransformation(vm, block.id, block.spans, 0, value.length)
    } else {
        VisualTransformation.None
    }

    fun report(selection: TextRange) {
        if (toolbar?.linkMode == true) return
        if (formattable && !readOnly) {
            vm.reportSelection(block.id, selection.min, selection.max)
        } else {
            vm.clearSelection(block.id)
        }
    }

    BasicTextField(
        value = shown,
        onValueChange = { new ->
            val textChanged = new.text != shown.text
            stored = new
            if (textChanged) onChange(new.text)
            report(new.selection)
        },
        readOnly = readOnly,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(scheme.primary),
        visualTransformation = transformation,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(requester)
            .onFocusChanged {
                if (it.isFocused) {
                    vm.focusedId = block.id
                    report(shown.selection)
                } else if (toolbar?.linkMode != true) {
                    vm.clearSelection(block.id)
                }
            }
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

// ───────────────────────── فهرست واقعی ─────────────────────────

/** هر مورد یه فیلد جدا با نقطه/شماره؛ اینتر = مورد بعدی، اینتر روی مورد خالیِ آخر = خروج از فهرست */
@Composable
private fun ListEditor(block: EditorBlock, vm: EditorViewModel, style: TextStyle, preview: Boolean) {
    val items = block.text.split('\n')
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        var offset = 0
        items.forEachIndexed { index, item ->
            val start = offset
            key(index) {
                ListItemRow(
                    block = block,
                    vm = vm,
                    index = index,
                    count = items.size,
                    value = item,
                    offset = start,
                    style = style,
                    preview = preview
                )
            }
            offset += item.length + 1
        }
    }
}

@Composable
private fun ListItemRow(
    block: EditorBlock,
    vm: EditorViewModel,
    index: Int,
    count: Int,
    value: String,
    offset: Int,
    style: TextStyle,
    preview: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val requester = remember { FocusRequester() }
    val toolbar = LocalTextToolbar.current as? EditorTextToolbar
    var stored by remember { mutableStateOf(TextFieldValue(value)) }
    val shown = if (stored.text == value) {
        stored
    } else {
        TextFieldValue(value, TextRange(stored.selection.end.coerceIn(0, value.length)))
    }
    val transformation = rememberSpanTransformation(vm, block.id, block.spans, offset, value.length)
    val localSpans = remember(block.spans, offset, value.length) {
        InlineFormat.slice(block.spans, offset, offset + value.length)
    }
    fun report(selection: TextRange) {
        if (toolbar?.linkMode == true) return
        if (!preview) vm.reportSelection(block.id, selection.min + offset, selection.max + offset)
    }

    val pendingItem = vm.pendingListFocus
    LaunchedEffect(pendingItem) {
        if (pendingItem != null && pendingItem.first == block.id && pendingItem.second == index) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
            vm.pendingListFocus = null
        }
    }
    val pending = vm.pendingFocusId
    LaunchedEffect(pending) {
        if (pending == block.id && index == count - 1) {
            withFrameNanos { }
            runCatching { requester.requestFocus() }
            vm.pendingFocusId = null
        }
    }

    val marker = if (block.type.ordered) "${index + 1}." else "•"
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = marker,
            style = style.copy(color = scheme.onSurfaceVariant),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(26.dp)
        )
        if (preview) {
            Text(
                text = styledText(value, localSpans, scheme.primary),
                style = style,
                modifier = Modifier.weight(1f)
            )
        } else {
            BasicTextField(
                value = shown,
                onValueChange = { new ->
                    val raw = new.text
                    stored = new
                    if (raw != shown.text) {
                        if (raw.contains('\n')) {
                            val cut = raw.indexOf('\n')
                            val before = raw.substring(0, cut)
                            val after = raw.substring(cut + 1).replace("\n", " ")
                            when {
                                before.isEmpty() && after.isEmpty() && count == 1 -> vm.setListItem(block.id, index, "")
                                before.isEmpty() && after.isEmpty() && index == count - 1 -> vm.exitList(block.id)
                                else -> vm.splitListItem(block.id, index, before, after)
                            }
                        } else {
                            vm.setListItem(block.id, index, raw)
                        }
                    }
                    report(new.selection)
                },
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                visualTransformation = transformation,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(requester)
                    .onFocusChanged {
                        if (it.isFocused) {
                            vm.focusedId = block.id
                            report(shown.selection)
                        } else if (toolbar?.linkMode != true) {
                            vm.clearSelection(block.id)
                        }
                    }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace && value.isEmpty()) {
                            vm.removeListItem(block.id, index)
                            true
                        } else {
                            false
                        }
                    },
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty() && index == 0) {
                            Text(
                                text = hintOf(block.type),
                                style = style.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                            )
                        }
                        inner()
                    }
                }
            )
        }
    }
}

// ───────────────────────── بدنه‌ی بلوک‌ها ─────────────────────────

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
            BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview, formattable = type.supportsInline) { new ->
                when {
                    type == BlockType.PARAGRAPH && vm.autoFormat(block.id, new) -> Unit
                    splitsOnEnter && new.contains('\n') -> vm.splitAtNewline(block.id, new)
                    else -> vm.updateText(block.id, new)
                }
            }
        }

        BlockEditing.LIST -> ListEditor(block = block, vm = vm, style = style, preview = preview)

        BlockEditing.QUOTE -> {
            val barColor = scheme.onSurface.copy(alpha = 0.4f)
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
                BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview, formattable = type.supportsInline) {
                    // نقل‌قول معمولی: اینتر دوم از نقل‌قول بیرون میاره (مثل وردپرس)؛ نقل‌قول برجسته همون رفتار قبلی
                    if (type == BlockType.QUOTE) vm.quoteChanged(block.id, it) else vm.updateText(block.id, it)
                }
            }
        }

        BlockEditing.CODE -> {
            if (isRaw(block) && (preview || !focused)) {
                RawPreview(block = block)
            } else {
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
        }

        BlockEditing.IMAGE -> ImageBody(block = block, vm = vm, preview = preview, focused = focused)

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
