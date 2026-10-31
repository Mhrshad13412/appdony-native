package com.appdony.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appdony.app.ui.screens.AiChatScreen
import com.appdony.app.ui.screens.SupportAdminScreen
import com.appdony.app.ui.screens.SupportChatScreen
import com.appdony.app.ui.screens.AppsScreen
import com.appdony.app.ui.screens.BlogScreen
import com.appdony.app.ui.screens.CategoryScreen
import com.appdony.app.ui.screens.DetailScreen
import com.appdony.app.ui.screens.DownloadsScreen
import com.appdony.app.ui.screens.FavoritesScreen
import com.appdony.app.ui.screens.GamesScreen
import com.appdony.app.ui.screens.SearchScreen
import com.appdony.app.ui.screens.SettingsScreen
import com.appdony.app.ui.screens.UpdatesScreen

// ───── انیمیشن تب‌ها: صفحه مثل یک نوار افقی به چپ/راست «غُل می‌خورد» ─────
// چیدمان RTL است: تب‌های با اندیس بیشتر سمت چپ‌اند؛ پس رفتن به آن‌ها یعنی ورود از چپ.
private const val TAB_ANIM_MS = 420
private val TabEasing = FastOutSlowInEasing

private fun tabIndexOf(route: String?): Int =
    bottomNavItems.indexOfFirst { it.route == route?.substringBefore('?') }

/** +1: مقصد سمت چپ است (ورود از چپ)؛ -1: مقصد سمت راست؛ 0: انتقال بین تب‌ها نیست */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabDirection(): Int {
    val from = tabIndexOf(initialState.destination.route)
    val to = tabIndexOf(targetState.destination.route)
    if (from < 0 || to < 0 || from == to) return 0
    return if (to > from) 1 else -1
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnter(): EnterTransition =
    when (tabDirection()) {
        0 -> fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
        else -> {
            val dir = tabDirection()
            slideInHorizontally(tween(TAB_ANIM_MS, easing = TabEasing)) { full -> -dir * full } +
                fadeIn(tween(TAB_ANIM_MS, easing = TabEasing)) +
                scaleIn(initialScale = 0.92f, animationSpec = tween(TAB_ANIM_MS, easing = TabEasing))
        }
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExit(): ExitTransition =
    when (tabDirection()) {
        0 -> fadeOut(tween(300))
        else -> {
            val dir = tabDirection()
            slideOutHorizontally(tween(TAB_ANIM_MS, easing = TabEasing)) { full -> dir * full } +
                fadeOut(tween(TAB_ANIM_MS / 2)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(TAB_ANIM_MS, easing = TabEasing))
        }
    }

private fun NavGraphBuilder.tabComposable(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route = route,
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabEnter() },
        popExitTransition = { tabExit() }
    ) { content(it) }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = "games"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) },
        popExitTransition = { fadeOut(tween(300)) }
    ) {
        tabComposable("games") {
            GamesScreen(
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") },
                onCategoryClick = { id, name -> navController.navigate("category/$id/${Uri.encode(name)}") }
            )
        }
        tabComposable("apps") {
            AppsScreen(
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") },
                onCategoryClick = { id, name -> navController.navigate("category/$id/${Uri.encode(name)}") }
            )
        }
        tabComposable("blog") {
            BlogScreen(
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=true") }, // 👈 اینجا true میشه
                onCategoryClick = { id, name -> navController.navigate("category/$id/${Uri.encode(name)}") }
            )
        }
        composable("updates") { UpdatesScreen() }
        // جستجو: نوار بالا همزمان به نوار جستجو تبدیل می‌شه؛ نتیجه‌ها کمی دیرتر و آروم از پایین میان
        composable(
            route = "search",
            enterTransition = {
                fadeIn(tween(320, delayMillis = 140)) +
                    slideInVertically(tween(380, delayMillis = 100, easing = FastOutSlowInEasing)) { it / 14 }
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(240)) },
            popExitTransition = {
                fadeOut(tween(180)) +
                    slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it / 14 }
            }
        ) {
            SearchScreen(onBack = { navController.popBackStack() }, onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") })
        }
        composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        composable("favorites") { FavoritesScreen(onBack = { navController.popBackStack() }, onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") }) }
        composable("chat") {
            AiChatScreen(
                onBack = { navController.popBackStack() },
                onSupportClick = { admin -> navController.navigate(if (admin) "support_admin" else "support") },
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") }
            )
        }
        composable("ai_chat") {
            AiChatScreen(
                onBack = { navController.popBackStack() },
                onSupportClick = { admin -> navController.navigate(if (admin) "support_admin" else "support") },
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") }
            )
        }

        // پشتیبانی: کاربر عادی مستقیم می‌ره چت، ادمین اول لیست کاربرها رو می‌بینه
        composable("support") {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val session = androidx.compose.runtime.remember { com.appdony.app.data.session.SessionManager(ctx) }
            val uid by session.userId.collectAsState(initial = 0)
            val dn by session.displayName.collectAsState(initial = "")
            val un by session.username.collectAsState(initial = "")
            if (uid > 0) {
                SupportChatScreen(
                    thread = com.appdony.app.data.api.SupportApi.threadOf(uid),
                    title = "پشتیبانی",
                    myName = dn.ifBlank { un }.ifBlank { "کاربر" },
                    asAdmin = false,
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable("support_admin") {
            SupportAdminScreen(
                onOpen = { t, n -> navController.navigate("support_chat/${Uri.encode(t)}/${Uri.encode(n)}") },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "support_chat/{thread}/{name}",
            arguments = listOf(navArgument("thread") { type = NavType.StringType }, navArgument("name") { type = NavType.StringType })
        ) { backStack ->
            SupportChatScreen(
                thread = Uri.decode(backStack.arguments?.getString("thread").orEmpty()),
                title = Uri.decode(backStack.arguments?.getString("name").orEmpty()),
                myName = "پشتیبان",
                asAdmin = true,
                onBack = { navController.popBackStack() }
            )
        }
        tabComposable("downloads") { DownloadsScreen() }

        composable(
            route = "category/{categoryId}/{categoryName}",
            arguments = listOf(navArgument("categoryId") { type = NavType.IntType }, navArgument("categoryName") { type = NavType.StringType })
        ) { backStack ->
            val catId = backStack.arguments?.getInt("categoryId") ?: return@composable
            val catName = backStack.arguments?.getString("categoryName") ?: ""
            CategoryScreen(
                categoryId = catId, categoryName = Uri.decode(catName), onBack = { navController.popBackStack() },
                onPostClick = { id -> navController.navigate("detail/$id?isBlog=false") }
            )
        }

        // 👇 اضافه شدن isBlog به مسیر
        composable(
            route = "detail/{postId}?isBlog={isBlog}",
            arguments = listOf(
                navArgument("postId") { type = NavType.IntType },
                navArgument("isBlog") { type = NavType.BoolType; defaultValue = false }
            ),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeIn(tween(400)) },
            exitTransition = { fadeOut(tween(400)) },
            popEnterTransition = { fadeIn(tween(400)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(400, easing = FastOutSlowInEasing)) + fadeOut(tween(400)) }
        ) { backStack ->
            val postId = backStack.arguments?.getInt("postId") ?: return@composable
            val isBlog = backStack.arguments?.getBoolean("isBlog") ?: false
            DetailScreen(postId = postId, isBlog = isBlog, onBack = { navController.popBackStack() }, onSimilarClick = { id -> navController.navigate("detail/$id?isBlog=false") })
        }
    }
}
