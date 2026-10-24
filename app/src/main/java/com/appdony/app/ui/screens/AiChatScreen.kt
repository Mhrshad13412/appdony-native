package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appdony.app.data.api.AiApi
import com.appdony.app.data.preferences.ChatHistoryManager
import com.appdony.app.data.preferences.ChatMessage
import com.appdony.app.ui.components.PlagueDoctorAvatar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val chatHistoryManager = remember { ChatHistoryManager(context) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var isHistoryLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        chatHistoryManager.getChatHistory().collect { history ->
            if (history.isEmpty()) {
                val welcomeMsg = listOf(ChatMessage("سلام! من دکتر طاعونم. چطور میتونم کمکت کنم؟", false))
                messages = welcomeMsg
                chatHistoryManager.saveChatHistory(welcomeMsg)
            } else {
                messages = history
            }
            isHistoryLoaded = true
        }
    }

    fun sendMessage(customText: String? = null) {
        val textToSend = customText ?: inputText
        if (textToSend.isBlank() || isLoading) return
        val userMsg = textToSend.trim()
        val updatedMessages = messages + ChatMessage(userMsg, true)
        messages = updatedMessages
        if (customText == null) inputText = ""
        isLoading = true
        scope.launch { chatHistoryManager.saveChatHistory(updatedMessages) }

        scope.launch {
            val history = messages.dropLast(1).map { (if (it.isUser) "user" else "assistant") to it.text }
            val result = AiApi.sendMessage(userMsg, history)
            result.onSuccess { response ->
                val finalMessages = messages + ChatMessage(response, false)
                messages = finalMessages
                chatHistoryManager.saveChatHistory(finalMessages)
            }.onFailure { error ->
                val errorMsg = ChatMessage("خطا: ${error.message ?: "مشکل در اتصال"}", false)
                val finalMessages = messages + errorMsg
                messages = finalMessages
                chatHistoryManager.saveChatHistory(finalMessages)
            }
            isLoading = false
        }
    }

    fun clearChat() {
        scope.launch {
            chatHistoryManager.clearHistory()
            val welcomeMsg = listOf(ChatMessage("سلام! من دکتر طاعونم. چطور میتونم کمکت کنم؟", false))
            messages = welcomeMsg
            chatHistoryManager.saveChatHistory(welcomeMsg)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("چت با دکتر طاعون", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = { clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "پاک کردن چت", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            if (!isHistoryLoaded) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { msg ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                        ) {
                            if (!msg.isUser) {
                                PlagueDoctorAvatar(sizeDp = 40)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                            bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                        )
                                    )
                                    .background(if (msg.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = if (msg.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                    if (isLoading) {
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlagueDoctorAvatar(sizeDp = 40)
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { sendMessage("یه بازی یا برنامه خوب برام پیشنهاد بده") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "پیشنهاد هوشمند",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("پیامت رو بنویس...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { sendMessage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "ارسال", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
