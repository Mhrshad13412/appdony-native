package com.appdony.app.data.model

import com.appdony.app.util.HtmlParser
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/**
 * یک ردیف بلوک «برترین‌ها»؛ عنوان، آیکون، دسته و امتیاز دقیقاً همون چیزیه که سایت نشون می‌ده.
 * [postId] آی‌دی پست برای باز شدن صفحه‌ی جزئیاته (null یعنی پیدا نشده) و [link] آدرس پست توی سایت.
 */
data class TopChartEntry(
    val postId: Int?,
    val title: String,
    val imageUrl: String?,
    val category: String,
    val rating: String?,
    val link: String = ""
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

    private data class Scored(val post: Post, val rating: Double?, val votes: Long)

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

    private fun fame(e: Scored): Int =
        maxOf(Popularity.score(e.post.cleanTitle()), PopularityRemote.score(e.post.id) ?: 0)

    /** سنگینه (پارس HTML)؛ روی ترد پس‌زمینه صدا زده بشه */
    fun build(posts: List<Post>, size: Int = SIZE): TopChartsData {
        val entries = posts.map { p ->
            val s = statsOf(p)
            Scored(post = p, rating = s.rating, votes = s.votes)
        }

        val installed = entries.sortedWith(
            compareByDescending<Scored> { it.votes }
                .thenByDescending { fame(it) }
                .thenByDescending { it.rating ?: 0.0 }
        ).take(size)

        val rated = entries.filter { it.rating != null }.sortedWith(
            compareByDescending<Scored> { it.rating ?: 0.0 }
                .thenByDescending { it.votes }
                .thenByDescending { fame(it) }
        ).take(size)

        return TopChartsData(installed = installed.map(::toEntry), rated = rated.map(::toEntry))
    }

    private fun toEntry(e: Scored): TopChartEntry {
        val post = e.post
        // پست برای باز شدن سریع صفحه‌ی جزئیات تو حافظه نگه داشته می‌شه (مثل PostCard)
        PostMemory.put(post)
        return TopChartEntry(
            postId = post.id,
            title = post.cleanTitle(),
            imageUrl = try { post.featuredImageUrl() } catch (ex: Exception) { null },
            category = post.cardCategory().orEmpty(),
            rating = e.rating?.let { String.format(Locale.US, "%.1f", it) },
            link = post.link.orEmpty()
        )
    }
}

/**
 * خوندن بلوک «برترین‌ها» از HTML صفحه‌ی سایت (section با data-charts):
 * تب «بیشترین نصب» (data-list=installs) و تب «بیشترین امتیاز» (data-list=rating).
 */
object TopChartsScraper {

    /** null یعنی بلوک توی صفحه نبود یا هر دو لیست خالی بود */
    fun parse(html: String, baseUrl: String): TopChartsData? {
        val doc = Jsoup.parse(html, baseUrl)
        val section = doc.selectFirst("section[data-charts]") ?: return null
        fun rows(name: String): List<TopChartEntry> =
            section.select(".charts-list[data-list=$name] a.chart-row")
                .mapNotNull { rowOf(it) }
                .take(TopChartsRanker.SIZE)

        val data = TopChartsData(installed = rows("installs"), rated = rows("rating"))
        return if (data.isEmpty) null else data
    }

    private fun rowOf(a: Element): TopChartEntry? {
        val link = a.absUrl("href")
        val title = a.selectFirst(".chart-info h3")?.text()?.trim().orEmpty()
        if (link.isBlank() || title.isBlank()) return null
        val image = a.selectFirst(".chart-icon img")?.absUrl("src")?.takeIf { it.isNotBlank() }
        val category = a.selectFirst(".chart-info .meta-line")?.ownText()
            ?.replace("·", " ")?.replace("\u00A0", " ")?.trim().orEmpty()
        val rating = a.selectFirst(".chart-info .rating")?.text()?.trim()?.takeIf { it.isNotBlank() }
        return TopChartEntry(
            postId = null,
            title = title,
            imageUrl = image,
            category = category,
            rating = rating,
            link = link
        )
    }

    /** نامک رمزگشایی‌شده و حروف‌کوچک‌ی آدرس پست؛ کلید وصل کردن ردیف سایت به پست توی API */
    fun slugKey(link: String): String? =
        link.toHttpUrlOrNull()?.pathSegments?.lastOrNull { it.isNotBlank() }?.lowercase()
}
