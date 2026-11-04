package com.appdony.app.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** یک گزینه‌ی انتخاب‌گر: اسم و توضیح (بالای ردیف نوشته می‌شه) + آیکن (تو ردیف) */
data class PillOption(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val iconSize: Dp = 24.dp
)

/**
 * انتخاب‌گر هم‌شکل انتخاب‌گر هوش مصنوعی: فقط آیکن‌ها تو یه ردیف تیره، یه دسته‌ی لغزنده رو گزینه‌ی فعاله.
 * اسم و توضیح گزینه‌ی فعال بالای ردیف، وسط، نوشته می‌شه و با عوض شدن گزینه عوض می‌شه.
 */
@Composable
fun PillSelector(options: List<PillOption>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    val selectedTint = glassSelectedIconTint()
    val current = options[selectedIndex.coerceIn(0, options.lastIndex)]

    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Crossfade(targetState = current, label = "pill_text") { o ->
            Column(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(o.title, color = scheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    o.desc,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .glassTrack(shape)
        ) {
            val cell = maxWidth / options.size
            // آفست توی RTL از راست حساب می‌شه، یعنی هم‌جهت با ترتیب آیکن‌ها
            val x = animateDpAsState(
                targetValue = cell * selectedIndex,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "pill_knob"
            )
            Box(
                modifier = Modifier
                    .offset(x = x.value)
                    .size(width = cell, height = 56.dp)
                    .padding(4.dp)
                    .glassKnob(RoundedCornerShape(14.dp))
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                options.forEachIndexed { i, o ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(i) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = o.icon,
                            contentDescription = o.title,
                            tint = if (i == selectedIndex) selectedTint else scheme.onSurfaceVariant,
                            modifier = Modifier.size(o.iconSize)
                        )
                    }
                }
            }
        }
    }
}
