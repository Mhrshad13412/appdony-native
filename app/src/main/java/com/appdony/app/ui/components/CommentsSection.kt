package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.model.Comment
import com.appdony.app.ui.screens.CommentsUiState
import com.appdony.app.ui.screens.CommentsViewModel
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary

@Composable
fun CommentsSection(
    postId: Int,
    isLoggedIn: Boolean,
    nonce: String,
    onNeedLogin: () -> Unit
) {
    val vm: CommentsViewModel = viewModel(
        key = "comments_$postId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CommentsViewModel(postId) as T
            }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val sendResult by vm.sendResult.collectAsStateWithLifecycle()

    var showComposer by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    // اگه لاگین نباشه و بخواد بنویسه، onNeedLogin صدا زده میشه
    LaunchedEffect(showComposer) {
        if (showComposer && !isLoggedIn) {
            showComposer = false
            onNeedLogin()
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {

        // هدر + دکمه نظر جدید
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "نظرات کاربران",
                color = AppdonyTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable {
                        if (isLoggedIn) showComposer = !showComposer
                        else onNeedLogin()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "نظر جدید",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // فرم نوشتن نظر
        if (showComposer && isLoggedIn) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppdonyCardBg)
                    .padding(12.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("نظرت رو بنویس...", color = AppdonyTextMuted)
                    },
                    minLines = 3,
                    maxLines = 6,
                    enabled = !sending,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = AppdonyTextPrimary,
                        unfocusedTextColor = AppdonyTextPrimary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = !sending && commentText.isNotBlank()) {
                                vm.sendComment(commentText, nonce)
                                commentText = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sending) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = "ارسال",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // نمایش نتیجه ارسال
        if (sendResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (sendResult!!.contains("✓")) Color(0x330D7377)
                        else Color(0x33FF5252)
                    )
                    .clickable { vm.clearSendResult() }
                    .padding(12.dp)
            ) {
                Text(
                    text = sendResult!!,
                    color = if (sendResult!!.contains("✓")) Color(0xFF14A0A6)
                    else Color(0xFFFF8A80),
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // لیست کامنت‌ها
        when (val s = state) {
            is CommentsUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            is CommentsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppdonyCardBg)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(s.message, color = AppdonyTextMuted, fontSize = 13.sp)
                }
            }
            is CommentsUiState.Success -> {
                if (s.comments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppdonyCardBg)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "هنوز نظری ثبت نشده",
                                color = AppdonyTextMuted,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "اولین نفر باش!",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.comments.take(20).forEach { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppdonyCardBg)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            val avatar = comment.avatarUrl()
            if (!avatar.isNullOrBlank()) {
                AsyncImage(
                    model = avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = AppdonyTextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.author_name,
                color = AppdonyTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = comment.date.take(10),
                color = AppdonyTextMuted,
                fontSize = 10.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = comment.cleanContent(),
                color = AppdonyTextPrimary,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}
