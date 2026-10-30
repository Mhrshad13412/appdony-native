package com.appdony.app.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.ui.graphics.Color
import com.appdony.app.ui.theme.AppdonyGreenLight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.appdony.app.data.api.LoginException
import com.appdony.app.data.api.WordPressLogin
import com.appdony.app.data.model.UserData
import com.appdony.app.data.session.CredentialsManager
import com.appdony.app.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: (UserData) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var errorTrace by remember { mutableStateOf<String?>(null) }
    var traceCopied by remember { mutableStateOf(false) }

    fun doLogin() {
        if (isLoading) return
        if (username.isBlank() || password.isBlank()) {
            errorText = "نام کاربری (یا ایمیل) و رمز عبور را وارد کن."
            errorTrace = null
            return
        }
        focusManager.clearFocus()
        isLoading = true
        errorText = null
        errorTrace = null
        traceCopied = false

        val typedUser = username.trim()
        val typedPass = password

        scope.launch {
            WordPressLogin.login(typedUser, typedPass)
                .onSuccess { user ->
                    withContext(Dispatchers.IO) {
                        // ذخیره‌ی سشن و اطلاعات ورود خودکار
                        SessionManager(context).saveSession(
                            userId = user.id,
                            username = user.username.ifBlank { user.slug.ifBlank { typedUser } },
                            displayName = user.displayName(),
                            email = user.email,
                            avatarUrl = user.avatarUrl().orEmpty(),
                            nonce = user.nonce
                        )
                        CredentialsManager(context).saveCredentials(typedUser, typedPass)
                        // ثبت کاربر توی لیست پشتیبان (اگه نشد، ورود خراب نمی‌شه)
                        com.appdony.app.data.api.SupportApi.registerUser(
                            com.appdony.app.data.api.SupportApi.threadOf(user.id),
                            user.displayName()
                        )
                    }
                    onLoginSuccess(user)
                }
                .onFailure { error ->
                    errorText = error.message ?: "ورود انجام نشد."
                    errorTrace = (error as? LoginException)?.trace
                }
            isLoading = false
        }
    }

    FloatingDialog(onDismiss = onDismiss, dismissible = !isLoading) {
        val fieldColors = loginFieldColors()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ورود به اپدونی",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { if (!isLoading) onDismiss() }) {
                Icon(Icons.Default.Close, contentDescription = "بستن", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Text(
            text = "با حساب سایت اپدونی وارد شو",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("نام کاربری یا ایمیل") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("رمز عبور") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "پنهان کردن رمز" else "نمایش رمز"
                    )
                }
            },
            singleLine = true,
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = fieldColors,
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { doLogin() }),
            modifier = Modifier.fillMaxWidth()
        )

        val error = errorText
        if (error != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val trace = errorTrace
        if (trace != null) {
            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(trace))
                    traceCopied = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (traceCopied) "کپی شد ✓" else "کپی گزارش فنی", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { doLogin() },
            enabled = !isLoading,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                disabledContentColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            } else {
                Text("ورود", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/** رنگ‌های یکدست فیلدهای ورود: بوردر ملایم، فوکوس فیروزه‌ای، متن و لیبل خوانا. */
@Composable
private fun loginFieldColors(): TextFieldColors {
    val scheme = MaterialTheme.colorScheme
    val accent = AppdonyGreenLight
    val container = scheme.surfaceVariant.copy(alpha = 0.55f)
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = scheme.onSurface,
        unfocusedTextColor = scheme.onSurface,
        disabledTextColor = scheme.onSurface.copy(alpha = 0.5f),
        focusedContainerColor = container,
        unfocusedContainerColor = container,
        disabledContainerColor = container,
        focusedBorderColor = accent,
        unfocusedBorderColor = scheme.onSurface.copy(alpha = 0.14f),
        disabledBorderColor = scheme.onSurface.copy(alpha = 0.08f),
        focusedLabelColor = accent,
        unfocusedLabelColor = scheme.onSurfaceVariant,
        disabledLabelColor = scheme.onSurfaceVariant.copy(alpha = 0.6f),
        focusedLeadingIconColor = accent,
        unfocusedLeadingIconColor = scheme.onSurfaceVariant,
        focusedTrailingIconColor = accent,
        unfocusedTrailingIconColor = scheme.onSurfaceVariant,
        cursorColor = accent,
        selectionColors = TextSelectionColors(handleColor = accent, backgroundColor = accent.copy(alpha = 0.3f))
    )
}
