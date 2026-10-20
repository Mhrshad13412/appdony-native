package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.appdony.app.data.model.Comment
import com.appdony.app.ui.screens.CommentsUiState
import com.appdony.app.ui.screens.CommentsViewModel

@Composable
fun CommentsSection(postId: Int, isLoggedIn: Boolean, nonce: String, onNeedLogin: () -> Unit) {
    val context = LocalContext.current // 👈 کانتکست گرفته می‌شود
    
    val vm: CommentsViewModel = viewModel(
        key = "comments_$postId",
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return CommentsViewModel(postId, context) as T // 👈 پاس داده می‌شود
            }
        }
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val sending by vm.sending.collectAsStateWithLifecycle()
    val sendResult by vm.sendResult.collectAsStateWithLifecycle()

    var showComposer by remember { mutableStateOf(false) }
    var commentText by remember { mutableStateOf("") }

    LaunchedEffect(showComposer) {
        if (showComposer && !isLoggedIn) { showComposer = false; onNeedLogin() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("نظرات کاربران", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary)
                    .clickable { if (isLoggedIn) showComposer = !showComposer else onNeedLogin() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("نظر جدید", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (showComposer && isLoggedIn) {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
                OutlinedTextField(
                    value = commentText, onValueChange = { commentText = it }, modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("نظرت رو بنویس...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    minLines = 3, maxLines = 6, enabled = !sending, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary)
                            .clickable(enabled = !sending && commentText.isNotBlank()) { vm.sendComment(commentText, nonce); commentText = "" }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (sending) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                            }
                            Text("ارسال", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (sendResult != null) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (sendResult!!.contains("✓")) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0x33FF5252))
                    .clickable { vm.clearSendResult() }.padding(12.dp)
            ) {
                Text(sendResult!!, color = if (sendResult!!.contains("✓")) MaterialTheme.colorScheme.primary else Color(0xFFFF8A80), fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        when (val s = state) {
            is CommentsUiState.Loading -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            is CommentsUiState.Error -> Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp), contentAlignment = Alignment.Center) {
                Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            is CommentsUiState.Success -> {
                if (s.comments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(14.dp)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("هنوز نظری ثبت نشده", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            Spacer(Modifier.height(6.dp))
                            Text("اولین نفر باش!", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        s.comments.take(20).forEach { comment -> CommentItem(comment) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(12.dp)) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            val avatar = comment.avatarUrl()
            if (!avatar.isNullOrBlank()) AsyncImage(model = avatar, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            else Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(comment.author_name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(comment.date.take(10), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(comment.cleanContent(), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
