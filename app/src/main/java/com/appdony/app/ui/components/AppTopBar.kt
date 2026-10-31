package com.appdony.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * نوار بالای صفحه‌ی اصلی؛ همین نوار تو حالت جستجو تبدیل به نوار جستجو می‌شه:
 * آواتار آروم جمع می‌شه، دکمه‌ی برگشت باز می‌شه و کادر جستجو پهن می‌شه و خودش فیلد نوشتنه.
 * (progress: ۰ = حالت خونه ، ۱ = حالت جستجو)
 */
@Composable
fun AppTopBar(
    isLoggedIn: Boolean = false,
    userInitial: String = "",
    onSearchClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    searchMode: Boolean = false,
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onSearchBack: () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = if (searchMode) 1f else 0f,
        animationSpec = tween(380, easing = FastOutSlowInEasing),
        label = "search_morph"
    )
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // وارد جستجو شدیم: بعد از اینکه انیمیشن جا افتاد کیبورد باز می‌شه؛ بیرون اومدیم: کیبورد بسته می‌شه
    LaunchedEffect(searchMode) {
        if (searchMode) {
            if (query.isEmpty()) {
                delay(320)
                try {
                    focusRequester.requestFocus()
                } catch (e: Exception) {
                }
            }
        } else {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(scheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // دکمه‌ی برگشت: فقط تو حالت جستجو آروم باز می‌شه
        Box(
            modifier = Modifier
                .width((44f * progress).dp)
                .height(44.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(44.dp)
                    .graphicsLayer { alpha = progress }
                    .clip(CircleShape)
                    .clickable(enabled = searchMode) { onSearchBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "بازگشت",
                    tint = scheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width((10f * progress).dp))

        // کادر جستجو: تو خونه فقط دکمه‌ست، تو جستجو فیلد نوشتن
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.surfaceVariant)
                .then(if (searchMode) Modifier else Modifier.clickable { onSearchClick() })
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "جستجو",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = "جستجو در اپدونی...",
                        color = scheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                if (searchMode || progress > 0.001f) {
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        enabled = searchMode,
                        singleLine = true,
                        textStyle = TextStyle(color = scheme.onSurface, fontSize = 14.sp),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }
            }
            if (searchMode && query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "پاک کردن",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable { onQueryChange("") }
                )
            }
        }

        Spacer(modifier = Modifier.width((10f * (1f - progress)).dp))

        // آواتار: تو حالت جستجو آروم جمع و محو می‌شه
        Box(
            modifier = Modifier
                .width((44f * (1f - progress)).dp)
                .height(44.dp)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(44.dp)
                    .graphicsLayer { alpha = 1f - progress }
                    .clip(CircleShape)
                    .background(if (isLoggedIn) scheme.primary else scheme.surfaceVariant)
                    .clickable(enabled = !searchMode) { onLoginClick() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoggedIn && userInitial.isNotBlank()) {
                    Text(
                        text = userInitial.first().uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "ورود",
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
