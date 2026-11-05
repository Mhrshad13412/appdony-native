package com.appdony.app.data.model

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material.icons.outlined.Html
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.ViewColumn
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.ui.graphics.vector.ImageVector

/** دسته‌بندی بلوک‌ها توی کشوی بالا */
enum class BlockGroup(val label: String) {
    TEXT("متن"),
    MEDIA("رسانه"),
    DESIGN("طراحی"),
    WIDGETS("ویجت‌ها"),
    EMBED("جاسازی")
}

/** نوع ویرایشِ هر بلوک توی صفحه‌ی ادیتور */
enum class BlockEditing {
    PARAGRAPH,   // متن چندخطی ساده
    HEADING,     // سرنویس با انتخاب سطح
    LIST,        // هر خط یک مورد
    QUOTE,       // نقل‌قول (خط رنگی کنارش)
    CODE,        // متن چپ‌چین و تک‌فاصله
    BUTTON,      // متن دکمه + لینک
    URL,         // فقط یک آدرس
    IMAGE,       // تصویر: انتخاب از گالری + متن جایگزین (alt)
    DIVIDER,     // بدون متن (خط جداکننده، فاصله و ...)
    PLACEHOLDER  // کارت جایگزین؛ ویرایش کاملش توی مرحله‌های بعد میاد
}

/**
 * همه‌ی بلوک‌هایی که توی کشوی بالا هستن. [wpName] اسم بلوک توی گوتنبرگ وردپرسه
 * (برای ذخیره‌ی نوشته توی مرحله‌ی بعد لازمه).
 */
enum class BlockType(
    val wpName: String,
    val label: String,
    val icon: ImageVector,
    val group: BlockGroup,
    val editing: BlockEditing,
    val ordered: Boolean = false
) {
    PARAGRAPH("core/paragraph", "پاراگراف", Icons.Outlined.Notes, BlockGroup.TEXT, BlockEditing.PARAGRAPH),
    HEADING("core/heading", "سرنویس", Icons.Outlined.Title, BlockGroup.TEXT, BlockEditing.HEADING),
    LIST("core/list", "فهرست", Icons.Outlined.FormatListBulleted, BlockGroup.TEXT, BlockEditing.LIST),
    ORDERED_LIST("core/list", "فهرست شماره‌دار", Icons.Outlined.FormatListNumbered, BlockGroup.TEXT, BlockEditing.LIST, ordered = true),
    QUOTE("core/quote", "نقل‌قول", Icons.Outlined.FormatQuote, BlockGroup.TEXT, BlockEditing.QUOTE),
    PULLQUOTE("core/pullquote", "نقل‌قول برجسته", Icons.Outlined.FormatQuote, BlockGroup.TEXT, BlockEditing.QUOTE),
    CODE("core/code", "کد", Icons.Outlined.Code, BlockGroup.TEXT, BlockEditing.CODE),
    PREFORMATTED("core/preformatted", "پیش‌قالب‌بندی", Icons.Outlined.Terminal, BlockGroup.TEXT, BlockEditing.CODE),
    VERSE("core/verse", "شعر", Icons.Outlined.MenuBook, BlockGroup.TEXT, BlockEditing.PARAGRAPH),
    TABLE("core/table", "جدول", Icons.Outlined.TableChart, BlockGroup.TEXT, BlockEditing.PLACEHOLDER),

    IMAGE("core/image", "تصویر", Icons.Outlined.Image, BlockGroup.MEDIA, BlockEditing.IMAGE),
    GALLERY("core/gallery", "گالری", Icons.Outlined.Collections, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),
    AUDIO("core/audio", "صوت", Icons.Outlined.MusicNote, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),
    VIDEO("core/video", "ویدیو", Icons.Outlined.Videocam, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),
    FILE("core/file", "پرونده", Icons.Outlined.AttachFile, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),
    COVER("core/cover", "کاور", Icons.Outlined.Wallpaper, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),
    MEDIA_TEXT("core/media-text", "رسانه و متن", Icons.Outlined.ViewColumn, BlockGroup.MEDIA, BlockEditing.PLACEHOLDER),

    BUTTON("core/buttons", "دکمه", Icons.Outlined.SmartButton, BlockGroup.DESIGN, BlockEditing.BUTTON),
    COLUMNS("core/columns", "ستون‌ها", Icons.Outlined.ViewColumn, BlockGroup.DESIGN, BlockEditing.PLACEHOLDER),
    GROUP("core/group", "گروه", Icons.Outlined.Layers, BlockGroup.DESIGN, BlockEditing.PLACEHOLDER),
    SEPARATOR("core/separator", "جداکننده", Icons.Outlined.HorizontalRule, BlockGroup.DESIGN, BlockEditing.DIVIDER),
    SPACER("core/spacer", "فاصله‌دهنده", Icons.Outlined.SpaceBar, BlockGroup.DESIGN, BlockEditing.DIVIDER),
    MORE("core/more", "ادامه مطلب", Icons.Outlined.MoreHoriz, BlockGroup.DESIGN, BlockEditing.DIVIDER),

    HTML("core/html", "HTML سفارشی", Icons.Outlined.Html, BlockGroup.WIDGETS, BlockEditing.CODE),
    SHORTCODE("core/shortcode", "کد کوتاه", Icons.Outlined.Code, BlockGroup.WIDGETS, BlockEditing.CODE),
    LATEST_POSTS("core/latest-posts", "آخرین نوشته‌ها", Icons.Outlined.Article, BlockGroup.WIDGETS, BlockEditing.PLACEHOLDER),
    CATEGORIES("core/categories", "فهرست دسته‌ها", Icons.Outlined.Category, BlockGroup.WIDGETS, BlockEditing.PLACEHOLDER),
    SEARCH("core/search", "جستجو", Icons.Outlined.Search, BlockGroup.WIDGETS, BlockEditing.PLACEHOLDER),

    EMBED("core/embed", "جاسازی", Icons.Outlined.Public, BlockGroup.EMBED, BlockEditing.URL),
    YOUTUBE("core/embed", "یوتیوب", Icons.Outlined.PlayCircleOutline, BlockGroup.EMBED, BlockEditing.URL),
    TWITTER("core/embed", "ایکس (توییتر)", Icons.Outlined.Public, BlockGroup.EMBED, BlockEditing.URL),
    TELEGRAM("core/embed", "تلگرام", Icons.Outlined.Send, BlockGroup.EMBED, BlockEditing.URL)
}

/**
 * یک بلوک توی ادیتور. [text] متن اصلیه (برای دکمه: متن دکمه، برای جاسازی: آدرس، برای تصویر: «شناسه|آدرس»)
 * و [extra] مقدار دومه (سرنویس: سطح ۲/۳/۴، دکمه: لینک، تصویر: متن جایگزین alt).
 */
data class EditorBlock(
    val id: Long,
    val type: BlockType,
    val text: String = "",
    val extra: String = ""
)

/**
 * شمارش استفاده از بلوک‌ها برای «۶ بلوک پرکاربرد» توی کشوی پایین.
 * اولش پیش‌فرض‌ها هستن و هرچی بیشتر استفاده بشه، جلوتر میاد.
 */
object BlockUsage {
    private const val PREFS = "appdony_editor_usage"

    private val defaults = listOf(
        BlockType.PARAGRAPH, BlockType.HEADING, BlockType.IMAGE,
        BlockType.LIST, BlockType.BUTTON, BlockType.QUOTE
    )

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun record(context: Context, type: BlockType) {
        val p = prefs(context)
        p.edit().putInt(type.name, p.getInt(type.name, 0) + 1).apply()
    }

    fun topSix(context: Context): List<BlockType> {
        val p = prefs(context)
        return BlockType.entries
            .sortedByDescending { type ->
                val index = defaults.indexOf(type)
                val bonus = if (index >= 0) 3.0 + (defaults.size - index) * 0.01 else 0.0
                p.getInt(type.name, 0) + bonus
            }
            .take(6)
    }
}
