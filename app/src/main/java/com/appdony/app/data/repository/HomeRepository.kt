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

    suspend fun loadStructure(isGames: Boolean): HomeStructure {
        val categories = api.getCategories()

        val rootSlug = if (isGames) "game" else "app"
        val otherRootSlug = if (isGames) "app" else "game"
        val root = categories.firstOrNull { it.slug == rootSlug }
        val otherRoot = categories.firstOrNull { it.slug == otherRootSlug }

        val hidden = mutableSetOf("game", "app", "appdony-plus")
        if (isGames) hidden.add("ai")

        val usable = categories.filter { it.count > 0 && it.slug !in hidden }
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
        return if (rootId != null) {
            api.getPostsByCategory(categoryId = rootId, perPage = 8)
        } else {
            api.getPosts(perPage = 8)
        }
    }

    suspend fun loadSection(section: HomeSection, rootId: Int?): List<Post> {
        val root = rootId
        return if (section.filterByRoot && root != null) {
            api.getPostsByCategory(categoryId = section.id, perPage = 40)
                .filter { root in it.categories }
                .take(12)
        } else {
            api.getPostsByCategory(categoryId = section.id, perPage = 12)
        }
    }

    private fun cleanName(name: String): String {
        return Html.fromHtml(name, Html.FROM_HTML_MODE_LEGACY).toString().trim()
    }
}
