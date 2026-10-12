package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun BlogScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit
) {
    FeedScreen(
        feedType = "blog",
        onPostClick = onPostClick,
        onCategoryClick = onCategoryClick
    )
}
