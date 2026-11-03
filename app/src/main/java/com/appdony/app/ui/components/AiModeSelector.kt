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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.VisibilityOff
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** حالت‌های دستیار؛ همیشه فقط یکی فعاله (مثل دنده‌ی اتومات) */
enum class AiMode(val title: String, val desc: String, val icon: ImageVector) {
    OFF("خاموش", "پاپی و دکمه‌ی هوش مصنوعی هیچ‌جا نشون داده نمی‌شن.", Icons.Outlined.VisibilityOff),
    CORNER("ثابت گوشه", "یه دکمه‌ی ثابت گوشه‌ی صفحه، بالای منو. اول با نوشته‌ی «هوش مصنوعی» باز می‌شه و بعد به دایره جمع می‌شه.", Icons.Outlined.ChatBubbleOutline),
    MENU("ثابت منو", "دکمه‌ی گرد هوش مصنوعی وسط منوی پایین.", Icons.Outlined.Dashboard),
    FLOATING("متحرک", "پاپی توی برنامه می‌چرخه و می‌تونی بکشیش و هرجا خواستی بذاریش.", Icons.Outlined.Pets),
    OUTSIDE("متحرک بیرون برنامه", "پاپی روی بقیه‌ی برنامه‌ها هم می‌چرخه. اجازه‌ی «نمایش روی سایر برنامه‌ها» لازمه.", Icons.Outlined.Layers)
}

/**
 * انتخاب‌گر دنده‌ای: فقط آیکن‌ها تو یه ردیف، یه دسته‌ی لغزنده رو حالت فعاله.
 * اسم و توضیح حالت فعال بالای ردیف، وسط، نوشته می‌شه و با عوض شدن حالت عوض می‌شه
 * (اینجوری متن لازم نیست کنار هر آیکن جا بشه).
 */
@Composable
fun AiModeSelector(selected: AiMode, onSelect: (AiMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val modes = AiMode.values()
    val shape = RoundedCornerShape(18.dp)

    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Crossfade(targetState = selected, label = "ai_mode_text") { m ->
            Column(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(m.title, color = scheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    m.desc,
                    color = scheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(shape)
                .background(scheme.background)
        ) {
            val cell = maxWidth / modes.size
            // آفست توی RTL از راست حساب می‌شه، یعنی هم‌جهت با ترتیب آیکن‌ها
            val x = animateDpAsState(
                targetValue = cell * selected.ordinal,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
                label = "ai_mode_knob"
            )
            Box(
                modifier = Modifier
                    .offset(x = x.value)
                    .size(width = cell, height = 56.dp)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.primary)
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                modes.forEach { m ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSelect(m) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = m.icon,
                            contentDescription = m.title,
                            tint = if (m == selected) Color.White else scheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
