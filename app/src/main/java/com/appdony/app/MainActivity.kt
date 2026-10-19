package com.appdony.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
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
import com.appdony.app.ui.components.*
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyTheme
import com.appdony.app.util.DownloadHelper
import com.appdony.app.util.UpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val fontScale = when (themeState.fontSize) { FontSize.SMALL -> 0.85f; FontSize.MEDIUM -> 1.0f; FontSize.LARGE -> 1.2f }
    val density = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        AppdonyTheme(darkTheme = themeState.mode == ThemeMode.DARK, primaryColor = ThemeManager.themeColors[themeState.colorIndex]) {
            MainScreen()
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val showChrome = currentRoute != "search" && currentRoute != "settings" && currentRoute != "favorites" && currentRoute != "chat" && !currentRoute.startsWith("detail/")

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

    // 👇 وضعیت‌های آپدیت و انیمیشن
    var updateInfo by remember { mutableStateOf<UpdateHelper.UpdateInfo?>(null) }
    var showUpdateBanner by remember { mutableStateOf(false) }
    var transitionTarget by remember { mutableStateOf<String?>(null) } // برای انیمیشن

    LaunchedEffect(Unit) {
        if (!hasSeenOnboarding) showTutorial = true
        
        val currentVersion = BuildConfig.VERSION_NAME
        val info = UpdateHelper.checkForUpdate()
        if (info != null && info.latestVersion != currentVersion) {
            updateInfo = info
            delay(2000)
            showUpdateBanner = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (showChrome) {
                    Column {
                        AppTopBar(isLoggedIn, displayName, { navController.navigate("search") }, { if (isLoggedIn) showAccountSheet = true else showLoginSheet = true })
                        UpdateBanner(
                            isVisible = showUpdateBanner,
                            versionName = updateInfo?.latestVersion ?: "",
                            onDownloadClick = {
                                showUpdateBanner = false
                                updateInfo?.let { info ->
                                    scope.launch(Dispatchers.Main) {
                                        DownloadHelper.startDownload(context, info.downloadUrl, "Appdony-Update-${info.latestVersion}.apk")
                                    }
                                }
                            },
                            onDismiss = { showUpdateBanner = false }
                        )
                    }
                }
            },
            bottomBar = {
                if (showChrome) {
                    AppBottomNav(
                        currentRoute = currentRoute,
                        onTabSelected = { route ->
                            // 👇 به جای ناوبری مستقیم، انیمیشن رو صدا می‌زنیم
                            transitionTarget = route
                        }
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                AppNavGraph(navController)
                
                if (showChrome && !showTutorial) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 90.dp, end = 16.dp)) {
                        FloatingAssistant(
                            onSearchClick = { navController.navigate("search") },
                            onDownloadsClick = { navController.navigate("updates") },
                            onHelpClick = { showTutorial = true },
                            onChatClick = { navController.navigate("chat") }
                        )
                    }
                }
            }
        }

        // 👇 نمایش لایه انیمیشن
        transitionTarget?.let { target ->
            CircularRevealAnimation(
                targetRoute = target,
                onAnimationEnd = {
                    navController.navigate(target) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    transitionTarget = null // پاک کردن انیمیشن
                }
            )
        }

        if (showTutorial) {
            SpotlightTutorial(onFinish = { scope.launch { onboardingManager.setSeen() }; showTutorial = false })
        }

        if (showLoginSheet) {
            LoginSheet(onDismiss = { showLoginSheet = false }, onLoginSuccess = { user ->
                scope.launch {
                    sessionManager.saveSession(user.id, user.username, user.displayName(), user.email, user.avatarUrl() ?: "", user.nonce)
                    showLoginSheet = false
                    snackbarHostState.showSnackbar("خوش آمدید ${user.displayName()} 👋")
                }
            })
        }

        if (showAccountSheet) {
            AccountSheet(
                displayName = displayName.ifBlank { username }, username = username,
                onDismiss = { showAccountSheet = false },
                onDownloads = { navController.navigate("updates") },
                onSettings = { navController.navigate("settings") },
                onFavorites = { navController.navigate("favorites") },
                onHelp = { showTutorial = true },
                onAbout = { scope.launch { snackbarHostState.showSnackbar("اپدونی - نسخه 2.5.33") } },
                onLogout = { scope.launch { sessionManager.clearSession(); showAccountSheet = false; snackbarHostState.showSnackbar("از حساب خارج شدی") } }
            )
        }
    }
}
