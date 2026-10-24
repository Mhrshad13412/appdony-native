package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun GamesScreen(
    onPostClick: (Int) -> Unit
) {
    // استفاده مستقیم از HomeScreen با تنظیم isGamesTab روی true
    HomeScreen(
        isGamesTab = true,
        onPostClick = onPostClick
    )
}
