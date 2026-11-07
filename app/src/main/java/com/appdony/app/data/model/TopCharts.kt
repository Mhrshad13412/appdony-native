package com.appdony.app.data.model

import com.appdony.app.util.HtmlParser
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/** یک ردیف بلوک «برترین‌ها»: پست + امتیاز (۱ تا ۵) + تعداد رأی */
data class TopChartEntry(
    val post: Post,
    val rating: Double?,
    val votes: Long
)

/** دو لیست ۱۰ تایی بلوک «برترین‌ها» */
data class TopChartsData(
    val installed: List<TopChartEntry>,
    val rated: List<TopChartEntry>
) {
    val isEmpty: Boolean get() = installed.isEmpty() && rated.isEmpty()
}

/**
 * رتبه‌بندی بلوک «برترین‌ها»؛ بدون هوش مصنوعی و فقط از روی متن خود پست‌ها.
 * - بیشترین امتیاز: امتیاز (۱ تا ۵) از جمله‌ی «امتیاز ۷۵ از ۱۰۲۲۸ رأی» توی متن پست؛ مساوی‌ها با تعداد رأی و بعد شهرت.
 * - بیشترین نصب: تعداد رأی/نظر همون پست (همون «تعداد نظرات» متاباکس امتیاز نمایشی توی وردپرس)؛
 *   مساوی‌ها (یا پست‌های بدون تعداد) با شهرت (Popularity + RAWG) و بعد امتیاز.
 */
object TopChartsRanker {

    const val SIZE = 10

    private data class Stats(val rating: Double?, val votes: Long)

    private val cache = ConcurrentHashMap<Int, Stats>()

    /** «10.2K» ، «1.5M» ، «1,200» ، «350» → عدد */
    private fun parseVotes(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        val t = raw.trim().replace(",", "").replace(" ", "").uppercase(Locale.US)
        val mult = when {
            t.endsWith("K") -> 1_000.0
            t.endsWith("M") -> 1_000_000.0
            else -> 1.0
        }
        val num = t.trimEnd('K', 'M').toDoubleOrNull() ?: return 0L
        return (num * mult).roundToLong()
    }

    private fun statsOf(post: Post): Stats {
        cache[post.id]?.let { return it }
        val stats = try {
            val info = HtmlParser.parsePostContent(post.content.rendered)
            Stats(info.rating?.toDoubleOrNull(), parseVotes(info.ratingCount))
        } catch (e: Exception) {
            Stats(null, 0L)
        }
        cache[post.id] = stats
        return stats
    }

    private fun fame(e: TopChartEntry): Int =
        maxOf(Popularity.score(e.post.cleanTitle()), PopularityRemote.score(e.post.id) ?: 0)

    /** سنگینه (پارس HTML)؛ روی ترد پس‌زمینه صدا زده بشه */
    fun build(posts: List<Post>, size: Int = SIZE): TopChartsData {
        val entries = posts.map { p ->
            val s = statsOf(p)
            TopChartEntry(post = p, rating = s.rating, votes = s.votes)
        }

        val installed = entries.sortedWith(
            compareByDescending<TopChartEntry> { it.votes }
                .thenByDescending { fame(it) }
                .thenByDescending { it.rating ?: 0.0 }
        ).take(size)

        val rated = entries.filter { it.rating != null }.sortedWith(
            compareByDescending<TopChartEntry> { it.rating ?: 0.0 }
                .thenByDescending { it.votes }
                .thenByDescending { fame(it) }
        ).take(size)

        return TopChartsData(installed = installed, rated = rated)
    }
}
