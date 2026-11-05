package com.appdony.app.data.model

/** یک ردیف از «لینک‌های دانلود» (برچسب، لینک فایل، صفحه‌ی دانلود) */
data class DlLink(
    val texto: String = "",
    val link: String = "",
    val page: String = ""
)

/** یک «باکس سفارشی» (عنوان + محتوای HTML) */
data class CustomBox(
    val title: String = "",
    val content: String = ""
)

/** فیلدهای متاباکس‌های playclone (اطلاعات برنامه، دانلود، رسانه، باکس‌ها، امتیاز، پلاس، منبع) */
data class AppInfo(
    val descripcion: String = "",
    val version: String = "",
    val tamano: String = "",
    val fechaActualizacion: String = "",
    val requerimientos: String = "",
    val consiguelo: String = "",
    val novedades: String = "",
    val dlOption: String = "direct-download", // direct-download | links
    val directDownload: String = "",
    val dlLinks: List<DlLink> = listOf(DlLink()),
    val screenshots: String = "",
    val videoId: String = "",
    val boxes: List<CustomBox> = listOf(CustomBox()),
    val ratingAverage: String = "",
    val ratingCount: String = "",
    val appdoniPlus: Boolean = false,
    val contentSource: String = "own", // own | 5play | other
    val contentSourceCustom: String = ""
)

/** فیلدهای Yoast SEO که از اپ ویرایش می‌شن */
data class SeoInfo(
    val focusKeyword: String = "",
    val title: String = "",
    val description: String = ""
)

/** تنظیمات خود نوشته (REST). [date] به شکل 2026-11-05 10:30 و خالی = همین حالا */
data class PostSettings(
    val slug: String = "",
    val date: String = "",
    val excerpt: String = "",
    val commentsOpen: Boolean = true,
    val categoryIds: Set<Int> = emptySet()
)

data class WpCategory(val id: Int, val name: String, val parent: Int)
