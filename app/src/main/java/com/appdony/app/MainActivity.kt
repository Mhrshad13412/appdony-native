package com.appdony.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.imageLoader
import coil.request.ImageRequest
import com.appdony.app.data.model.featuredImageUrl
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.OnboardingManager
import com.appdony.app.data.preferences.SettingsManager
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.repository.AppRepository
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AccountSheet
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.FloatingAssistant
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.SplashScreen
import com.appdony.app.ui.components.SpotlightTutorial
import com.appdony.app.ui.components.UpdateBanner
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyTheme
import com.appdony.app.util.BlogUpdateWorker
import com.appdony.app.util.DownloadHelper
import com.appdony.app.util.UpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppRoot(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun AppRoot(intent: Intent?) {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())

    val fontScale = when (themeState.fontSize) {
        FontSize.SMALL -> 0.85f
        FontSize.MEDIUM -> 1.0f
        FontSize.LARGE -> 1.2f
    }
    val density = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        AppdonyTheme(
            darkTheme = themeState.mode == ThemeMode.DARK,
            primaryColor = ThemeManager.themeColors[themeState.colorIndex]
        ) {
            MainScreen(incomingIntent = intent)
        }
    }
}

@Composable
fun MainScreen(incomingIntent: Intent?) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val showChrome = currentRoute != "search" && currentRoute != "settings" && currentRoute != "favorites" && currentRoute != "chat" && !currentRoute.startsWith("detail/")

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    val settingsManager = remember { SettingsManager(context) }
    val startPage by settingsManager.startPage.collectAsStateWithLifecycle(initialValue = "games")
    val showAiAssistant by settingsManager.showAiAssistant.collectAsStateWithLifecycle(initialValue = true)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val blogWorkRequest = PeriodicWorkRequestBuilder<BlogUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "BlogUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            blogWorkRequest
        )
    }

    LaunchedEffect(incomingIntent) {
        incomingIntent?.data?.let { uri ->
            val postId = uri.getQueryParameter("p")
            if (postId != null) navController.navigate("detail/$postId?isBlog=false")
        }
        incomingIntent?.getStringExtra("shortcut_route")?.let { route ->
            if (route == "search" || route == "updates" || route == "favorites") navController.navigate(route)
        }
    }

    LaunchedEffect(Unit) {
        val repo = AppRepository(context)
        val result = repo.loadInitial("games")
        result.onSuccess { data ->
            val imageUrls = listOfNotNull(
                data.banner?.featuredImageUrl(),
                data.singleFeature?.featuredImageUrl()
            ) + data.featured.mapNotNull { it.featuredImageUrl() }

            imageUrls.take(10).forEach { url ->
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .build()
                context.imageLoader.enqueue(request)
            }
        }
        delay(1500)
        isLoading = false
    }

    if (isLoading) {
        SplashScreen()
    } else {
        val sessionManager = remember { SessionManager(context) }
        val onboardingManager = remember { OnboardingManager(context) }
        val snackbarHostState = remember { SnackbarHostState() }

        val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
        val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")
        val username by sessionManager.username.collectAsStateWithLifecycle(initialValue = "")
        val hasSeenOnboarding by onboardingManager.hasSeenOnboarding.collectAsStateWithLifecycle(initialValue = false)

        var showLoginSheet by remember { mutableStateOf(false) }
        var showAccountSheet by remember { mutableStateOf(false) }
        var showTutorial by remember { mutableStateOf(false) }
        var updateInfo by remember { mutableStateOf<UpdateHelper.UpdateInfo?>(null) }
        var showUpdateBanner by remember { mutableStateOf(false) }

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
                            AppTopBar(
                                isLoggedIn = isLoggedIn,
                                userInitial = displayName,
                                onSearchClick = { navController.navigate("search") },
                                onLoginClick = {
                                    if (isLoggedIn) showAccountSheet = true else showLoginSheet = true
                                }
                            )
                            UpdateBanner(
                                isVisible = showUpdateBanner,
                                versionName = updateInfo?.latestVersion ?: "",
                                onDownloadClick = {
                                    showUpdateBanner = false
                                    updateInfo?.let { info ->
                                        scope.launch(Dispatchers.Main) {
                                            DownloadHelper.startDownload(
                                                context,
                                                info.downloadUrl,
                                                "Appdony-Update-${info.latestVersion}.apk"
                                            )
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
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AppNavGraph(navController = navController, startDestination = startPage)
                    if (showChrome && !showTutorial && showAiAssistant) {
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

            if (showTutorial) {
                SpotlightTutorial(onFinish = {
                    scope.launch { onboardingManager.setSeen() }
                    showTutorial = false
                })
            }

            if (showLoginSheet) {
                LoginSheet(
                    onDismiss = { showLoginSheet = false },
                    onLoginSuccess = { user ->
                        scope.launch {
                            sessionManager.saveSession(user.id, user.username, user.name, user.email, user.avatarUrl() ?: "", user.nonce)
                            showLoginSheet = false
                            snackbarHostState.showSnackbar("خوش آمدید ${user.displayName()} ")
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
                    onAbout = { scope.launch { snackbarHostState.showSnackbar("اپدونی - نسخه 2.9.35") } }
                )
            }
        }
    }
}
