package com.appdony.app.ui.components

import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.cleanTitle
import com.appdony.app.util.AssistantSay
import com.appdony.app.util.SayKind
import kotlin.random.Random

// ───────────────────────── جمله‌ها ─────────────────────────

internal fun String.stripEmoji(): String =
    replace(Regex("[\\p{So}\\uFE0F\\u200D]"), "").replace(Regex("\\s+"), " ").trim()

internal object AssistantPhrases {
    private val general = listOf(
        "میو! اگه بازی خاصی تو ذهنته، از دکمه‌ی جستجو اسمش رو بزن 🔍",
        "خسته نشدی؟ یه نفس عمیق بکش، بعد برگرد سراغ دانلود 😄",
        "هر وقت خواستی با من چت کن؛ بازی خوب هم پیدا می‌کنم!",
        "فضای گوشیت پره؟ قبل از نصب بازی‌های بزرگ یه نگاهی بهش بنداز.",
        "اگه نصب نشد، گزینه‌ی «نصب از منابع ناشناس» رو چک کن.",
        "منو بگیر و هر جا خواستی ببر؛ ناراحت نمی‌شم 😉",
        "از تنظیمات می‌تونی رنگ برنامه و اندازه‌ی نوشته‌ها رو عوض کنی.",
        "اگه مزاحمتم، از تنظیمات می‌تونی منو مخفی کنی... ولی دلم برات تنگ می‌شه.",
        "علاقه‌مندی‌هات رو از منوی حساب کاربری پیدا می‌کنی.",
        "فایل‌هایی که دانلود کردی توی تب «دانلودها» منتظر نصبن.",
        "یه بازی خوب مثل یه چای داغه؛ عجله نکن، خوب انتخابش کن ☕",
        "قبل از دانلود، نظر بقیه‌ی کاربرها پایین صفحه‌ی هر پست رو بخون.",
        "دانلود قطع شد؟ اینترنت یا VPN رو عوض کن و دوباره بزن.",
        "اینترنتت ضعیفه؟ دانلودهای سنگین رو با وای‌فای انجام بده.",
        "بعد از نصب، فایل نصبی رو پاک کن تا حافظه آزاد بشه.",
        "نسخه‌ی اندروید گوشی‌ت باید با نیازمندی بازی بخونه، وگرنه نصب نمی‌شه.",
        "پیشنهاد می‌خوای؟ از من بپرس، یکی جور می‌کنم 🎮",
        "من همین‌جام؛ اگه جایی گیر کردی فقط بزن روم.",
        "حوصله‌ات سر رفته؟ یه چرخی بین بازی‌ها بزن، حتماً چیزی پیدا می‌شه.",
        "بازی‌های سنگین رو با شارژ کافی نصب کن 🔋",
        "گاهی کش برنامه‌ها رو پاک کن، گوشی سبک‌تر می‌شه.",
        "اگه بازی باز نشد، یه بار گوشی رو ری‌استارت کن.",
        "تنها نیستی؛ من همیشه همین گوشه منتظرم 🐾",
        "حالت شب و روز رو هم می‌تونی از تنظیمات عوض کنی.",
        "کاموام رو دیدی؟ بدون اون خوابم نمی‌بره 🧶"
    ).map { AssistantSay(it.stripEmoji(), SayKind.TIP) }

    private val plus = listOf(
        "با خرید اپدونی پلاس به بازی‌های با کیفیت دست پیدا کن.",
        "بازی‌های ویژه و باکیفیت توی اپدونی پلاس منتظرتن.",
        "می‌خوای بازی‌های درجه‌یک داشته باشی؟ اپدونی پلاس رو بخر.",
        "اپدونی پلاس رو امتحان کن؛ بازی‌های باکیفیت‌تر اون‌جاست."
    ).map { AssistantSay(it, SayKind.PLUS) }

    private val games = listOf(
        "بازی‌ها دسته‌بندی دارن؛ اگه سبک خاصی دوست داری دسته‌ها رو ببین.",
        "قبل از دانلود، حجم بازی رو با فضای گوشیت مقایسه کن.",
        "بازی آفلاین می‌خوای یا آنلاین؟ از من بپرس!",
        "بعضی بازی‌ها فایل دیتای جدا دارن؛ توضیحات پست رو حتماً بخون.",
        "بازی نصب شد ولی باز نمی‌شه؟ فایل دیتا رو چک کن.",
        "اینجا کلی بازی هست؛ اگه گیج شدی بگو یکی برات انتخاب کنم 🎲",
        "بازی بزرگ؟ اول دانلود، بعد نصب؛ صبور باش.",
        "یادت نره استراحت هم بکنی، چشمات حق دارن 👀"
    ).map { AssistantSay(it.stripEmoji(), SayKind.TIP) }

    private val apps = listOf(
        "دنبال یه برنامه‌ی خاص می‌گردی؟ جستجو سریع‌ترین راهه.",
        "قبل از نصب، نسخه‌ی اندرویدی که برنامه لازم داره رو چک کن.",
        "برای هر کاری برنامه هست؛ بگو چی لازم داری تا دنبالش بگردم.",
        "برنامه‌هات رو به‌روز نگه دار، امنیتشون بیشتر می‌شه.",
        "نصب از منبع ناشناس؟ یه بار اجازه‌اش رو بده و تمام.",
        "برنامه‌ی جدید نصب می‌کنی؟ دسترسی‌هاش رو یه نگاه بنداز."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val blog = listOf(
        "اینجا مقاله‌های آموزشیه؛ جواب خیلی از مشکلات نصب و دانلود هم توشه.",
        "وبلاگ رو ورق بزن، شاید چیز جالبی پیدا کنی.",
        "اخبار تازه‌ی دنیای بازی و برنامه همین‌جاست.",
        "سؤالی داشتی؟ قبل از گشتن بین مقاله‌ها از من هم بپرس."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val downloads = listOf(
        "دانلودهات اینجاست؛ هر وقت خواستی نصبشون کن.",
        "دانلود کامل شد؟ دکمه‌ی «نصب» رو بزن.",
        "فایل‌های نصب‌شده رو از اینجا پاک کن تا حافظه خالی بشه.",
        "دانلود گیر کرده؟ پاکش کن و دوباره شروع کن.",
        "دانلود کند شد؟ اینترنت رو عوض کن."
    ).map { AssistantSay(it, SayKind.DOWNLOAD) }

    private val search = listOf(
        "اسم بازی رو فارسی یا انگلیسی امتحان کن؛ گاهی یکیش بهتر جواب می‌ده.",
        "نتیجه‌ای نبود؟ یه کلمه‌ی کوتاه‌تر بنویس.",
        "املا رو چک کن، گاهی یه حرف همه‌چی رو عوض می‌کنه 😅",
        "جستجوهای قبلیت همین‌جا ذخیره می‌شه."
    ).map { AssistantSay(it.stripEmoji(), SayKind.SEARCH) }

    private val detail = listOf(
        "قبل از دانلود، توضیحات و نیازمندی‌ها رو بخون.",
        "نظرها رو هم ببین؛ تجربه‌ی بقیه کمکت می‌کنه.",
        "پایین صفحه مطالب مشابه هست؛ شاید بهتر باشه.",
        "دکمه‌ی دانلود آماده‌ست؛ فقط فضای گوشی رو چک کن."
    ).map { AssistantSay(it, SayKind.TIP) }

    private val recent = ArrayDeque<String>()

    fun next(route: String?): AssistantSay {
        // گاهی (حدود ۱۴٪) تبلیغ ملایم اپدونی پلاس
        val pool = if (Random.nextFloat() < 0.14f) {
            plus
        } else {
            val r = route.orEmpty()
            val extra = when {
                r.startsWith("games") -> games
                r.startsWith("apps") -> apps
                r.startsWith("blog") -> blog
                r.startsWith("downloads") -> downloads
                r.startsWith("search") -> search
                r.startsWith("detail") -> detail
                else -> emptyList()
            }
            general + extra + extra
        }
        val fresh = pool.filter { it.text !in recent }.ifEmpty { pool }
        val pick = fresh.random()
        recent.addLast(pick.text)
        while (recent.size > 10) recent.removeFirst()
        return pick
    }
}

internal fun shortName(title: String): String {
    val quoted = Regex("[“”\"«»](.+?)[“”\"«»]").find(title)?.groupValues?.getOrNull(1)?.trim()
    if (!quoted.isNullOrBlank()) return quoted.take(45)
    return title.removePrefix("دانلود").trim().take(45)
}

/** یه اسم تصادفی از پست‌های خود اپدونی پیشنهاد می‌ده (اگه اینترنت نبود null) */
internal suspend fun fetchSuggestion(): AssistantSay? = try {
    val titles = ApiClient.api.getPosts(perPage = 12)
        .map { shortName(it.cleanTitle()) }
        .filter { it.length > 2 }
    val pick = titles.randomOrNull()
    if (pick == null) null
    else AssistantSay("این رو دیدی؟ «$pick» رو توی جستجو بزن.", SayKind.SUGGEST)
} catch (e: Exception) {
    null
}
