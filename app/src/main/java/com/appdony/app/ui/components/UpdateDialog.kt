package com.appdony.app.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.appdony.app.util.UpdateHelper
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


/**
 * چک آپدیت خودکار: موقع باز شدن برنامه آخرین ریلیز گیت‌هاب رو می‌گیره
 * و اگه جدیدتر بود کارت شناور آپدیت (پشتیبان پشت کامپیوتر) رو نشون می‌ده.
 */
@Composable
fun UpdateHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var info by remember { mutableStateOf<UpdateHelper.UpdateInfo?>(null) }
    var show by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(-1f) }      // ‎-۱ = هنوز دانلود شروع نشده
    var file by remember { mutableStateOf<File?>(null) }
    var failed by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(Unit) {
        val found = UpdateHelper.checkForUpdate(context)
        if (found != null) {
            info = found
            show = true
        }
    }

    val current = info
    if (!show || current == null) return

    UpdateDialog(
        info = current,
        progress = progress,
        failed = failed,
        onPrimary = {
            val ready = file
            if (ready != null) {
                UpdateHelper.install(context, ready)
            } else if (job?.isActive != true) {
                failed = false
                progress = 0f
                job = scope.launch {
                    val f = UpdateHelper.download(context, current) { progress = it }
                    if (f == null) {
                        failed = true
                        progress = -1f
                        show = true // اگه کارت کوچیک شده بود، برای تلاش دوباره برمی‌گرده
                    } else {
                        file = f
                        progress = 1f
                        show = true // «بفرما» رو ببینه، حتی اگه کارت کوچیک شده بود
                        UpdateHelper.install(context, f)
                    }
                }
            }
        },
        onLater = {
            job?.cancel()
            show = false
        },
        // علامت «–»: کارت می‌ره کنار ولی دانلود ادامه داره و خودش نصب رو شروع می‌کنه
        onMinimize = { show = false }
    )
}

@Composable
private fun UpdateDialog(
    info: UpdateHelper.UpdateInfo,
    progress: Float,
    failed: Boolean,
    onPrimary: () -> Unit,
    onLater: () -> Unit,
    onMinimize: () -> Unit
) {
    val downloading = progress in 0f..0.9999f
    val finished = progress >= 1f
    // مرحله‌ی انیمیشن: کد زدن ← (نصب رو زد) بلند می‌شه و 👌 می‌ده ← (دانلود تموم شد) «بفرما»
    val stage = when {
        finished -> UpdateStage.Ready
        downloading -> UpdateStage.Confirm
        else -> UpdateStage.Coding
    }

    Dialog(
        onDismissRequest = { if (downloading) onMinimize() else onLater() },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = !downloading,
            usePlatformDefaultWidth = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                window.setDimAmount(0.45f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    val params = window.attributes
                    params.blurBehindRadius = 28
                    window.attributes = params
                }
            }
        }

        var visible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { visible = true }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(220)) +
                    scaleIn(initialScale = 0.88f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow)) +
                    slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it / 8 }
            ) {
                // همون استایل منوی پایین: گوشه‌ی گرد ۳۲، پس‌زمینه‌ی surface، خط دور کم‌رنگ و سایه
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = glassStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shadowElevation = 8.dp
                ) {
                  Box {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        UpdateDevScene(
                            stage = stage,
                            progress = progress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(200f / 135f)
                                .clip(RoundedCornerShape(22.dp))
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = when (stage) {
                                UpdateStage.Coding -> "نسخه جدید اومده!"
                                UpdateStage.Confirm -> "عالیه، دارم می‌گیرمش…"
                                UpdateStage.Ready -> "بفرما، آماده‌ست!"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        VersionChip("نسخه ${info.latestVersion}")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = info.changelog,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (failed) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "دانلود نشد؛ اینترنتت رو چک کن و دوباره بزن.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        if (downloading) {
                            ProgressPill(progress)
                        } else {
                            NavPill(
                                text = when {
                                    finished -> "نصب نسخه جدید"
                                    failed -> "تلاش دوباره"
                                    else -> "دانلود و نصب"
                                },
                                filled = true,
                                onClick = onPrimary,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            NavPill(
                                text = "بعداً",
                                filled = false,
                                onClick = onLater,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    MinimizeButton(
                        onClick = if (downloading) onMinimize else onLater,
                        modifier = Modifier.align(Alignment.TopEnd).padding(26.dp)
                    )
                  }
                }
            }
        }
    }
}

/** علامت «–» گوشه‌ی کارت: کارت رو کنار می‌ذاره (دانلود ادامه پیدا می‌کنه) */
@Composable
private fun MinimizeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.40f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(12.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.White.copy(alpha = 0.9f))
        )
    }
}

@Composable
private fun VersionChip(text: String) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .glassBorder(1.5.dp, primary, shape)
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Text(text, color = primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** دکمه با ظاهر اندیکاتور منوی پایین */
@Composable
private fun NavPill(text: String, filled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(shape)
            .background(if (filled) primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface)
            .border(
                1.5.dp,
                if (filled) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (filled) FontWeight.SemiBold else FontWeight.Medium,
            color = if (filled) primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressPill(progress: Float) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(primary.copy(alpha = 0.14f))
            .glassBorder(1.5.dp, primary, shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(primary.copy(alpha = 0.30f))
                .align(Alignment.CenterStart)
        )
        Text(
            text = "در حال دانلود… ${(progress * 100).roundToInt()}٪",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = primary
        )
    }
}
