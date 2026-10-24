package com.appdony.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appdony.app.data.model.UserData
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.OnboardingManager
import com.appdony.app.data.preferences.SettingsManager
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AccountSheet
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.FloatingAssistant
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.SplashScreen
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyTheme
import com.appdony.app.util.BlogUpdateWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AppRoot() }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    val onboardingManager = remember { OnboardingManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val showAiAssistant by settingsManager.showAiAssistant.collectAsStateWithLifecycle(initialValue = true)
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val username by sessionManager.username.collectAsStateWithLifecycle(initialValue = "")
    val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")

    var showSplash by remember { mutableStateOf(true) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    LaunchedEffect(Unit) {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val workRequest = PeriodicWorkRequestBuilder<BlogUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("BlogUpdateWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }

    val fontScale = when (themeState.fontSize) {
        FontSize.SMALL -> 0.85f
        FontSize.MEDIUM -> 1.0f
        FontSize.LARGE -> 1.15f
    }
    val density = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        AppdonyTheme(themeState = themeState) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                if (showSplash) {
                    SplashScreen()
                } else {
                    MainContent(
                        isLoggedIn = isLoggedIn,
                        username = username,
                        displayName = displayName,
                        showAiAssistant = showAiAssistant,
                        onLoginClick = { showLoginSheet = true },
                        onAccountClick = { showAccountSheet = true }
                    )
                }

                if (showLoginSheet) {
                    LoginSheet(onDismiss = { showLoginSheet = false }, onLoginSuccess = { user: UserData -> showLoginSheet = false })
                }

                if (showAccountSheet) {
                    AccountSheet(
                        displayName = if (displayName.isNotBlank()) displayName else username,
                        username = username,
                        onDismiss = { showAccountSheet = false },
                        onDownloads = { showAccountSheet = false },
                        onSettings = { showAccountSheet = false },
                        onFavorites = { showAccountSheet = false },
                        onHelp = { showAccountSheet = false },
                        onAbout = { showAccountSheet = false },
                        onLogout = { scope.launch { sessionManager.clearSession() }; showAccountSheet = false }
                    )
                }
            }
        }
    }
}

@Composable
fun MainContent(
    isLoggedIn: Boolean,
    username: String,
    displayName: String,
    showAiAssistant: Boolean,
    onLoginClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isMainRoute = currentRoute in listOf("games", "apps", "blog", "downloads")

    Scaffold(
        topBar = {
            if (currentRoute == "games" || currentRoute == "apps") {
                AppTopBar(
                    isLoggedIn = isLoggedIn,
                    userInitial = if (displayName.isNotBlank()) displayName else username,
                    onSearchClick = { navController.navigate("search") },
                    onLoginClick = { if (isLoggedIn) onAccountClick() else onLoginClick() }
                )
            }
        },
        bottomBar = {
            if (isMainRoute) {
                AppBottomNav(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AppNavGraph(navController = navController)
            if (showAiAssistant && isMainRoute) {
                Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)) {
                    FloatingAssistant(
                        onSearchClick = { navController.navigate("search") },
                        onDownloadsClick = { navController.navigate("downloads") },
                        onHelpClick = { navController.navigate("blog") },
                        onChatClick = { navController.navigate("ai_chat") }
                    )
                }
            }
        }
    }
}
