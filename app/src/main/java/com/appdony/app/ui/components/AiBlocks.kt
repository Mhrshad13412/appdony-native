package com.appdony.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.preferences.AiModel
import com.appdony.app.data.preferences.AiQuota
import com.appdony.app.data.preferences.formatRemaining
import kotlinx.coroutines.delay

// ───────────────────────── تجزیه‌ی پیام به متن و کد ─────────────────────────

sealed interface MsgPart {
    data class Text(val text: String) : MsgPart
    data class Code(val lang: String, val code: String) : MsgPart
}

fun parseMessageParts(raw: String): List<MsgPart> {
    val parts = mutableListOf<MsgPart>()
    fun addText(t: String) {
        val s = t.trim('\n')
        if (s.isNotBlank()) parts += MsgPart.Text(s)
    }

    var i = 0
    while (i < raw.length) {
        val start = raw.indexOf("```", i)
        if (start < 0) {
            addText(raw.substring(i))
            break
        }
        addText(raw.substring(i, start))

        val lineEnd = raw.indexOf('\n', start + 3)
        var lang = ""
        var bodyStart = start + 3
        if (lineEnd >= 0) {
            val first = raw.substring(start + 3, lineEnd).trim()
            if (first.length <= 15 && !first.contains(' ')) {
                lang = first
                bodyStart = lineEnd + 1
            }
        }
        val end = raw.indexOf("```", bodyStart)
        val body = if (end < 0) raw.substring(bodyStart) else raw.substring(bodyStart, end)
        parts += MsgPart.Code(lang, body.trimEnd('\n'))
        i = if (end < 0) raw.length else end + 3
    }
    return parts
}

fun hasCodeBlock(raw: String): Boolean = raw.contains("```")

/** **بولد** و `کد داخل خط` رو ساده نشون می‌ده */
private fun inlineStyled(text: String, codeColor: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        if (text.startsWith("**", i)) {
            val e = text.indexOf("**", i + 2)
            if (e > 0) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, e)) }
                i = e + 2
                continue
            }
        }
        if (text[i] == '`') {
            val e = text.indexOf('`', i + 1)
            if (e > 0) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, color = codeColor)) {
                    append(text.substring(i + 1, e))
                }
                i = e + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}

// ───────────────────────── محتوای پیام + بلاک کد ─────────────────────────

@Composable
fun AiMessageContent(text: String, isUser: Boolean) {
    val textColor = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
    val inlineBg = if (isUser) Color.Black.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val parts = remember(text) { parseMessageParts(text) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEach { part ->
            when (part) {
                is MsgPart.Text -> Text(
                    text = inlineStyled(part.text, textColor, inlineBg),
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 23.sp
                )
                is MsgPart.Code -> AiCodeBlock(part.lang, part.code)
            }
        }
    }
}

@Composable
fun AiCodeBlock(lang: String, code: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1B1D24))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF262A34))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lang.ifBlank { "code" },
                    color = Color(0xFF9AA3B2),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(code))
                            copied = true
                            Toast.makeText(context, "کد کپی شد", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                        contentDescription = "کپی",
                        tint = if (copied) Color(0xFF34C759) else Color(0xFF9AA3B2),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (copied) "کپی شد" else "کپی",
                        color = if (copied) Color(0xFF34C759) else Color(0xFF9AA3B2),
                        fontSize = 11.sp
                    )
                }
            }
            Text(
                text = code,
                color = Color(0xFFE6E8EE),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                softWrap = false,
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            )
        }
    }
}

// ───────────────────────── انتخاب مدل ─────────────────────────

@Composable
fun AiModelSelector(selected: AiModel, enabled: Boolean, onSelect: (AiModel) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AiModel.values().forEach { m ->
            val isSelected = m == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        BorderStroke(1.dp, if (isSelected) primary else Color.Transparent),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = enabled && !isSelected) { onSelect(m) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    m.label,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    m.hint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ───────────────────────── کارت سهم مصرف ─────────────────────────

@Composable
fun AiQuotaCard(quota: AiQuota?, now: Long) {
    if (quota == null) return
    val m = quota.model
    val blocked = quota.blockReason(now) != null
    val resetIn = quota.windowResetAt - now
    val windowActive = resetIn > 0
    val usedMsgs = if (windowActive) quota.usedMessages else 0
    val usedTokens = if (windowActive) quota.usedTokens else 0
    val color = if (blocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "پیام این بازه: $usedMsgs از ${m.windowMessages}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "امروز: ${quota.dailyUsed} از ${m.dailyMessages}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (usedMsgs.toFloat() / m.windowMessages).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.18f)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (windowActive)
                "توکن: $usedTokens از ${m.windowTokens} • ریست بازه: ${formatRemaining(resetIn)} دیگه"
            else
                "با اولین پیامت، بازه‌ی ۵ ساعته شروع می‌شه",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}

// ───────────────────────── بلاک پیشنهاد برنامه‌ها ─────────────────────────

@Composable
fun AiSuggestionBlock(
    posts: List<Post>,
    onPostClick: (Int) -> Unit,
    onRefresh: () -> Unit
) {
    if (posts.isEmpty()) return
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "پیشنهاد پاپی برای تو 🐾",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .clickable { onRefresh() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Refresh, "پیشنهاد جدید", tint = primary, modifier = Modifier.size(20.dp))
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                Column(
                    modifier = Modifier
                        .width(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onPostClick(post.id) }
                ) {
                    val img = post.featuredImageUrl()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .background(primary.copy(alpha = 0.10f))
                    ) {
                        if (img != null) {
                            AsyncImage(
                                model = img,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(84.dp)
                            )
                        }
                    }
                    Text(
                        text = shortSuggestionTitle(post.cleanTitle()),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

private fun shortSuggestionTitle(title: String): String {
    val quoted = Regex("[“”\"«»](.+?)[“”\"«»]").find(title)?.groupValues?.getOrNull(1)?.trim()
    if (!quoted.isNullOrBlank()) return quoted
    return title.removePrefix("دانلود").trim()
}
