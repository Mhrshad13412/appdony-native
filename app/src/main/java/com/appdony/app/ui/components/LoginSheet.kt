package com.appdony.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import com.appdony.app.ui.theme.AppdonyBg
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
    var rememberMe by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var hiddenWebView by remember { mutableStateOf<WebView?>(null) }
    var isLoginSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // پاک کردن کوکی‌های مزاحم قبلی
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
                // 👈 جلوگیری از باز شدن پنجره ذخیره رمز گوگل
                settings.saveFormData = false
                settings.savePassword = false
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        // هر بار که صفحه جدید لود شد، اگر در حال لاگین بودیم، خطا را پاک کن
                        if (loading) errorMsg = null
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url == null) return

                        // اگر در صفحه لاگین هستیم و دکمه لاگین زده شده
                        if (url.contains("wp-login.php") && isLoginSubmitted) {
                            loading = true
                            val safeUser = username.replace("'", "\\'").replace("\n", "")
                            val safePass = password.replace("'", "\\'").replace("\n", "")
                            
                            // تزریق جاوااسکریپت برای پر کردن فرم و کلیک روی دکمه ورود
                            view?.evaluateJavascript("""
                                javascript:(function(){
                                    var userField = document.getElementById('user_login');
                                    var passField = document.getElementById('user_pass');
                                    var submitBtn = document.getElementById('wp-submit');
                                    if (userField && passField && submitBtn) {
                                        userField.value = '$safeUser';
                                        passField.value = '$safePass';
                                        submitBtn.click();
                                    }
                                })()
                            """.trimIndent(), null)
                            isLoginSubmitted = false 
                        } 
                        // اگر از صفحه لاگین خارج شدیم (یعنی لاگین موفق بوده)
                        else if (!url.contains("wp-login.php") && loading) {
                            // کمی صبر می‌کنیم تا کوکی‌ها کاملاً تنظیم شوند
                            scope.launch {
                                delay(1500)
                                
                                // بررسی می‌کنیم آیا کوکی لاگین واقعاً ست شده است یا خیر
                                val cookies = CookieManager.getInstance().getCookie("https://appdony.ir") ?: ""
                                if (cookies.contains("wordpress_logged_in")) {
                                    
                                    // استخراج توکن (Nonce) از داخل صفحه
                                    view?.evaluateJavascript("""
                                        (function() {
                                            var nonce = '';
                                            var scripts = document.getElementsByTagName('script');
                                            for (var i = 0; i < scripts.length; i++) {
                                                var text = scripts[i].innerHTML;
                                                var match = text.match(/"nonce":"([^"]+)"/);
                                                if (match) { nonce = match[1]; break; }
                                            }
                                            return nonce;
                                        })();
                                    """.trimIndent()) { nonceResult ->
                                        val cleanNonce = nonceResult.replace("\"", "").replace("null", "")
                                        
                                        scope.launch {
                                            val result = withContext(Dispatchers.IO) { 
                                                repository.getCurrentUser(cookies, cleanNonce) 
                                            }
                                            result.onSuccess { user ->
                                                if (user.id > 0) {
                                                    if (rememberMe) credentialsManager.saveCredentials(username, password)
                                                    else credentialsManager.clearCredentials()
                                                    
                                                    sessionManager.updateNonce(cleanNonce)
                                                    onLoginSuccess(user.copy(nonce = cleanNonce))
                                                    onDismiss()
                                                } else {
                                                    errorMsg = "اطلاعات کاربری یافت نشد."
                                                    loading = false
                                                }
                                            }.onFailure {
                                                errorMsg = "خطا در دریافت اطلاعات کاربر."
                                                loading = false
                                            }
                                        }
                                    }
                                } else {
                                    // اگر کوکی ست نشده بود یعنی رمز یا ایمیل اشتباه است
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
        // 👈 تنظیمات ظاهری برای رفع مشکل پس‌زمینه و سفیدی
        containerColor = AppdonyBg, // رنگ تیره اپ
        scrimColor = Color.Black.copy(alpha = 0.6f), // سایه تیره پشت پنجره
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .imePadding()
        ) {
            // هدر
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

            // فیلد نام کاربری / ایمیل
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

            // فیلد رمز عبور
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

            // مرا به خاطر بسپار
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { rememberMe = !rememberMe },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
                Text("مرا به خاطر بسپار (ورود خودکار)", color = AppdonyTextMuted, fontSize = 13.sp)
            }

            // پیام خطا (بدون هیچ ترفند اضافه‌ای)
            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0x33FF5252)).padding(12.dp)
                ) {
                    Text(errorMsg!!, color = Color(0xFFFF8A80), fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // دکمه ورود
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
