package com.appdony.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.ui.components.AiMessageContent
import com.appdony.app.ui.components.AiModelSelector
import com.appdony.app.ui.components.AiQuotaCard
import com.appdony.app.ui.components.AiSuggestionBlock
import com.appdony.app.ui.components.PlagueDoctorAvatar
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.hasCodeBlock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val QuickReplies = listOf(
    "یه بازی پیشنهاد بده 🎮",
    "نصب نمیشه، کمک کن",
    "دانلود گیر کرده",
    "یه برنامه‌ی خوب معرفی کن"
)

@Composable
fun AiChatScreen(
    onBack: () -> Unit,
    onSupportClick: (isAdmin: Boolean) -> Unit = {},
    onPostClick: (Int) -> Unit = {},
    viewModel: AiChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val pendingImage by viewModel.pendingImage.collectAsState()
    val model by viewModel.model.collectAsState()
    val quota by viewModel.quota.collectAsState()
    val suggestions by viewModel.suggestions.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val primary = MaterialTheme.colorScheme.primary

    // ساعت زنده برای شمارش معکوس بازه‌ی ۵ ساعته
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    // وقتی بازه تموم شد، سهم دوباره خونده می‌شه
    LaunchedEffect(now) {
        val reset = quota?.windowResetAt ?: 0L
        if (reset in 1..now) viewModel.refreshQuota()
    }

    val blockReason = quota?.blockReason(now)
    val blocked = blockReason != null

    // انتخاب عکس از گالری
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.attachImage(uri)
    }

    // دکمه‌ی پشتیبانی برای همه هست؛ مهمان اول وارد می‌شه یا حساب می‌سازه. ادمین (با ایمیل) می‌ره لیست کاربرها
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { com.appdony.app.data.session.SessionManager(context) }
    val loggedIn by session.isLoggedIn.collectAsState(initial = false)
    val email by session.userEmail.collectAsState(initial = "")
    val scope = rememberCoroutineScope()
    var showLogin by remember { mutableStateOf(false) }

    // پاپی بعد از هر جواب چند ثانیه نرم بالا پایین می‌پره
    var speaking by remember { mutableStateOf(false) }
    LaunchedEffect(messages.size) {
        if (messages.size > 1 && !messages.last().isUser) {
            speaking = true
            kotlinx.coroutines.delay(2400)
            speaking = false
        }
    }

    LaunchedEffect(messages.size, isLoading) {
        val count = messages.size + if (isLoading) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun send(text: String) {
        if ((text.isNotBlank() || pendingImage != null) && !isLoading && !blocked) {
            viewModel.sendMessage(text)
            inputText = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
            PlagueDoctorAvatar(sizeDp = 42, hasShadow = false, talking = isLoading || speaking)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("پاپی", color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOffline) Color(0xFFF5A623) else Color(0xFF34C759))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isOffline) "حالت آفلاین" else "آماده‌ی کمک",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(21.dp))
                    .background(primary.copy(alpha = 0.16f))
                    .border(BorderStroke(1.dp, primary.copy(alpha = 0.7f)), RoundedCornerShape(21.dp))
                    .clickable {
                        if (loggedIn) onSupportClick(com.appdony.app.data.api.SupportApi.isAdmin(email))
                        else showLogin = true
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.SupportAgent, null, tint = primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("پشتیبانی", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { viewModel.clearChat() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DeleteOutline, "پاک کردن", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            }
        }

        // انتخاب مدل + سهم مصرف
        AiModelSelector(selected = model, enabled = !isLoading, onSelect = { viewModel.selectModel(it) })
        AiQuotaCard(quota = quota, now = now)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg -> MessageRow(msg) }
            if (isLoading) {
                item {
                    Row(verticalAlignment = Alignment.Bottom) {
                        PlagueDoctorAvatar(sizeDp = 36, hasShadow = false, talking = true)
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            TypingDots()
                        }
                    }
                }
            }
        }

        if (messages.size <= 2 && !isLoading) {
            // بلاک پیشنهاد برنامه‌ها
            AiSuggestionBlock(
                posts = suggestions,
                onPostClick = onPostClick,
                onRefresh = { viewModel.loadSuggestions() }
            )

            androidx.compose.foundation.lazy.LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(QuickReplies) { reply ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .border(BorderStroke(1.dp, primary.copy(alpha = 0.7f)), RoundedCornerShape(18.dp))
                            .background(primary.copy(alpha = 0.10f))
                            .clickable { send(reply) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(reply, color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                    }
                }
            }
        }

        // نوار «عکس آماده‌ی ارسال»
        pendingImage?.let { bmp ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "عکس آماده‌ی ارسال",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                    if (!model.supportsImage) {
                        Text(
                            "برای عکس، از کلود هایکو استفاده می‌شه",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.removePendingImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, "حذف عکس", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isLoading) {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AddPhotoAlternate, "ارسال عکس", tint = primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                enabled = !blocked,
                placeholder = {
                    Text(
                        if (blocked) "سقف مصرف این مدل پر شده" else "پیامت رو بنویس...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                },
                shape = RoundedCornerShape(26.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send(inputText) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = primary
                )
            )
            Spacer(Modifier.width(8.dp))
            val canSend = (inputText.isNotBlank() || pendingImage != null) && !isLoading && !blocked
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = canSend) { send(inputText) },
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

    // مهمان: بعد از ورود یا ساخت حساب، مستقیم می‌ره پشتیبانی (ادمین می‌ره لیست کاربرها)
    if (showLogin) {
        LoginSheet(
            onDismiss = { showLogin = false },
            onLoginSuccess = { user ->
                showLogin = false
                scope.launch {
                    val mail = user.email.ifBlank { session.userEmail.first() }
                    onSupportClick(com.appdony.app.data.api.SupportApi.isAdmin(mail))
                }
            }
        )
    }
}

@Composable
private fun MessageRow(msg: ChatMessage) {
    val primary = MaterialTheme.colorScheme.primary
    // پیام‌های دارای کد پهن‌تر نشون داده می‌شن
    val maxBubble = if (hasCodeBlock(msg.text)) 330.dp else 290.dp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!msg.isUser) {
            PlagueDoctorAvatar(sizeDp = 36, hasShadow = false)
            Spacer(Modifier.width(8.dp))
        }
        Box(
            modifier = Modifier
                .widthIn(max = maxBubble)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (msg.isUser) 18.dp else 5.dp,
                        bottomEnd = if (msg.isUser) 5.dp else 18.dp
                    )
                )
                .background(if (msg.isUser) primary else MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column {
                msg.image?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "عکس ارسالی",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(220.dp)
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    if (msg.text.isNotBlank()) Spacer(Modifier.height(8.dp))
                }
                if (msg.text.isNotBlank()) {
                    AiMessageContent(text = msg.text, isUser = msg.isUser)
                }
            }
        }
    }
}

@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val a by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0.25f at (index * 150)
                        1f at (index * 150 + 250)
                        0.25f at (index * 150 + 500)
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(a)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}
