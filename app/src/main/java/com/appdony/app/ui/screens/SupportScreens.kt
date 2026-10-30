package com.appdony.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.api.SupportApi
import com.appdony.app.data.api.SupportMessage
import com.appdony.app.ui.components.CatAvatar
import com.appdony.app.ui.components.SupportAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val IDLE_SECONDS = 120

/** جمله‌هایی که پشتیبان هر ۱۰ ثانیه یکی‌شون رو می‌گه */
private val SupportPhrases = listOf(
    "سلام! من پشتیبانم، هر مشکلی داری همین‌جا بنویس 👋",
    "داری چی می‌گی؟ دارم با ذره‌بین نگات می‌کنم 🔍",
    "مشکل نصب یا دانلود داری؟ توضیح بده تا حلش کنیم.",
    "اسم بازی یا برنامه‌ای که مشکل داره رو هم بنویس.",
    "من همین‌جام، عجله نکن؛ پیامت رو که بفرستی جواب می‌دم.",
    "اگه ایده یا پیشنهادی برای اپدونی داری، با کمال میل می‌شنوم 💡",
    "پیام‌هات همین‌جا می‌مونه؛ هر وقت برگردی می‌تونی ادامه بدی.",
    "یه چیزی توی ذهنته؟ بنویس، من که حوصله‌م سر نمی‌ره 😄"
)

// ───────────────────────── چت پشتیبانی ─────────────────────────

/**
 * [asAdmin] = false: کاربر عادی با پشتیبان. true: پشتیبان توی گفتگوی یه کاربر.
 * [thread] شناسه‌ی گفتگو (مثلاً u12).
 */
@Composable
fun SupportChatScreen(
    thread: String,
    title: String,
    myName: String,
    asAdmin: Boolean,
    onBack: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<SupportMessage>()) }
    var lastId by remember { mutableLongStateOf(0L) }
    var error by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    // ── همگام‌سازی پیام‌ها هر ۳ ثانیه
    suspend fun refresh() {
        SupportApi.fetch(thread, lastId)
            .onSuccess { new ->
                error = null
                if (new.isNotEmpty()) {
                    messages = messages + new
                    lastId = new.last().id
                }
            }
            .onFailure { error = it.message }
    }

    LaunchedEffect(thread) {
        if (!asAdmin) SupportApi.registerUser(thread, myName)
        while (true) {
            refresh()
            delay(3000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // ── تایمر بیکاری: ۲ دقیقه بدون پیام ← پشتیبان میره پایین، گربه میاد بالا
    var activity by remember { mutableIntStateOf(0) }
    var remaining by remember { mutableIntStateOf(IDLE_SECONDS) }
    val idle = !asAdmin && remaining <= 0

    LaunchedEffect(activity, asAdmin) {
        if (asAdmin) return@LaunchedEffect
        remaining = IDLE_SECONDS
        while (remaining > 0) {
            delay(1000)
            remaining--
        }
    }

    // ── حرف زدن هر ۱۰ ثانیه + نگاه با ذره‌بین
    val look = remember { Animatable(0f) }
    var bubble by remember { mutableStateOf<String?>(null) }
    var talking by remember { mutableStateOf(false) }
    LaunchedEffect(idle, asAdmin) {
        if (asAdmin || idle) {
            bubble = null; talking = false; look.snapTo(0f)
            return@LaunchedEffect
        }
        while (true) {
            delay(10_000)
            val phrase = SupportPhrases.random()
            look.animateTo(1f, tween(600))
            bubble = phrase
            talking = true
            delay((phrase.length * 55L).coerceIn(1800L, 4000L))
            talking = false
            look.animateTo(0f, tween(600))
            delay(1200)
            bubble = null
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isBlank() || sending) return
        input = ""
        sending = true
        activity++
        scope.launch {
            SupportApi.send(
                thread = thread,
                sender = if (asAdmin) "support" else "user",
                userName = if (asAdmin) "پشتیبان" else myName,
                body = text
            ).onFailure { error = it.message; input = text }
            sending = false
            refresh()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // ── هدر
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))

            // پشتیبان و گربه توی یه پنجره‌ی گرد؛ یکی میره پایین، اون یکی میاد بالا
            val slide = remember { Animatable(0f) }
            LaunchedEffect(idle) {
                slide.animateTo(if (idle) 1f else 0f, spring(0.6f, Spring.StiffnessLow))
            }
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (asAdmin) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(title.take(1), color = primary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(modifier = Modifier.offset(y = (slide.value * 70f).dp)) {
                        SupportAvatar(sizeDp = 54, talking = talking, look = look.value)
                    }
                    Box(
                        modifier = Modifier
                            .offset(y = ((1f - slide.value) * -70f).dp)
                            .clickable(enabled = idle) { activity++ }
                    ) {
                        CatAvatar(sizeDp = 52, hasShadow = false)
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (asAdmin) title else "پشتیبانی",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    when {
                        asAdmin -> "گفتگو با کاربر"
                        idle -> "منتظرم... گربه جام رو گرفته 🐱"
                        else -> "پاسخ‌گوی شما"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            if (!asAdmin) IdleTimer(remaining = remaining, total = IDLE_SECONDS)
        }

        // ── حباب حرف پشتیبان
        if (!asAdmin && bubble != null) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.14f))
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(bubble.orEmpty(), color = MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, lineHeight = 21.sp)
            }
        }

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }

        // ── پیام‌ها
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && error == null) {
                item {
                    Text(
                        if (asAdmin) "هنوز پیامی نیست." else "هنوز پیامی نفرستادی؛ اولین پیام رو بنویس 👇",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            items(messages, key = { it.id }) { msg ->
                val mine = if (asAdmin) msg.sender == "support" else msg.sender == "user"
                SupportBubble(msg.body, mine)
            }
        }

        // ── نوار نوشتن
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("پیامت رو بنویس...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                shape = RoundedCornerShape(26.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = primary
                )
            )
            Spacer(Modifier.width(8.dp))
            val canSend = input.isNotBlank() && !sending
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canSend) { send() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "ارسال",
                    tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/** تایمر گرد: حلقه‌ی پیشرفت + زمان باقی‌مونده (mm:ss) */
@Composable
private fun IdleTimer(remaining: Int, total: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val low = remaining in 1..15
    val color = if (remaining <= 0) MaterialTheme.colorScheme.onSurfaceVariant else if (low) Color(0xFFF5A623) else primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp)) {
            CircularProgressIndicator(
                progress = { remaining.coerceAtLeast(0) / total.toFloat() },
                modifier = Modifier.size(26.dp),
                color = color,
                trackColor = color.copy(alpha = 0.18f),
                strokeWidth = 3.dp
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "%d:%02d".format(remaining.coerceAtLeast(0) / 60, remaining.coerceAtLeast(0) % 60),
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SupportBubble(text: String, mine: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 290.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp, topEnd = 18.dp,
                        bottomStart = if (mine) 18.dp else 5.dp,
                        bottomEnd = if (mine) 5.dp else 18.dp
                    )
                )
                .background(if (mine) primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text,
                color = if (mine) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 23.sp
            )
        }
    }
}

// ───────────────────────── لیست کاربرها (فقط ادمین) ─────────────────────────

private data class AdminRow(val thread: String, val name: String, val last: String, val lastId: Long)

@Composable
fun SupportAdminScreen(
    onOpen: (thread: String, name: String) -> Unit,
    onBack: () -> Unit
) {
    var rows by remember { mutableStateOf<List<AdminRow>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val users = SupportApi.users()
            val threads = SupportApi.threads()
            if (users.isFailure && threads.isFailure) {
                error = users.exceptionOrNull()?.message
            } else {
                error = null
                val byThread = threads.getOrNull().orEmpty().associateBy { it.thread }
                val merged = LinkedHashMap<String, AdminRow>()
                users.getOrNull().orEmpty().forEach { u ->
                    val t = byThread[u.thread]
                    merged[u.thread] = AdminRow(u.thread, u.userName.ifBlank { u.thread }, t?.lastBody.orEmpty(), t?.lastId ?: 0L)
                }
                // گفتگوهایی که کاربرشون توی جدول کاربرها نیست
                byThread.values.forEach { t ->
                    if (t.thread !in merged) merged[t.thread] = AdminRow(t.thread, t.userName, t.lastBody, t.lastId)
                }
                rows = merged.values.sortedByDescending { it.lastId }
            }
            delay(5000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("پنل پشتیبانی", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${rows?.size ?: 0} کاربر",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }

        val list = rows
        if (list == null && error == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (list != null && list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز کاربری ثبت‌نام نکرده.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (list != null) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.thread }) { row ->
                    val primary = MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onOpen(row.thread, row.name) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(primary.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(row.name.take(1), color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                row.last.ifBlank { "هنوز پیامی نفرستاده" },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
