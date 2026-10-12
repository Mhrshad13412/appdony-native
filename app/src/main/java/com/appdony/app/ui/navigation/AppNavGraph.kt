package com.appdony.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import com.appdony.app.ui.screens.AppsScreen
import com.appdony.app.ui.screens.BlogScreen
import com.appdony.app.ui.screens.DetailScreen
import com.appdony.app.ui.screens.GamesScreen
import com.appdony.app.ui.screens.UpdatesScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "games"
    ) {
        composable("games") {
            GamesScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name -> /* TODO */ }
            )
        }
        composable("apps") {
            AppsScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name -> /* TODO */ }
            )
        }
        composable("blog") {
            BlogScreen(
                onPostClick = { id -> navController.navigate("detail/$id") },
                onCategoryClick = { id, name -> /* TODO */ }
            )
        }
        composable("updates") { UpdatesScreen() }

        composable(
            route = "detail/{postId}",
            arguments = listOf(navArgument("postId") { type = NavType.IntType })
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
