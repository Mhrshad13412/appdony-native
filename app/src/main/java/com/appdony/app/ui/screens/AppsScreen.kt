package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun AppsScreen(
    onPostClick: (Int) -> Unit
) {
    // استفاده مستقیم از HomeScreen با تنظیم isGamesTab روی false
    HomeScreen(
        isGamesTab = false,
        onPostClick = onPostClick
    )
}
