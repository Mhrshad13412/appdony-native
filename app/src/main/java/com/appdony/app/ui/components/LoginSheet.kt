package com.appdony.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.appdony.app.data.model.UserData
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.data.session.CredentialsManager
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: (user: UserData) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialsManager = remember { CredentialsManager(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var hiddenWebView by remember { mutableStateOf<WebView?>(null) }
    var webViewReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        credentialsManager.getUsername()?.let { username = it }
        credentialsManager.getPassword()?.let { password = it }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        webViewReady = true
                    }
                }
                loadUrl("https://appdony.ir/wp-login.php")
                hiddenWebView = this
            }
        },
        modifier = Modifier.size(1.dp)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("ورود به اپدونی", color = AppdonyTextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("با حساب کاربری سایت وارد شو", color = AppdonyTextMuted, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = AppdonyTextPrimary)
                }
            }

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                // 👇 راهنمای فیلد تغییر کرد
                placeholder = { Text("نام کاربری (نه ایمیل)", color = AppdonyTextMuted) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = AppdonyTextMuted) },
                singleLine = true,
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = AppdonyCardBg,
                    focusedContainerColor = AppdonyCardBg,
                    unfocusedContainerColor = AppdonyCardBg,
                    focusedTextColor = AppdonyTextPrimary,
                    unfocusedTextColor = AppdonyTextPrimary,
                    disabledContainerColor = AppdonyCardBg,
                    disabledTextColor = AppdonyTextMuted
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("رمز عبور", color = AppdonyTextMuted) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = AppdonyTextMuted) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null, tint = AppdonyTextMuted
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = !loading,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = AppdonyCardBg,
                    focusedContainerColor = AppdonyCardBg,
                    unfocusedContainerColor = AppdonyCardBg,
                    focusedTextColor = AppdonyTextPrimary,
                    unfocusedTextColor = AppdonyTextPrimary,
                    disabledContainerColor = AppdonyCardBg,
                    disabledTextColor = AppdonyTextMuted
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (webViewReady && !loading && username.isNotBlank() && password.isNotBlank()) {
                            val wv = hiddenWebView ?: return@KeyboardActions
                            loading = true; errorMsg = null
                            wv.evaluateJavascript(buildLoginJs(username, password)) {
                                scope.launch {
                                    delay(2500)
                                    val cookies = CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
                                    if (cookies.contains("wordpress_logged_in")) {
                                        val repo = AppRepository(context)
                                        val result = withContext(Dispatchers.IO) { repo.getCurrentUser() }
                                        result.onSuccess { user ->
                                            loading = false
                                            if (user.id > 0) {
                                                if (rememberMe) credentialsManager.saveCredentials(username, password)
                                                else credentialsManager.clearCredentials()
                                                onLoginSuccess(user)
                                            } else errorMsg = "خطا در دریافت اطلاعات"
                                        }.onFailure {
                                            loading = false; errorMsg = "خطا: ${it.message}"
                                        }
                                    } else {
                                        loading = false; errorMsg = "نام کاربری یا رمز عبور اشتباه است"
                                    }
                                }
                            }
                        }
                    }
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "مرا به خاطر بسپار (ورود خودکار)",
                    color = AppdonyTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { rememberMe = !rememberMe }
                )
            }

            if (errorMsg != null) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33FF5252)).padding(12.dp)) {
                    Text(errorMsg!!, color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!webViewReady) { errorMsg = "لطفاً چند لحظه صبر کن..."; return@Button }
                    val wv = hiddenWebView ?: return@Button
                    loading = true; errorMsg = null
                    wv.evaluateJavascript(buildLoginJs(username, password)) {
                        scope.launch {
                            delay(2500)
                            val cookies = CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
                            if (cookies.contains("wordpress_logged_in")) {
                                val repo = AppRepository(context)
                                val result = withContext(Dispatchers.IO) { repo.getCurrentUser() }
                                result.onSuccess { user ->
                                    loading = false
                                    if (user.id > 0) {
                                        if (rememberMe) credentialsManager.saveCredentials(username, password)
                                        else credentialsManager.clearCredentials()
                                        onLoginSuccess(user)
                                    } else errorMsg = "خطا در دریافت اطلاعات"
                                }.onFailure { loading = false; errorMsg = "خطا: ${it.message}" }
                            } else {
                                loading = false; errorMsg = "نام کاربری یا رمز عبور اشتباه است"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, disabledContainerColor = AppdonyCardBg)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Text("ورود", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun buildLoginJs(username: String, password: String): String {
    val safeUser = username.replace("'", "\\'").replace("\n", "")
    val safePass = password.replace("'", "\\'").replace("\n", "")
    return """
        javascript:(function() {
            var userField = document.getElementById('user_login');
            var passField = document.getElementById('user_pass');
            var submitBtn = document.getElementById('wp-submit');
            var remember = document.getElementById('rememberme');
            if (remember) { remember.checked = true; }
            if (userField && passField && submitBtn) {
                userField.value = '$safeUser';
                passField.value = '$safePass';
                var event = new Event('input', { bubbles: true });
                userField.dispatchEvent(event);
                passField.dispatchEvent(event);
                submitBtn.click();
                return 'ok';
            }
            return 'error';
        })();
    """.trimIndent()
}
