package com.appdony.app.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appdony.app.ui.screens.AiChatScreen
import com.appdony.app.ui.screens.AppsScreen
import com.appdony.app.ui.screens.BlogScreen
import com.appdony.app.ui.screens.CategoryScreen
import com.appdony.app.ui.screens.DetailScreen
import com.appdony.app.ui.screens.FavoritesScreen
import com.appdony.app.ui.screens.GamesScreen
import com.appdony.app.ui.screens.SearchScreen
import com.appdony.app.ui.screens.SettingsScreen
import com.appdony.app.ui.screens.UpdatesScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "games",
        // 👇 انیمیشن پیش‌فرض برای جابه‌جایی بین تب‌ها (مثل بازی‌ها به برنامه‌ها)
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300))
        }
    ) {
        composable("games") {
            GamesScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name ->
                    navController.navigate("category/$id/${Uri.encode(name)}")
                }
            )
        }
        composable("apps") {
            AppsScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name ->
                    navController.navigate("category/$id/${Uri.encode(name)}")
                }
            )
        }
        composable("blog") {
            BlogScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name ->
                    navController.navigate("category/$id/${Uri.encode(name)}")
                }
            )
        }
        composable("updates") { UpdatesScreen() }

        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { id -> navController.navigate("detail/$id") }
            )
        }

        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable("favorites") {
            FavoritesScreen(
                onBack = { navController.popBackStack() },
                onPostClick = { id -> navController.navigate("detail/$id") }
            )
        }

        composable("chat") {
            AiChatScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = "category/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.IntType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStack ->
            val catId = backStack.arguments?.getInt("categoryId") ?: return@composable
            val catName = backStack.arguments?.getString("categoryName") ?: ""
            CategoryScreen(
                categoryId = catId,
                categoryName = Uri.decode(catName),
                onBack = { navController.popBackStack() },
                onPostClick = { id -> navController.navigate("detail/$id") }
            )
        }

        composable(
            route = "detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.IntType }),
            // 👇 انیمیشن اختصاصی برای صفحه جزئیات (ورود از راست، خروج به راست)
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it }, // از سمت راست صفحه وارد میشه
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(400))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(400))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it }, // موقع برگشت به سمت راست میره
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) { backStack ->
            val postId = backStack.arguments?.getInt("postId") ?: return@composable
            DetailScreen(
                postId = postId,
                onBack = { navController.popBackStack() },
                onSimilarClick = { id -> navController.navigate("detail/$id") }
            )
        }
    }
}
