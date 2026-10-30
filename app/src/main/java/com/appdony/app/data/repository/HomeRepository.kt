package com.appdony.app.data.repository

import android.text.Html
import com.appdony.app.data.api.ApiClient
import com.appdony.app.data.model.Post

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
            if (uncategorized != null) {
                add(HomeSection(uncategorized.id, cleanName(uncategorized.name), root != null))
            }
            body.forEach {
                add(HomeSection(it.id, cleanName(it.name), root != null && !useChildren))
            }
        }

        return HomeStructure(root?.id, sections.distinctBy { it.id })
    }

    suspend fun loadFeatured(rootId: Int?): List<Post> {
        val posts = if (rootId != null) {
            api.getPostsByCategory(categoryId = rootId, perPage = 14)
        } else {
            api.getPosts(perPage = 14)
        }
        return posts.filterNot(::isPlusPost).take(8)
    }

    /** بلاک «اپدونی پلاس»: فقط پست‌های دسته‌ی مخصوص همون تب */
    suspend fun loadPlus(isGames: Boolean): List<Post> {
        val id = if (isGames) PLUS_GAMES_ID else PLUS_APPS_ID
        return api.getPostsByCategory(categoryId = id, perPage = 10)
    }

    suspend fun loadSection(section: HomeSection, rootId: Int?): List<Post> {
        val root = rootId
        return if (section.filterByRoot && root != null) {
            api.getPostsByCategory(categoryId = section.id, perPage = 40)
                .filter { root in it.categories }
                .filterNot(::isPlusPost)
                .take(12)
        } else {
            api.getPostsByCategory(categoryId = section.id, perPage = 16)
                .filterNot(::isPlusPost)
                .take(12)
        }
    }

    private fun isPlusPost(post: Post): Boolean = post.categories.any { it in PLUS_IDS }

    private fun cleanName(name: String): String {
        return Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}
