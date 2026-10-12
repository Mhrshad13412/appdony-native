package com.appdony.app.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun GamesScreen(
    onPostClick: (Int) -> Unit,
    onCategoryClick: (Int, String) -> Unit
) {
    FeedScreen(
        feedType = "games",
        onPostClick = onPostClick,
        onCategoryClick = onCategoryClick
    )
}
