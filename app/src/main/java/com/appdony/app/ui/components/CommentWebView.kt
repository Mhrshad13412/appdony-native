package com.appdony.app.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyGreen
import com.appdony.app.ui.theme.AppdonyTextPrimary

/**
 * صفحه‌ی افزودن نظر با WebView.
 * کاربر بدون اینکه بفهمه، وارد صفحه کامنت سایت می‌شه.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CommentWebView(
    postId: Int,
    onClose: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppdonyBg)
    ) {
        // هدر
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppdonyCardBg)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "بستن",
                    tint = AppdonyTextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "ثبت نظر جدید",
                color = AppdonyTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // مخفی کردن هدر و فوتر سایت برای تمیزتر شدن
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var selectors = ['header', 'footer', '.site-header',
                                                         '.site-footer', 'nav', '.breadcrumb',
                                                         '.sidebar', 'aside'];
                                        selectors.forEach(function(s) {
                                            document.querySelectorAll(s).forEach(function(el) {
                                                el.style.display = 'none';
                                            });
                                        });
                                        document.body.style.background = '#121517';
                                        document.body.style.color = '#E6E1E5';
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }
                        }
                        // آدرس صفحه‌ی کامنت پست
                        loadUrl("https://appdony.ir/?p=$postId#respond")
                    }
                }
            )

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AppdonyBg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppdonyGreen)
                }
            }
        }
    }
}
