package com.appdony.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import com.appdony.app.data.model.InlineFormat
import com.appdony.app.data.model.InlineSpan
import com.appdony.app.data.model.SpanKind
import java.util.TreeSet

/** متن + قالب‌بندی‌ها → متن رنگی و استایل‌دار (بولد، کج، خط‌خورده، لینک)؛ [highlight] بازه‌ی در حال لینک شدنه */
internal fun styledText(
    text: String,
    spans: List<InlineSpan>,
    linkColor: Color,
    highlight: IntRange? = null,
    highlightColor: Color = Color.Unspecified
): AnnotatedString {
    val clean = InlineFormat.clip(spans, text.length)
    if (clean.isEmpty() && highlight == null) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        val cuts = TreeSet<Int>()
        cuts.add(0)
        cuts.add(text.length)
        clean.forEach {
            cuts.add(it.start)
            cuts.add(it.end)
        }
        val points = cuts.toList()
        for (k in 0 until points.size - 1) {
            val a = points[k]
            val b = points[k + 1]
            val active = clean.filter { it.start <= a && it.end >= b }
            if (active.isEmpty()) continue
            val kinds = active.map { it.kind }
            val lines = ArrayList<TextDecoration>()
            if (SpanKind.STRIKE in kinds) lines.add(TextDecoration.LineThrough)
            if (SpanKind.LINK in kinds) lines.add(TextDecoration.Underline)
            addStyle(
                SpanStyle(
                    fontWeight = if (SpanKind.BOLD in kinds) FontWeight.Bold else null,
                    fontStyle = if (SpanKind.ITALIC in kinds) FontStyle.Italic else null,
                    textDecoration = if (lines.isEmpty()) null else TextDecoration.combine(lines),
                    color = if (SpanKind.LINK in kinds) linkColor else Color.Unspecified
                ),
                a,
                b
            )
        }
        if (highlight != null) {
            val a = highlight.first.coerceIn(0, text.length)
            val b = (highlight.last + 1).coerceIn(0, text.length)
            if (b > a) addStyle(SpanStyle(background = highlightColor), a, b)
        }
    }
}

/** قالب‌بندی‌ها رو روی متن داخل فیلد نشون می‌ده بدون اینکه متن یا جای مکان‌نما عوض بشه */
internal class SpanTransformation(
    private val spans: List<InlineSpan>,
    private val linkColor: Color,
    private val highlight: IntRange?,
    private val highlightColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            styledText(text.text, spans, linkColor, highlight, highlightColor),
            OffsetMapping.Identity
        )
}

/**
 * قالب‌بندی‌های یک فیلد. [offset] جای شروع این فیلد توی متن کل بلوکه (برای موردهای فهرست)
 * و [length] طول متن همین فیلد.
 */
@Composable
internal fun rememberSpanTransformation(
    vm: EditorViewModel,
    blockId: Long,
    spans: List<InlineSpan>,
    offset: Int,
    length: Int
): VisualTransformation {
    val scheme = MaterialTheme.colorScheme
    val linkColor = scheme.primary
    val highlightColor = scheme.primary.copy(alpha = 0.30f)
    val target = vm.linkTarget?.takeIf { it.blockId == blockId }
    val a = target?.start ?: -1
    val b = target?.end ?: -1
    return remember(spans, offset, length, linkColor, a, b) {
        val local = InlineFormat.slice(spans, offset, offset + length)
        val range = if (target != null && b > offset && a < offset + length) {
            (maxOf(a, offset) - offset) until (minOf(b, offset + length) - offset)
        } else {
            null
        }
        SpanTransformation(local, linkColor, range, highlightColor)
    }
}
