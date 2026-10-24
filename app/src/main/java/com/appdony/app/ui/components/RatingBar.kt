package com.appdony.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun RatingBar(rating: Int, onRatingChanged: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "ستاره $i",
                tint = if (i <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp).clickable { onRatingChanged(i) }
            )
        }
    }
}
