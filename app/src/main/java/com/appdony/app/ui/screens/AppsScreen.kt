package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun AppsScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit
) {
    FeedScreen(feedType = "apps", onPostClick = onPostClick, onCategoryClick = onCategoryClick)
}
