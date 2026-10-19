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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.appdony.app.data.model.UserData
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.ui.theme.AppdonyBg
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
    var isLoading by remember { mutableStateOf(true) }
    var loginDone by remember { mutableStateOf(false) }
    var sawLoginPage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppdonyBg,
        dragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                if (url?.contains("wp-login.php") == true) sawLoginPage = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.evaluateJavascript(hideThemeScript, null)

                                if (!loginDone && sawLoginPage && url?.contains("wp-login.php") != true) {
                                    loginDone = true
                                    scope.launch {
                                        delay(1000)
                                        val repo = AppRepository(ctx)
                                        val result = withContext(Dispatchers.IO) { repo.getCurrentUser() }
                                        result.onSuccess { user -> if (user.id > 0) onLoginSuccess(user) }
                                    }
                                }
                            }
                        }
                        loadUrl("https://appdony.ir/wp-login.php")
                    }
                }
            )
            if (isLoading) {
                Box(Modifier.fillMaxSize().background(AppdonyBg), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private val hideThemeScript = """
(function() {
    var css = document.createElement('style');
    css.innerHTML = `
        * { font-family: -apple-system, sans-serif !important; }
        body, html { background: #121517 !important; }
        body.login { background: #121517 !important; display: flex !important; align-items: center !important; justify-content: center !important; min-height: 100vh !important; margin: 0 !important; }
        #login { width: 100% !important; max-width: 400px !important; padding: 20px !important; margin: 0 auto !important; }
        .login h1, .login h1 a { color: #E6E1E5 !important; background: none !important; font-size: 24px !important; text-align: center !important; padding: 0 0 20px 0 !important; text-indent: 0 !important; width: auto !important; height: auto !important; text-shadow: none !important; }
        .login form { background: #1E2225 !important; border: 1px solid #0D7377 !important; border-radius: 16px !important; padding: 24px !important; box-shadow: 0 4px 20px rgba(13,115,119,0.3) !important; margin-top: 0 !important; }
        .login label { color: #E6E1E5 !important; font-size: 13px !important; display: block !important; margin-bottom: 6px !important; }
        .login input[type=text], .login input[type=password], .login input[type=email] { background: #23262A !important; color: #E6E1E5 !important; border: 1px solid #0D7377 !important; border-radius: 10px !important; padding: 12px !important; width: 100% !important; box-sizing: border-box !important; font-size: 14px !important; margin-bottom: 12px !important; box-shadow: none !important; outline: none !important; }
        .login input:focus { border-color: #14A0A6 !important; box-shadow: 0 0 0 3px rgba(13,115,119,0.3) !important; }
        .login .button-primary, .login #wp-submit { background: #0D7377 !important; color: #fff !important; border: none !important; border-radius: 12px !important; padding: 14px !important; width: 100% !important; font-weight: bold !important; font-size: 15px !important; cursor: pointer !important; margin-top: 8px !important; text-shadow: none !important; box-shadow: none !important; height: auto !important; line-height: 1.2 !important; }
        .login #nav, .login #backtoblog { text-align: center !important; margin-top: 16px !important; }
        .login #nav a, .login #backtoblog a { color: #14A0A6 !important; font-size: 13px !important; text-decoration: none !important; }
        .login .message, .login .notice, .login .error { background: #23262A !important; color: #E6E1E5 !important; border: none !important; border-left: 4px solid #0D7377 !important; border-radius: 8px !important; padding: 12px !important; margin-bottom: 16px !important; box-shadow: none !important; }
        .login .error { border-left-color: #FF5252 !important; color: #FF8A80 !important; }
        .login .forgetmenot label { color: #939396 !important; font-size: 12px !important; }
        .login .forgetmenot input { width: auto !important; margin-right: 6px !important; }
        .login .privacy-policy-page-link, .login .language-switcher { display: none !important; }
    `;
    document.head.appendChild(css);
    
    // 👇 تیک زدن خودکار "مرا به خاطر بسپار"
    setTimeout(function() {
        var remember = document.getElementById('rememberme');
        if (remember) { remember.checked = true; }
    }, 500);
})();
""".trimIndent()
