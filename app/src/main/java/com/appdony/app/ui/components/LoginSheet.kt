package com.appdony.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyGreen
import java.net.URLDecoder

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginSheet(
    onDismiss: () -> Unit,
    onLoginSuccess: (username: String, displayName: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isLoading by remember { mutableStateOf(true) }
    var loginDone by remember { mutableStateOf(false) }
    var sawLoginPage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppdonyBg,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = false
                        settings.displayZoomControls = false

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {

                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                if (url?.contains("wp-login.php") == true) {
                                    sawLoginPage = true
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.evaluateJavascript(hideThemeScript, null)

                                if (!loginDone &&
                                    sawLoginPage &&
                                    url?.contains("wp-login.php") != true
                                ) {
                                    val user = extractUserFromCookies()
                                    if (user.first.isNotEmpty()) {
                                        loginDone = true
                                        onLoginSuccess(user.first, user.second)
                                    }
                                }
                            }
                        }

                        loadUrl("https://appdony.ir/wp-login.php?redirect_to=https://appdony.ir/")
                    }
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppdonyBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppdonyGreen)
                }
            }
        }
    }
}

private fun extractUserFromCookies(): Pair<String, String> {
    val allCookies = CookieManager.getInstance()
        .getCookie("https://appdony.ir") ?: return "" to ""

    val loginCookie = allCookies.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("wordpress_logged_in_") }
        ?: return "" to ""

    val value = loginCookie.substringAfter("=", "")
    val decoded = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }

    val parts = decoded.split("|")
    val username = parts.getOrNull(0) ?: ""
    return username to username
}

private val hideThemeScript = """
(function() {
    var selectors = ['header', 'footer', '.site-header', '.site-footer', 'nav', '.breadcrumb', '.sidebar', 'aside'];
    selectors.forEach(function(s) {
        document.querySelectorAll(s).forEach(function(el) {
            el.style.display = 'none';
        });
    });
    document.body.style.background = '#121517';
    document.body.style.color = '#E6E1E5';
    document.documentElement.style.background = '#121517';
    var loginForm = document.getElementById('loginform');
    if (loginForm) {
        loginForm.style.background = '#1E2225';
        loginForm.style.border = '1px solid #0D7377';
        loginForm.style.borderRadius = '16px';
        loginForm.style.padding = '24px';
        loginForm.style.boxShadow = '0 4px 20px rgba(13,115,119,0.3)';
    }
    var inputs = document.querySelectorAll('input[type=text], input[type=password], input[type=email]');
    inputs.forEach(function(inp) {
        inp.style.background = '#23262A';
        inp.style.color = '#E6E1E5';
        inp.style.border = '1px solid #0D7377';
        inp.style.borderRadius = '10px';
        inp.style.padding = '12px';
        inp.style.width = '100%';
        inp.style.boxSizing = 'border-box';
        inp.style.marginBottom = '10px';
    });
    var labels = document.querySelectorAll('label');
    labels.forEach(function(l) { l.style.color = '#E6E1E5'; });
    var submit = document.getElementById('wp-submit');
    if (submit) {
        submit.style.background = '#0D7377';
        submit.style.color = '#fff';
        submit.style.border = 'none';
        submit.style.borderRadius = '12px';
        submit.style.padding = '14px 20px';
        submit.style.width = '100%';
        submit.style.fontWeight = 'bold';
        submit.style.fontSize = '15px';
        submit.style.cursor = 'pointer';
        submit.style.marginTop = '10px';
    }
    var links = document.querySelectorAll('a');
    links.forEach(function(a) { a.style.color = '#14A0A6'; });
    var loginTitle = document.querySelector('h1');
    if (loginTitle) { loginTitle.style.color = '#E6E1E5'; }
})();
""".trimIndent()
