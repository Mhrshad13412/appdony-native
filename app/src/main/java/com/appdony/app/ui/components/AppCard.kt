package com.appdony.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.PostMemory
import com.appdony.app.data.model.PostMeta
import com.appdony.app.data.model.cachedMeta
import com.appdony.app.data.model.cardCategory
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.model.gameCardName
import com.appdony.app.data.model.isJustPublished
import com.appdony.app.data.model.isJustUpdated
import com.appdony.app.data.model.meta
import com.appdony.app.ui.theme.AppdonyCardBg
import com.appdony.app.ui.theme.AppdonyTextMuted
import com.appdony.app.ui.theme.AppdonyTextPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val CardWidth = 122.dp

// جای ردیف دسته و ردیف اندروید/نسخه همیشه رزرو می‌شه تا کارت‌ها هم‌قد باشن.
// این‌ها «حداقل» ارتفاعن نه ارتفاع ثابت: متن فارسی هیچ‌وقت از پایین بریده نمی‌شه
private val CardCategoryMinHeight = 18.dp
private val CardMetaMinHeight = 24.dp

// ════════════════════════════════════════════════════════════════════════════
// ⚠️ مهم — لود اولویت‌دار کارت‌ها (از ساده به سخت)؛ تحت هیچ عنوان حذف یا ساده نشه ⚠️
// این مراحل «گذر زمان» نیستن؛ هیچ تأخیر ساختگی‌ای نیست و هر بخش همون لحظه‌ای که داده‌ش آماده شد میاد:
//   ۱) اسکلت ردیف (CardSkeleton تو HomeScreen، تا رسیدن لیست)
//   ۲) قاب پیش‌فرض خود کارت: همون لحظه
//   ۳) متن‌ها (اسم و دسته): همون لحظه، چون از قبل تو حافظه‌ن
//   ۴) اندروید/نسخه: پشت صحنه حساب می‌شه، تا اون موقع لودینگ خودش رو داره
//   ۵) عکس: از شبکه/کش میاد (کم‌کیفیت و سبک: size 240 + RGB565)، تا اون موقع لودینگ خودش رو داره
// کاربر تو بدترین شرایط هم همیشه یه چیزی می‌بینه و هیچ‌وقت منتظر یه صفحه‌ی خالی نمی‌مونه.
// ════════════════════════════════════════════════════════════════════════════

/** اندروید مورد نیاز و نسخه‌ی پست؛ اگه قبلاً حساب شده باشه فوراً میاد، وگرنه پشت صحنه حساب می‌شه */
@Composable
fun rememberPostMeta(post: Post): PostMeta {
    val meta by produceState(initialValue = post.cachedMeta() ?: PostMeta(), key1 = post.id) {
        value = withContext(Dispatchers.Default) { post.meta() }
    }
    return meta
}

/** کارت بازی/برنامه از روی خود پست: عکس، «جدید»، اسم، اندروید و نسخه */
@Composable
fun PostCard(post: Post, onClick: () -> Unit, modifier: Modifier = Modifier, fillWidth: Boolean = false) {
    // اندروید/نسخه پشت صحنه حساب می‌شن؛ تا آماده نشدن، کارت جاش یه لودینگ نشون می‌ده (null = هنوز آماده نیست)
    val meta by produceState<PostMeta?>(initialValue = post.cachedMeta(), key1 = post.id) {
        value = withContext(Dispatchers.Default) { post.meta() }
    }
    // پست برای باز شدن سریع صفحه‌ی جزئیات تو حافظه نگه داشته می‌شه
    remember(post.id) { PostMemory.put(post) }
    val title = remember(post.id) { post.cleanTitle() }
    val image = remember(post.id) { try { post.featuredImageUrl() } catch (e: Exception) { null } }
    val category = remember(post.id) { post.cardCategory() }
    val isNew = remember(post.id, post.date, post.modified) { post.isJustPublished() }
    val isUpdated = remember(post.id, post.date, post.modified) { post.isJustUpdated() }

    AppCard(
        title = title,
        imageUrl = image,
        onClick = onClick,
        modifier = modifier,
        category = category,
        androidReq = meta?.androidReq,
        version = meta?.version,
        metaLoading = meta == null,
        isNew = isNew,
        isUpdated = isUpdated,
        fillWidth = fillWidth
    )
}

@Composable
fun AppCard(
    title: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    category: String? = null,
    androidReq: String? = null,
    version: String? = null,
    metaLoading: Boolean = false,
    isNew: Boolean = false,
    isUpdated: Boolean = false,
    fillWidth: Boolean = false
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val shownTitle = remember(title) { title.gameCardName() }
    // عکس کم‌کیفیت و سبک: سایز کوچیک + RGB565 (فقط برای عکس‌های بدون شفافیت) + کراس‌فید نرم
    val imageRequest = remember(imageUrl) {
        ImageRequest.Builder(context).data(imageUrl).size(240).allowRgb565(true).crossfade(200).build()
    }

    val sizing = if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(CardWidth)

    Column(
        modifier = modifier.then(sizing).appdonyCard().clickable { onClick() }.padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.12f).clip(RoundedCornerShape(16.dp))) {
            // عکس همون لحظه درخواست می‌شه؛ تا برسه، لودینگ خودش رو نشون می‌ده
            SubcomposeAsyncImage(
                model = imageRequest, contentDescription = shownTitle, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                loading = { ShimmerBox(modifier = Modifier.fillMaxSize(), cornerRadius = 0) },
                error = {
                    Box(modifier = Modifier.fillMaxSize().background(AppdonyCardBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Warning, "خطا", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(28.dp))
                    }
                },
                success = { SubcomposeAsyncImageContent() }
            )
            if (isNew) NewBadge(modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
            else if (isUpdated) UpdatedBadge(modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = shownTitle, color = AppdonyTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            lineHeight = 15.sp, modifier = Modifier.fillMaxWidth()
        )
        // اسم دسته درست زیر اسم بازی/برنامه؛ جاش همیشه رزروئه ولی بدون برش، تا حروف فارسی له نشن
        Box(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = CardCategoryMinHeight),
            contentAlignment = Alignment.Center
        ) {
            if (!category.isNullOrBlank()) {
                Text(
                    text = category, color = primary, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ردیف اندروید/نسخه؛ جاش همیشه رزروئه (چه لودینگ، چه آماده، چه خالی) تا کارت‌ها هم‌قد بمونن
        Box(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = CardMetaMinHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (metaLoading) {
                // لودینگ اندروید و نسخه تا حسابشون تموم بشه
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(modifier = Modifier.width(34.dp).height(10.dp), cornerRadius = 4)
                        ShimmerBox(modifier = Modifier.width(34.dp).height(10.dp), cornerRadius = 4)
                    }
                }
            } else if (androidReq != null || version != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (androidReq != null) MetaItem(Icons.Filled.Android, androidReq, primary)
                        if (version != null) MetaItem(Icons.Filled.Bolt, version, primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String, tint: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text, color = AppdonyTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = TextStyle(textDirection = TextDirection.Ltr)
        )
    }
}

/** نشان «جدید» با همون استایل منوی پایین */
@Composable
internal fun NewBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.92f))
            .background(scheme.primary.copy(alpha = 0.14f))
            .glassBorder(1.dp, scheme.primary, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = "جدید", color = scheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/** نشان «به‌روز شد» برای کارت‌هایی که تازه آپدیت شدن */
@Composable
internal fun UpdatedBadge(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.92f))
            .background(scheme.primary.copy(alpha = 0.14f))
            .glassBorder(1.dp, scheme.primary, shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Update, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(11.dp))
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = "به‌روز شد", color = scheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
