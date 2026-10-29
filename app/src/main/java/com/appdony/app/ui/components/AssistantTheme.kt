package com.appdony.app.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.appdony.app.util.SayKind

internal val AvatarSize = 60.dp
internal val EdgeMargin = 12.dp
internal val AvatarShape = RoundedCornerShape(24.dp)
internal val GoldColor = Color(0xFFFFB300)
internal val GreenColor = Color(0xFF34C759)
internal val RedColor = Color(0xFFFF6B6B)

// رنگ‌های گربه
internal val FurColor = Color(0xFFFFB566)
internal val FurDark = Color(0xFFE48A3E)
internal val CreamColor = Color(0xFFFFF1DC)
internal val OutlineColor = Color(0xFF6B3E1E)
internal val PinkColor = Color(0xFFFF8FA3)
internal val EyeColor = Color(0xFF2A1A14)

// رنگ کاموای هر دکمه (چت، دانلودها، جستجو) و کاموای خودِ گربه
internal val YarnColors = listOf(Color(0xFFEC6B8E), Color(0xFF4DB6AC), Color(0xFFFFB74D))
internal val CatYarn = Color(0xFFEC6B8E)

internal fun kindIcon(kind: SayKind): ImageVector = when (kind) {
    SayKind.TIP -> Icons.Outlined.Lightbulb
    SayKind.SEARCH, SayKind.SUGGEST -> Icons.Outlined.Search
    SayKind.DOWNLOAD -> Icons.Outlined.Download
    SayKind.DONE -> Icons.Outlined.CheckCircle
    SayKind.FAIL -> Icons.Outlined.ErrorOutline
    SayKind.PLUS -> Icons.Outlined.WorkspacePremium
}

internal fun kindColor(kind: SayKind, fallback: Color): Color = when (kind) {
    SayKind.PLUS -> GoldColor
    SayKind.DONE -> GreenColor
    SayKind.FAIL -> RedColor
    else -> fallback
}
