package com.appdony.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AccountSheet
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.LoginSheet
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
        initialValue = com.appdony.app.data.preferences.ThemeState()
    )

    AppdonyTheme(
        darkTheme = themeState.mode == ThemeMode.DARK,
        primaryColor = ThemeManager.themeColors[themeState.colorIndex],
        fontSize = themeState.fontSize
    ) {
        MainScreen()
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val showChrome = currentRoute != "search" &&
            currentRoute != "settings" &&
            !currentRoute.startsWith("detail/")

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")
    val username by sessionManager.username.collectAsStateWithLifecycle(initialValue = "")

    var showLoginSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    if (showLoginSheet) {
        LoginSheet(
            onDismiss = { showLoginSheet = false },
            onLoginSuccess = { user, disp ->
                scope.launch {
                    sessionManager.saveSession(
                        username = user,
                        displayName = disp.ifBlank { user }
                    )
                    showLoginSheet = false
                    snackbarHostState.showSnackbar("خوش آمدید $disp")
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
            onFavorites = {
                scope.launch {
                    snackbarHostState.showSnackbar("علاقه‌مندی‌ها به‌زودی...")
                }
            },
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

    Scaffold(
        topBar = {
            if (showChrome) {
                AppTopBar(
                    isLoggedIn = isLoggedIn,
                    userInitial = displayName,
                    onSearchClick = { navController.navigate("search") },
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
                .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
        ) {
            AppNavGraph(navController = navController)
        }
    }
}
