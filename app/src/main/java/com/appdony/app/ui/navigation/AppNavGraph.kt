package com.appdony.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.appdony.app.ui.screens.BlogScreen
import com.appdony.app.ui.screens.GamesScreen
import com.appdony.app.ui.screens.HomeScreen
import com.appdony.app.ui.screens.UpdatesScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") { HomeScreen() }
        composable("games") { GamesScreen() }
        composable("blog") { BlogScreen() }
        composable("updates") { UpdatesScreen() }
    }
}
