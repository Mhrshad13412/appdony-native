package com.appdony.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appdony.app.data.api.ApiClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var statusText by remember { mutableStateOf("منتظر تست...") }
                val scope = rememberCoroutineScope()

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = statusText)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            statusText = "در حال لود..."
                            scope.launch {
                                try {
                                    val posts = ApiClient.api.getPosts(perPage = 5)
                                    posts.take(3).forEach {
                                        Log.d("APPDONY_TEST", "→ ${it.title.rendered}")
                                    }
                                    statusText = "✅ ${posts.size} پست لود شد!"
                                } catch (e: Exception) {
                                    Log.e("APPDONY_TEST", "خطا", e)
                                    statusText = "❌ ${e.message}"
                                }
                            }
                        }) {
                            Text("تست API")
                        }
                    }
                }
            }
        }
    }
}
