package com.appdony.app.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.appdony.app.data.session.SessionManager
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
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { AppRepository(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var hiddenWebView by remember { mutableStateOf<WebView?>(null) }
    var isLoginSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()

        credentialsManager.getUsername()?.let { username = it }
        credentialsManager.getPassword()?.let { password = it }
        if (username.isNotBlank() && password.isNotBlank()) {
            isLoginSubmitted = true
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.saveFormData = false
                settings.savePassword = false
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url == null) return

                        if (url.contains("wp-login.php") && isLoginSubmitted) {
                            loading = true
                            val safeUser = username.replace("'", "\\'").replace("\n", "")
                            val safePass = password.replace("'", "\\'").replace("\n", "")
                            view?.evaluateJavascript("""
                                javascript:(function(){
                                    var u = document.getElementById('user_login');
                                    var p = document.getElementById('user_pass');
                                    var b = document.getElementById('wp-submit');
                                    if(u && p && b) { u.value = '$safeUser'; p.value = '$safePass'; b.click(); }
                                })()
                            """.trimIndent(), null)
                            isLoginSubmitted = false 
                        } 
                        else if (!url.contains("wp-login.php") && loading) {
                            scope.launch {
                                delay(1500) // زمان برای ذخیره کوکی‌ها
                                val cookies = CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
                                
                                if (cookies.contains("wordpress_logged_in")) {
                                    view?.evaluateJavascript("""
                                        (function() {
                                            if (typeof wpApiSettings !== 'undefined' && wpApiSettings.nonce) return wpApiSettings.nonce;
                                            var scripts = document.getElementsByTagName('script');
                                            for (var i = 0; i < scripts.length; i++) {
                                                var match = scripts[i].innerHTML.match(/"nonce":"([^"]+)"/);
                                                if (match) return match[1];
                                            }
                                            return '';
                                        })();
                                    """.trimIndent()) { nonceResult ->
                                        val cleanNonce = nonceResult.replace("\"", "").replace("null", "")
                                        
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { 
                                                repository.getCurrentUser(cookies, cleanNonce) 
                                            }
                                            result.onSuccess { user ->
                                                if (user.id > 0) {
                                                    credentialsManager.saveCredentials(username, password)
                                                    sessionManager.updateNonce(cleanNonce)
                                                    onLoginSuccess(user.copy(nonce = cleanNonce))
                                                    onDismiss()
                                                } else {
                                                    errorMsg = "اطلاعات کاربری یافت نشد."
                                                    loading = false
                                                }
                                            }.onFailure {
                                                errorMsg = "خطا در ارتباط با سرور."
                                                loading = false
                                            }
                                        }
                                    }
                                } else {
                                    errorMsg = "ایمیل/نام کاربری یا رمز عبور اشتباه است."
                                    loading = false
                                }
                            }
                        }
                    }
                }
                loadUrl("https://appdony.ir/wp-login.php")
                hiddenWebView = this
            }
        },
        modifier = Modifier.size(1.dp)
    )

    fun submitLogin() {
        loading = true
        errorMsg = null
        isLoginSubmitted = true
        hiddenWebView?.loadUrl("https://appdony.ir/wp-login.php")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface, // رفع مشکل پس‌زمینه
        scrimColor = Color.Black.copy(alpha = 0.6f),
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("با حساب کاربری سایت وارد شو", color = AppdonyTextMuted, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = AppdonyTextPrimary)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("نام کاربری یا ایمیل", color = AppdonyTextMuted) },
                leadingIcon = { Icon(Icons.Default.Person, null, tint = AppdonyTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = AppdonyCardBg,
                    focusedContainerColor = AppdonyCardBg,
                    unfocusedContainerColor = AppdonyCardBg,
                    focusedTextColor = AppdonyTextPrimary,
                    unfocusedTextColor = AppdonyTextPrimary
                ),
                enabled = !loading
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; errorMsg = null },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("رمز عبور", color = AppdonyTextMuted) },
                leadingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null, tint = AppdonyTextMuted
                        )
                    }
                },
                trailingIcon = { Icon(Icons.Default.Lock, null, tint = AppdonyTextMuted) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!loading) submitLogin() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = AppdonyCardBg,
                    focusedContainerColor = AppdonyCardBg,
                    unfocusedContainerColor = AppdonyCardBg,
                    focusedTextColor = AppdonyTextPrimary,
                    unfocusedTextColor = AppdonyTextPrimary
                ),
                enabled = !loading
            )

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33FF5252)).padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { submitLogin() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = AppdonyCardBg
                )
            ) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text("ورود", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
