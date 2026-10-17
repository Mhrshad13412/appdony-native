package com.appdony.app

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.OnboardingManager
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AccountSheet
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.FloatingAssistant
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.SpotlightTutorial
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot()
        }
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val themeState by themeManager.state.collectAsStateWithLifecycle(
        initialValue = ThemeState()
    )

    val fontScale = when (themeState.fontSize) {
        FontSize.SMALL -> 0.85f
        FontSize.MEDIUM -> 1.0f
        FontSize.LARGE -> 1.2f
    }
    val density = LocalDensity.current

    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale)
    ) {
        AppdonyTheme(
            darkTheme = themeState.mode == ThemeMode.DARK,
            primaryColor = ThemeManager.themeColors[themeState.colorIndex]
        ) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val showChrome = currentRoute != "search" &&
            currentRoute != "settings" &&
            currentRoute != "favorites" &&
            !currentRoute.startsWith("detail/")

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val onboardingManager = remember { OnboardingManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")
    val username by sessionManager.username.collectAsStateWithLifecycle(initialValue = "")
    
    val hasSeenOnboarding by onboardingManager.hasSeenOnboarding.collectAsStateWithLifecycle(initialValue = false)

    var showLoginSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) }

    LaunchedEffect(hasSeenOnboarding) {
        if (!hasSeenOnboarding) {
            showTutorial = true
        }
    }

    // ⚠️ نکته مهم: همه چیز داخل یک Box با fillMaxSize قرار می‌گیرد
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (showChrome) {
                    AppTopBar(
                        isLoggedIn = isLoggedIn,
                        userInitial = displayName,
                        onSearchClick = { navController.navigate("search") },
                        onHelpClick = { showTutorial = true },
                        onLoginClick = {
                            if (isLoggedIn) showAccountSheet = true
                            else showLoginSheet = true
                        }
                    )
                }
            },
            bottomBar = {
                if (showChrome) {
                    AppBottomNav(navController = navController)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AppNavGraph(navController = navController)

                // شخصیت شناور (با padding کمتر)
                if (showChrome && !showTutorial) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 80.dp, end = 16.dp) // 👈 فاصله کمتر
                    ) {
                        FloatingAssistant(
                            onSearchClick = { navController.navigate("search") },
                            onDownloadsClick = { navController.navigate("updates") },
                            onHelpClick = { showTutorial = true }
                        )
                    }
                }
            }
        }

        // 👇 لایه راهنما دقیقاً روی Scaffold قرار می‌گیرد
        if (showTutorial) {
            SpotlightTutorial(
                onFinish = {
                    scope.launch { onboardingManager.setSeen() }
                    showTutorial = false
                }
            )
        }

        // شیت‌های لاگین و پروفایل هم باید داخل همین Box باشن
        if (showLoginSheet) {
            LoginSheet(
                onDismiss = { showLoginSheet = false },
                onLoginSuccess = { user ->
                    scope.launch {
                        sessionManager.saveSession(
                            userId = user.id,
                            username = user.username,
                            displayName = user.displayName(),
                            email = user.email,
                            avatarUrl = user.avatarUrl() ?: "",
                            nonce = user.nonce
                        )
                        showLoginSheet = false
                        snackbarHostState.showSnackbar("خوش آمدید ${user.displayName()} 👋")
                    }
                }
            )
        }

        if (showAccountSheet) {
            AccountSheet(
                displayName = displayName.ifBlank { username },
                username = username,
                onDismiss = { showAccountSheet = false },
                onDownloads = { navController.navigate("updates") },
                onSettings = { navController.navigate("settings") },
                onFavorites = { navController.navigate("favorites") },
                onHelp = { showTutorial = true },
                onAbout = {
                    scope.launch {
                        snackbarHostState.showSnackbar("اپدونی - نسخه 2.5.33")
                    }
                },
                onLogout = {
                    scope.launch {
                        sessionManager.clearSession()
                        showAccountSheet = false
                        snackbarHostState.showSnackbar("از حساب خارج شدی")
                    }
                }
            )
        }
    }
}
