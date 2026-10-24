package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun GamesScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit = { _, _ -> } // ✅ امضا اصلاح شد
) {
    HomeScreen(
        isGamesTab = true,
        onPostClick = onPostClick,
        onCategoryClick = onCategoryClick
    )
}
