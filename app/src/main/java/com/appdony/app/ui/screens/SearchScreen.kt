package com.appdony.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.ui.components.AppRowItem

// جستجو یه ViewModel مشترک بین نوار بالا (که فیلد نوشتنه) و این صفحه (که نتیجه‌ها رو نشون می‌ده) داره
@Composable
fun rememberSearchViewModel(): SearchViewModel {
    val activity = findActivity(LocalContext.current)
    return viewModel(
        viewModelStoreOwner = activity,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SearchViewModel(activity.applicationContext) as T
            }
        }
    )
}

private fun findActivity(context: Context): ComponentActivity {
    var c: Context = context
    while (c is ContextWrapper) {
        if (c is ComponentActivity) return c
        c = c.baseContext
    }
    throw IllegalStateException("MainActivity پیدا نشد")
}

/** صفحه‌ی جستجو: فقط نتیجه‌ها؛ فیلد جستجو همون نوار بالاست و با انیمیشن بهش تبدیل می‌شه */
@Composable
fun SearchScreen(onBack: () -> Unit, onPostClick: (Int) -> Unit) {
    val viewModel = rememberSearchViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).imePadding()) {
        when (val s = state) {
            is SearchUiState.Idle -> {
                if (history.isNotEmpty()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("جستجوهای اخیر", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        Text("پاک کردن", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, modifier = Modifier.clickable { viewModel.clearHistory() })
                    }
                    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
                        items(history, key = { it }) { item ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.setQueryFromHistory(item) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(item, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("چی می‌خوای پیدا کنی؟", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            }
            is SearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is SearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is SearchUiState.Success -> {
                if (s.posts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("چیزی پیدا نشد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                        items(s.posts, key = { it.id }) { post ->
                            AppRowItem(rank = 0, title = post.cleanTitle(), imageUrl = post.featuredImageUrl(), onClick = { onPostClick(post.id) }, showRank = false)
                        }
                    }
                }
            }
        }
    }
}
