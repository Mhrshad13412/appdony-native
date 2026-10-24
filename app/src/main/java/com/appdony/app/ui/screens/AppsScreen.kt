package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun AppsScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit = { _, _ -> } // ✅ امضا اصلاح شد
) {
    HomeScreen(
        isGamesTab = false,
        onPostClick = onPostClick,
        onCategoryClick = onCategoryClick
    )
}
