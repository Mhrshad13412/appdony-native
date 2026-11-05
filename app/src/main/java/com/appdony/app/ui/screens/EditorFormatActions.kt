package com.appdony.app.ui.screens

import com.appdony.app.data.model.InlineFormat
import com.appdony.app.data.model.SpanKind

/** بازه‌ی انتخاب‌شده توی یک بلوک؛ موقعیت‌ها نسبت به متن کل بلوکه */
data class FormatSel(val blockId: Long, val start: Int, val end: Int)

/** فیلد متنی انتخابش رو خبر می‌ده تا دکمه‌های بولد، کج، خط‌خورده و لینک بدونن روی چی کار کنن */
fun EditorViewModel.reportSelection(blockId: Long, start: Int, end: Int) {
    val cur = fmtSel
    if (cur == null || cur.blockId != blockId || cur.start != start || cur.end != end) {
        fmtSel = FormatSel(blockId, start, end)
    }
}

fun EditorViewModel.clearSelection(blockId: Long) {
    if (fmtSel?.blockId == blockId) fmtSel = null
}

/** بولد، کج یا خط‌خورده رو روی انتخاب روشن یا خاموش می‌کنه */
fun EditorViewModel.toggleFormat(kind: SpanKind) {
    val sel = fmtSel ?: return
    val i = blocks.indexOfFirst { it.id == sel.blockId }
    if (i < 0) return
    val block = blocks[i]
    val len = block.text.length
    val a = sel.start.coerceIn(0, len)
    val b = sel.end.coerceIn(0, len)
    if (b <= a) return
    val clean = InlineFormat.clip(block.spans, len)
    blocks[i] = block.copy(spans = InlineFormat.toggle(clean, kind, a, b))
}

/** لینک رو روی بازه‌ای که موقع زدن دکمه‌ی لینک انتخاب بود می‌ذاره؛ آدرس خالی = برداشتن لینک */
fun EditorViewModel.applyLink(raw: String) {
    val sel = linkTarget ?: return
    val i = blocks.indexOfFirst { it.id == sel.blockId }
    if (i < 0) return
    val block = blocks[i]
    val len = block.text.length
    val a = sel.start.coerceIn(0, len)
    val b = sel.end.coerceIn(0, len)
    if (b <= a) return
    val url = normalizeUrl(raw)
    val clean = InlineFormat.clip(block.spans, len)
    blocks[i] = block.copy(
        spans = if (url.isEmpty()) {
            InlineFormat.remove(clean, SpanKind.LINK, a, b)
        } else {
            InlineFormat.setLink(clean, a, b, url)
        }
    )
}

private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
private val UNSAFE = setOf("javascript:", "data:", "vbscript:")

/** آدرس بدون پروتکل رو https می‌کنه و آدرس‌های خطرناک (javascript: و ...) رو رد می‌کنه */
fun normalizeUrl(raw: String): String {
    val t = raw.trim().replace(" ", "")
    if (t.isEmpty()) return ""
    val lower = t.lowercase()
    if (UNSAFE.any { lower.startsWith(it) }) return ""
    if (SCHEME.containsMatchIn(t) || t.startsWith("/") || t.startsWith("#")) return t
    return "https://$t"
}
