package com.appdony.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.model.BlockEditing
import com.appdony.app.data.model.BlockType
import com.appdony.app.data.model.EditorBlock

// ───────────────────────── بلوک آزاد (مثل وردپرس: بدون کادر و رنگ) ─────────────────────────

@Composable
internal fun BlockCard(block: EditorBlock, index: Int, count: Int, vm: EditorViewModel, preview: Boolean) {
    val focused = !preview && vm.focusedId == block.id
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
        if (focused) BlockToolbar(block = block, index = index, count = count, vm = vm)
        BlockBody(block = block, vm = vm, preview = preview, focused = focused)
    }
}

/** نوار ابزار کوچیک و خنثی فقط برای بلوک انتخاب‌شده */
@Composable
private fun BlockToolbar(block: EditorBlock, index: Int, count: Int, vm: EditorViewModel) {
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
            text = block.type.label,
            color = scheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        MiniButton(Icons.Outlined.ArrowUpward, "بالا", enabled = index > 0) { vm.move(block.id, -1) }
        MiniButton(Icons.Outlined.ArrowDownward, "پایین", enabled = index < count - 1) { vm.move(block.id, 1) }
        MiniButton(Icons.Outlined.Delete, "حذف", tint = scheme.error) { vm.remove(block.id) }
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

// ───────────────────────── فهرست واقعی ─────────────────────────

/** هر مورد یه فیلد جدا با نقطه/شماره؛ اینتر = مورد بعدی، اینتر روی مورد خالیِ آخر = خروج از فهرست */
@Composable
private fun ListEditor(block: EditorBlock, vm: EditorViewModel, style: TextStyle, preview: Boolean) {
    val items = block.text.split('\n')
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.forEachIndexed { index, item ->
            key(index) {
                ListItemRow(
                    block = block,
                    vm = vm,
                    index = index,
                    count = items.size,
                    value = item,
                    style = style,
                    preview = preview
                )
            }
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
    style: TextStyle,
    preview: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val requester = remember { FocusRequester() }

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
            Text(text = value, style = style, modifier = Modifier.weight(1f))
        } else {
            BasicTextField(
                value = value,
                onValueChange = { raw ->
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
                },
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(requester)
                    .onFocusChanged { if (it.isFocused) vm.focusedId = block.id }
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
            BlockField(vm = vm, block = block, value = block.text, hint = hintOf(type), style = style, readOnly = preview) { new ->
                if (splitsOnEnter && new.contains('\n')) vm.splitAtNewline(block.id, new) else vm.updateText(block.id, new)
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
