package com.appdony.app.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * چک آپدیت خودکار: موقع باز شدن برنامه آخرین ریلیز گیت‌هاب رو می‌گیره
 * و اگه جدیدتر بود دیالوگ گربه و کاموا رو نشون می‌ده.
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
                    } else {
                        file = f
                        progress = 1f
                        UpdateHelper.install(context, f)
                    }
                }
            }
        },
        onLater = {
            job?.cancel()
            show = false
        }
    )
}

@Composable
private fun UpdateDialog(
    info: UpdateHelper.UpdateInfo,
    progress: Float,
    failed: Boolean,
    onPrimary: () -> Unit,
    onLater: () -> Unit
) {
    val downloading = progress in 0f..0.9999f
    val finished = progress >= 1f

    Dialog(
        onDismissRequest = { if (!downloading) onLater() },
        properties = DialogProperties(
            dismissOnBackPress = !downloading,
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CatThrowingYarn()

                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "نسخه جدید اومده!",
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
                }
            }
        }
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
            .border(1.5.dp, primary, shape)
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
            .border(1.5.dp, primary, shape),
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

/** گربه‌ای که گلوله‌ی کاموا رو پرت می‌کنه؛ نخ کاموا پشت سرش کشیده می‌شه */
@Composable
private fun CatThrowingYarn() {
    val primary = MaterialTheme.colorScheme.primary
    val inf = rememberInfiniteTransition(label = "cat_yarn")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "throw"
    )
    val tailT by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tail"
    )
    val blinkT by inf.animateFloat(
        initialValue = 0f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                0f at 0
                0f at 2800
                1f at 2950
                0f at 3100
            }
        ),
        label = "blink"
    )

    Canvas(modifier = Modifier.size(width = 200.dp, height = 134.dp)) {
        // فضای طراحی ۱۲۰ واحد عرض داره؛ گربه روی بوم ۶۰×۶۰ کشیده شده
        val k = size.width / 120.dp.toPx()
        val u = 1.dp.toPx()
        // داخل withTransform نمی‌شه toPx صدا زد، پس مقدارها رو از همین‌جا حساب می‌کنیم
        val shiftX = 6.dp.toPx()
        val shiftY = 20.dp.toPx()

        // مرحله‌ی پرتاب: t بین ۰.۱۲ تا ۰.۸۸
        val q = ((t - 0.12f) / 0.76f).coerceIn(0f, 1f)
        val flying = t in 0.12f..0.88f
        val pawX = 36f
        val pawY = 52f
        val bx = 40f + 58f * q
        val by = 52f - 250f * q * (1f - q)
        val alpha = when {
            !flying -> 0f
            q < 0.08f -> q / 0.08f
            q > 0.85f -> (1f - q) / 0.15f
            else -> 1f
        }
        val paw = if (t < 0.2f) sin(PI * (t / 0.2f)).toFloat() else 0f
        val talk = (0.5f + 0.5f * sin(t * PI.toFloat() * 10f)) * 0.55f

        withTransform({
            scale(k, k, Offset.Zero)
            translate(shiftX, shiftY)
        }) {
            drawCat(
                CatPose(
                    face = 1f,
                    blink = blinkT,
                    ear = if (flying) 0.6f else 0f,
                    tail = (tailT - 0.5f) * 2f,
                    mouth = talk,
                    paw = paw,
                    lookX = if (flying) 0.3f + 0.6f * q else 0f,
                    lookY = if (flying) -0.8f * sin(PI * q).toFloat() else 0f,
                    scare = 0f,
                    happy = 0f,
                    wiggle = (tailT - 0.5f) * 2f,
                    yarn = primary
                )
            )

            if (alpha > 0f) {
                // نخ از پنجه‌ی گربه تا گلوله
                val thread = Path().apply {
                    moveTo(pawX * u, pawY * u)
                    quadraticBezierTo(
                        (pawX + bx) / 2f * u,
                        (maxOf(pawY, by) + 14f * (1f - q * 0.5f)) * u,
                        bx * u, by * u
                    )
                }
                drawPath(
                    thread,
                    primary.copy(alpha = 0.75f * alpha),
                    style = Stroke(width = 1.2f * u, cap = StrokeCap.Round)
                )
                drawYarnBall(Offset(bx * u, by * u), 5.2f * u, primary.copy(alpha = alpha), q * 720f)
            }
        }
    }
}
