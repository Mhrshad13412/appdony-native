package com.appdony.app.data.model

/**
 * اولویت‌بندی بر اساس شهرت: بازی‌های معروف‌تر اول لیست میان.
 * بدون هوش مصنوعی؛ یه لیست امتیازدار از اسم‌های معروف (انگلیسی + فارسی).
 * بازی‌هایی که توی لیست نیستن امتیاز صفر می‌گیرن و ترتیب اصلیشون (جدیدترین اول) حفظ می‌شه.
 *
 * برای اضافه کردن بازی: اسمش رو (انگلیسی یا فارسی) به یکی از سه لیست پایین اضافه کن.
 * فاصله و حروف بزرگ/کوچیک مهم نیست.
 */
object Popularity {

    // ★★★ خیلی معروف
    private val TOP = listOf(
        "minecraft", "ماینکرافت", "pubg", "پابجی", "پاپجی", "clash of clans", "کلش اف کلنز", "کلش آف کلنز",
        "clash royale", "کلش رویال", "brawl stars", "برال استارز", "free fire", "فری فایر",
        "call of duty", "کال اف دیوتی", "کالاف دیوتی", "gta", "grand theft auto", "جی تی ای",
        "subway surfers", "among us", "roblox", "روبلاکس", "genshin", "terraria", "تراریا",
        "don't starve", "dont starve", "دونت استارو", "دانت استارو", "fortnite", "stardew valley",
        "استاردیو", "mobile legends", "geometry dash", "plants vs zombies", "candy crush",
        "asphalt", "need for speed", "efootball", "fifa", "ea sports fc", "dream league soccer",
        "shadow fight", "temple run", "hill climb racing", "mortal kombat", "assassin's creed",
        "assassins creed", "resident evil", "counter strike", "standoff", "modern combat"
    )

    // ★★ معروف
    private val MID = listOf(
        "hay day", "lords mobile", "rise of kingdoms", "state of survival", "last day on earth",
        "the walking dead", "dead trigger", "real racing", "csr racing", "traffic rider", "traffic racer",
        "car parking multiplayer", "carx", "angry birds", "fruit ninja", "cut the rope", "my talking tom",
        "talking tom", "bad piggies", "monument valley", "badlands", "limbo",
        "hollow knight", "dead cells", "slay the spire", "oxygen not included", "rimworld", "kingdom rush",
        "bloons", "clash mini", "archero", "zombie tsunami", "plague inc", "stick war", "dragon ball",
        "naruto", "pokemon", "sonic", "mario", "minion rush", "despicable me", "bully", "gangstar",
        "vice city", "san andreas", "hitman", "sniper 3d", "critical ops", "bullet force", "warzone",
        "world of tanks", "war thunder", "pixel gun", "block strike", "skyblock", "craftsman",
        "lokicraft", "growtopia", "slither", "agar", "hungry shark", "my singing monsters",
        "township", "farmville", "family island", "toca boca", "sims", "simcity", "football manager",
        "nba 2k", "wwe", "real boxing", "tekken", "injustice", "marvel", "batman", "spider",
        "dungeon hunter", "soul knight", "vampire survivors", "brotato", "fall guys", "stumble guys",
        "blockman go", "bed wars", "bedwars", "zepeto", "honkai", "arknights", "raid shadow legends"
    )

    // ★ نسبتاً شناخته‌شده
    private val LOW = listOf(
        "idle", "tycoon", "simulator", "survival", "zombie", "racing", "football", "soccer", "ninja",
        "dragon", "warrior", "legend", "hero", "kingdom", "craft", "shooter", "battle", "war"
    )

    private fun norm(s: String): String = buildString(s.length) {
        for (c in s.lowercase().replace('ي', 'ی').replace('ك', 'ک')) {
            if (c in 'a'..'z' || c in '0'..'9' || (c in '\u0600'..'\u06FF' && c != '\u0640')) append(c)
        }
    }

    private val topKeys = TOP.map(::norm).filter { it.length >= 3 }
    private val midKeys = MID.map(::norm).filter { it.length >= 3 }
    private val lowKeys = LOW.map(::norm).filter { it.length >= 3 }

    /** هرچی بزرگ‌تر، معروف‌تر. صفر یعنی توی لیست نیست */
    fun score(title: String): Int {
        val t = norm(title)
        if (t.isEmpty()) return 0
        if (topKeys.any { t.contains(it) }) return 100
        if (midKeys.any { t.contains(it) }) return 60
        if (lowKeys.any { t.contains(it) }) return 20
        return 0
    }
}

/** مرتب‌سازی پایدار: معروف‌ها اول، بقیه با همون ترتیب قبلی (جدیدترین اول) */
fun List<Post>.sortedByPopularity(): List<Post> =
    sortedByDescending { Popularity.score(it.cleanTitle()) }
