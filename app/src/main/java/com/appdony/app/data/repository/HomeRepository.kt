package com.appdony.app.data.repository

import android.text.Html
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.CategoryNames
import com.appdony.app.data.model.Post
import com.appdony.app.data.model.PopularityRemote
import com.appdony.app.data.model.TopChartsData
import com.appdony.app.data.model.TopChartsRanker
import com.appdony.app.data.model.sortedByFame
import com.appdony.app.util.LatestOnly
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class HomeSection(
    val id: Int,
    val title: String,
    // اگر true باشه، فقط پست‌هایی که دسته‌ی اصلی تب (بازی/برنامه) رو هم دارن نشون داده می‌شن
    val filterByRoot: Boolean
)

data class HomeStructure(
    val rootId: Int?,
    val sections: List<HomeSection>
)

class HomeRepository {

    private val api get() = ApiClient.api

    companion object {
        // دسته‌ی «اپدونی پلاس» هر تب (آی‌دی دسته‌ها توی وردپرس)
        const val PLUS_GAMES_ID = 1515 // تب بازی‌ها (قبلاً «پوشیدنی‌ها»)
        const val PLUS_APPS_ID = 1528 // تب برنامه‌ها (قبلاً «تقویم»)
        private val PLUS_IDS = setOf(PLUS_GAMES_ID, PLUS_APPS_ID)

        // ردیف‌های ویژه‌ی بالای صفحه (آی‌دی منفی یعنی دسته‌ی واقعی نیست)
        const val SEC_POPULAR = -1
        const val SEC_NEWEST = -2
        const val SEC_UPDATED = -3
    }

    suspend fun loadStructure(isGames: Boolean): HomeStructure {
        val categories = api.getCategories()

        val rootSlug = if (isGames) "game" else "app"
        val otherRootSlug = if (isGames) "app" else "game"
        val root = categories.firstOrNull { it.slug == rootSlug }
        val otherRoot = categories.firstOrNull { it.slug == otherRootSlug }

        val hidden = mutableSetOf("game", "app", "appdony-plus")
        if (isGames) hidden.add("ai")

        val usable = categories.filter { it.count > 0 && it.slug !in hidden && it.id !in PLUS_IDS }
        val uncategorized = usable.firstOrNull { it.slug == "uncategorized" }
        val others = usable.filter { it.slug != "uncategorized" }

        // اگر زیردسته‌ی مستقیم برای بازی/برنامه وجود داشته باشه از همون‌ها استفاده می‌کنیم
        val children = if (root != null) others.filter { it.parent == root.id } else emptyList()
        val useChildren = children.isNotEmpty()
        val body = if (useChildren) {
            children
        } else {
            others.filter { otherRoot == null || it.parent != otherRoot.id }
        }

        val sections = buildList {
            add(HomeSection(SEC_POPULAR, "پربازدیدترین‌ها", false))
            add(HomeSection(SEC_NEWEST, "جدیدترین‌ها", false))
            add(HomeSection(SEC_UPDATED, "به‌روزترین‌ها", false))
            if (uncategorized != null) {
                add(HomeSection(uncategorized.id, cleanName(uncategorized.name), root != null))
            }
            body.forEach {
                add(HomeSection(it.id, cleanName(it.name), root != null && !useChildren))
            }
        }

        return HomeStructure(root?.id, sections.distinctBy { it.id })
    }

    /** بلاک «اپدونی پلاس»: فقط پست‌های دسته‌ی مخصوص همون تب */
    suspend fun loadPlus(isGames: Boolean): List<Post> {
        val id = if (isGames) PLUS_GAMES_ID else PLUS_APPS_ID
        return LatestOnly.latestPerApp(api.getPostsByCategory(categoryId = id, perPage = 20)).take(10)
    }

    /** بلوک «برترین‌ها»: ۱۰ تا از بیشترین نصب و ۱۰ تا از بیشترین امتیاز (فقط پست‌های همون تب، بدون پست‌های پلاس) */
    suspend fun loadTopCharts(rootId: Int?): TopChartsData {
        PopularityRemote.ensureFresh()
        val list = if (rootId != null) api.getPostsByCategory(categoryId = rootId, perPage = 60) else api.getPosts(perPage = 60)
        val unique = LatestOnly.latestPerApp(list.filterNot(::isPlusPost))
        // پارس متن پست‌ها سنگینه؛ دور از ترد اصلی
        return withContext(Dispatchers.Default) { TopChartsRanker.build(unique, TopChartsRanker.SIZE) }
    }

    // ⚠️ مهم — لود سبک و مرحله‌ای؛ تحت هیچ عنوان حذف نشه ⚠️
    // هر ردیف فقط وقتی به صفحه نزدیک شد (HomeViewModel.loadSection) و فقط با تعداد پست کم (۲۴ تا ۳۰) گرفته می‌شه
    // تا اول عنوان و اسکلت بیاد و بعد کارت‌ها یکی‌یکی (جزئیات مراحل: AppCard.kt)
    suspend fun loadSection(section: HomeSection, rootId: Int?): List<Post> {
        // آمار محبوبیت فقط برای ردیف‌های مرتب‌شده بر اساس شهرت لازمه؛ «جدیدترین/به‌روزترین» منتظرش نمی‌مونن
        if (section.id != SEC_NEWEST && section.id != SEC_UPDATED) PopularityRemote.ensureFresh()
        val root = rootId
        if (section.id < 0) return loadSpecial(section.id, root)
        return if (section.filterByRoot && root != null) {
            LatestOnly.latestPerApp(
                api.getPostsByCategory(categoryId = section.id, perPage = 30)
                    .filter { root in it.categories }
                    .filterNot(::isPlusPost)
            ).sortedByFame().take(12)
        } else {
            LatestOnly.latestPerApp(
                api.getPostsByCategory(categoryId = section.id, perPage = 24)
                    .filterNot(::isPlusPost)
            ).sortedByFame().take(12)
        }
    }

    /** ردیف‌های «پربازدیدترین‌ها»، «جدیدترین‌ها» و «به‌روزترین‌ها» */
    private suspend fun loadSpecial(id: Int, root: Int?): List<Post> {
        val posts = when (id) {
            SEC_POPULAR -> {
                PopularityRemote.ensureFresh()
                val list = if (root != null) api.getPostsByCategory(categoryId = root, perPage = 40) else api.getPosts(perPage = 40)
                LatestOnly.latestPerApp(list.filterNot(::isPlusPost)).sortedByFame()
            }
            SEC_NEWEST -> {
                val list = if (root != null) api.getPostsByCategory(categoryId = root, perPage = 24) else api.getPosts(perPage = 24)
                LatestOnly.latestPerApp(list.filterNot(::isPlusPost))
            }
            else -> {
                if (root == null) return emptyList()
                // مرتب‌شده بر اساس آخرین ویرایش (orderby=modified)، بعد از هر اپ فقط یک کارت
                LatestOnly.latestPerAppByEdit(
                    api.getPostsByCategory(categoryId = root, perPage = 30, orderBy = "modified")
                        .filterNot(::isPlusPost)
                )
            }
        }
        return posts.take(12)
    }

    private fun isPlusPost(post: Post): Boolean = post.categories.any { it in PLUS_IDS }

    private fun cleanName(name: String): String {
        return CategoryNames.pretty(Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim())
    }
}
