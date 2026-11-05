package com.appdony.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.appdony.app.data.model.CustomBox
import com.appdony.app.data.model.DlLink
import com.appdony.app.ui.components.glassTrack

/** بخش‌هایی که از دکمه‌ی کناری باز می‌شن ([short] = برچسب کوتاه زیر آیکن توی قرص عمودی) */
enum class SidePanel(val label: String, val short: String, val icon: ImageVector) {
    FEATURED("تصویر شاخص", "تصویر شاخص", Icons.Outlined.AddPhotoAlternate),
    SETTINGS("تنظیمات نوشته", "تنظیمات", Icons.Outlined.Tune),
    APP_INFO("اطلاعات برنامه / بازی", "اطلاعات برنامه", Icons.Outlined.Android),
    SEO("Yoast SEO", "سئو", Icons.Outlined.Search)
}

// ───────────────────────── دکمه‌ی کناری (قرص عمودی لبه‌ی راست، وسط) ─────────────────────────

/**
 * مثل کشوی پایین، فقط عمودی: «دسته» سر منوئه.
 * بسته: فقط دسته‌ی باریک چسبیده به لبه‌ی راست.
 * باز: نوار ابزارها از لبه‌ی راست میاد جای دسته و دسته رو با خودش به سمت وسط صفحه هل می‌ده
 * (دسته می‌ره کنار منو و فلشش برمی‌گرده)؛ موقع بستن دقیقاً برعکس: نوار به لبه‌ی راست جمع می‌شه
 * و دسته برمی‌گرده جای اول. زدن روی هر ابزار پنل اون بخش رو باز می‌کنه.
 */
@Composable
internal fun SideDock(
    menuOpen: Boolean,
    onHandle: () -> Unit,
    onPick: (SidePanel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // اول (لبه‌ی راست): نوار ابزارها؛ از لبه‌ی راست باز می‌شه و دسته رو هل می‌ده
        AnimatedVisibility(
            visible = menuOpen,
            enter = expandHorizontally(tween(280), expandFrom = Alignment.Start) + fadeIn(tween(220)),
            exit = shrinkHorizontally(tween(240), shrinkTowards = Alignment.Start) + fadeOut(tween(160))
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .width(76.dp)
                    .glassTrack(RoundedCornerShape(22.dp))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                SidePanel.entries.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(item) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(item.icon, contentDescription = item.label, tint = scheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.short,
                            color = scheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        // بعدش: دسته (سرِ منو)؛ بسته لبه‌ی راسته، باز کنار نوار ابزار و سمت وسط صفحه
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(64.dp)
                .glassTrack(RoundedCornerShape(12.dp))
                .clickable(onClick = onHandle),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (menuOpen) Icons.Outlined.ChevronRight else Icons.Outlined.ChevronLeft,
                contentDescription = if (menuOpen) "بستن منو" else "باز کردن منو",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ───────────────────────── پنل بازشونده (وسط صفحه؛ از راست میاد و به راست برمی‌گرده) ─────────────────────────

@Composable
internal fun SidePanelHost(
    panel: SidePanel?,
    vm: EditorViewModel,
    isBlog: Boolean,
    onPickImage: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    var last by remember { mutableStateOf(panel) }
    SideEffect { if (panel != null && last != panel) last = panel }
    val shown = panel ?: last
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.74f).dp

    AnimatedVisibility(
        visible = panel != null,
        modifier = modifier,
        enter = slideInHorizontally(tween(280)) { it } + fadeIn(tween(220)),
        exit = slideOutHorizontally(tween(240)) { it } + fadeOut(tween(180))
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(shape)
                .background(scheme.background)
                .glassTrack(shape)
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shown != null) {
                    Icon(shown.icon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = shown.label,
                        color = scheme.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                MiniButton(Icons.Outlined.Close, "بستن", onClick = onClose)
            }
            HorizontalDivider(color = scheme.onSurface.copy(alpha = 0.12f))
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight - 52.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (shown) {
                    SidePanel.FEATURED -> FeaturedBody(vm = vm, onPick = onPickImage)
                    SidePanel.SETTINGS -> SettingsBody(vm = vm, isBlog = isBlog)
                    SidePanel.APP_INFO -> AppInfoBody(vm = vm)
                    SidePanel.SEO -> SeoBody(vm = vm)
                    null -> Unit
                }
            }
        }
    }
}

// ───────────────────────── ابزارهای فرم ─────────────────────────

@Composable
private fun PField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    ltr: Boolean = false,
    lines: Int = 1,
    hint: String = ""
) {
    val scheme = MaterialTheme.colorScheme
    val style = TextStyle(
        color = scheme.onSurface,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content
    )
    Column(modifier = modifier) {
        Text(
            text = label,
            color = scheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.onSurface.copy(alpha = 0.06f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = lines == 1,
                textStyle = style,
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = (lines * 22).dp),
                decorationBox = { inner ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        if (value.isEmpty() && hint.isNotEmpty()) {
                            Text(text = hint, style = style.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.5f)))
                        }
                        inner()
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun PChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) scheme.primary.copy(alpha = 0.22f) else scheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ───────────────────────── تصویر شاخص ─────────────────────────

@Composable
private fun FeaturedBody(vm: EditorViewModel, onPick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val url = vm.featuredUrl
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(14.dp))
            .background(scheme.onSurface.copy(alpha = 0.06f))
            .clickable(enabled = url == null && !vm.uploading, onClick = onPick),
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
    if (url != null && !vm.uploading) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPick) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("تغییر تصویر")
            }
            TextButton(onClick = { vm.clearFeatured() }) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = scheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("حذف تصویر", color = scheme.error)
            }
        }
    }
}

// ───────────────────────── تنظیمات نوشته ─────────────────────────

@Composable
private fun SettingsBody(vm: EditorViewModel, isBlog: Boolean) {
    val ctx = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val s = vm.settings

    LaunchedEffect(isBlog) { if (!isBlog) vm.loadCategories(ctx) }

    PField("نامک (آدرس مطلب)", s.slug, { vm.settings = vm.settings.copy(slug = it) }, ltr = true, hint = "my-post-name")
    PField(
        "تاریخ انتشار (خالی = همین حالا)",
        s.date,
        { vm.settings = vm.settings.copy(date = it) },
        ltr = true,
        hint = "2026-11-05 10:30"
    )
    PField("چکیده", s.excerpt, { vm.settings = vm.settings.copy(excerpt = it) }, lines = 3)
    SwitchRow("دیدگاه‌ها باز باشه", s.commentsOpen) { vm.settings = vm.settings.copy(commentsOpen = it) }

    if (!isBlog) {
        SectionTitle("دسته‌ها")
        when {
            vm.loadingCategories -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            vm.categories.isEmpty() -> Text("دسته‌ای پیدا نشد", color = scheme.onSurfaceVariant, fontSize = 12.sp)
            else -> vm.categories.forEach { cat ->
                val selected = cat.id in s.categoryIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            val now = vm.settings
                            vm.settings = now.copy(
                                categoryIds = if (selected) now.categoryIds - cat.id else now.categoryIds + cat.id
                            )
                        }
                        .padding(start = if (cat.parent != 0) 20.dp else 0.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (selected) scheme.primary else scheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(text = cat.name, color = scheme.onSurface, fontSize = 13.sp)
                }
            }
        }
    }
}

// ───────────────────────── اطلاعات برنامه / بازی ─────────────────────────

@Composable
private fun AppInfoBody(vm: EditorViewModel) {
    val scheme = MaterialTheme.colorScheme
    val a = vm.appInfo

    SectionTitle("مشخصات")
    PField("توضیح کوتاه", a.descripcion, { vm.appInfo = vm.appInfo.copy(descripcion = it) })
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PField("نسخه", a.version, { vm.appInfo = vm.appInfo.copy(version = it) }, modifier = Modifier.weight(1f), ltr = true)
        PField("حجم", a.tamano, { vm.appInfo = vm.appInfo.copy(tamano = it) }, modifier = Modifier.weight(1f), ltr = true)
    }
    PField("تاریخ به‌روزرسانی", a.fechaActualizacion, { vm.appInfo = vm.appInfo.copy(fechaActualizacion = it) }, ltr = true, hint = "1405/6/4")
    PField("نیازمندی‌ها (اندروید)", a.requerimientos, { vm.appInfo = vm.appInfo.copy(requerimientos = it) }, ltr = true, hint = "5+")
    PField("لینک گوگل‌پلی", a.consiguelo, { vm.appInfo = vm.appInfo.copy(consiguelo = it) }, ltr = true)
    PField("تازه‌ها (HTML)", a.novedades, { vm.appInfo = vm.appInfo.copy(novedades = it) }, lines = 4)

    SectionTitle("لینک‌های دانلود")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PChip("دانلود مستقیم", a.dlOption == "direct-download") { vm.appInfo = vm.appInfo.copy(dlOption = "direct-download") }
        PChip("چند لینک", a.dlOption == "links") { vm.appInfo = vm.appInfo.copy(dlOption = "links") }
    }
    if (a.dlOption == "direct-download") {
        PField("آدرس دانلود مستقیم", a.directDownload, { vm.appInfo = vm.appInfo.copy(directDownload = it) }, ltr = true, hint = "https://…")
    } else {
        a.dlLinks.forEachIndexed { i, row ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.onSurface.copy(alpha = 0.04f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PField("برچسب", row.texto, { v -> vm.appInfo = vm.appInfo.copy(dlLinks = vm.appInfo.dlLinks.set(i) { it.copy(texto = v) }) })
                PField("لینک فایل", row.link, { v -> vm.appInfo = vm.appInfo.copy(dlLinks = vm.appInfo.dlLinks.set(i) { it.copy(link = v) }) }, ltr = true)
                PField("صفحه‌ی دانلود", row.page, { v -> vm.appInfo = vm.appInfo.copy(dlLinks = vm.appInfo.dlLinks.set(i) { it.copy(page = v) }) }, ltr = true)
                TextButton(onClick = {
                    val rest = vm.appInfo.dlLinks.filterIndexed { index, _ -> index != i }
                    vm.appInfo = vm.appInfo.copy(dlLinks = rest.ifEmpty { listOf(DlLink()) })
                }) { Text("حذف این لینک", color = scheme.error) }
            }
        }
        TextButton(onClick = { vm.appInfo = vm.appInfo.copy(dlLinks = vm.appInfo.dlLinks + DlLink()) }) {
            Text("افزودن لینک")
        }
    }

    SectionTitle("رسانه")
    PField("آدرس اسکرین‌شات‌ها (هر خط یک URL)", a.screenshots, { vm.appInfo = vm.appInfo.copy(screenshots = it) }, ltr = true, lines = 4)
    PField("شناسه‌ی ویدیو یوتیوب", a.videoId, { vm.appInfo = vm.appInfo.copy(videoId = it) }, ltr = true, hint = "dQw4w9WgXcQ")

    SectionTitle("باکس‌های سفارشی")
    a.boxes.forEachIndexed { i, box ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.onSurface.copy(alpha = 0.04f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PField("عنوان", box.title, { v -> vm.appInfo = vm.appInfo.copy(boxes = vm.appInfo.boxes.set(i) { it.copy(title = v) }) })
            PField("محتوا (HTML مجاز)", box.content, { v -> vm.appInfo = vm.appInfo.copy(boxes = vm.appInfo.boxes.set(i) { it.copy(content = v) }) }, lines = 3)
            TextButton(onClick = {
                val rest = vm.appInfo.boxes.filterIndexed { index, _ -> index != i }
                vm.appInfo = vm.appInfo.copy(boxes = rest.ifEmpty { listOf(CustomBox()) })
            }) { Text("حذف این باکس", color = scheme.error) }
        }
    }
    TextButton(onClick = { vm.appInfo = vm.appInfo.copy(boxes = vm.appInfo.boxes + CustomBox()) }) {
        Text("افزودن باکس")
    }

    SectionTitle("امتیاز نمایشی")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PField("میانگین", a.ratingAverage, { vm.appInfo = vm.appInfo.copy(ratingAverage = it) }, modifier = Modifier.weight(1f), ltr = true, hint = "4.2")
        PField("تعداد", a.ratingCount, { vm.appInfo = vm.appInfo.copy(ratingCount = it) }, modifier = Modifier.weight(1f), ltr = true)
    }

    SectionTitle("اپدونی پلاس و منبع")
    SwitchRow("این اپ مخصوص اعضای اپدونی پلاس است", a.appdoniPlus) { vm.appInfo = vm.appInfo.copy(appdoniPlus = it) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PChip("خودمان", a.contentSource == "own") { vm.appInfo = vm.appInfo.copy(contentSource = "own") }
        PChip("5play", a.contentSource == "5play") { vm.appInfo = vm.appInfo.copy(contentSource = "5play") }
        PChip("سایر", a.contentSource == "other") { vm.appInfo = vm.appInfo.copy(contentSource = "other") }
    }
    if (a.contentSource == "other") {
        PField("نام منبع", a.contentSourceCustom, { vm.appInfo = vm.appInfo.copy(contentSourceCustom = it) }, ltr = true, hint = "happymod")
    }
}

private fun <T> List<T>.set(index: Int, change: (T) -> T): List<T> =
    mapIndexed { i, item -> if (i == index) change(item) else item }

// ───────────────────────── Yoast SEO ─────────────────────────

@Composable
private fun SeoBody(vm: EditorViewModel) {
    val scheme = MaterialTheme.colorScheme
    val s = vm.seo
    PField("کلمه‌ی کلیدی کانونی", s.focusKeyword, { vm.seo = vm.seo.copy(focusKeyword = it) })
    PField("عنوان سئو", s.title, { vm.seo = vm.seo.copy(title = it) })
    Text("${s.title.length} / 60 نویسه", color = scheme.onSurfaceVariant, fontSize = 11.sp)
    PField("توضیحات متا", s.description, { vm.seo = vm.seo.copy(description = it) }, lines = 3)
    Text("${s.description.length} / 160 نویسه", color = scheme.onSurfaceVariant, fontSize = 11.sp)
}
