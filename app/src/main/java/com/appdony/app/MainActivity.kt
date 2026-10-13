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
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppdonyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""
    val showChrome = currentRoute != "search" && !currentRoute.startsWith("detail/")

    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")

    var showLoginSheet by remember { mutableStateOf(false) }

    if (showLoginSheet) {
        LoginSheet(
            onDismiss = { showLoginSheet = false },
            onLoginSuccess = { username, disp ->
                scope.launch {
                    sessionManager.saveSession(
                        username = username,
                        displayName = disp.ifBlank { username }
                    )
                    showLoginSheet = false
                    snackbarHostState.showSnackbar("خوش آمدید $disp")
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
                        if (isLoggedIn) {
                            scope.launch {
                                snackbarHostState.showSnackbar("قبلاً وارد شدی: $displayName")
                            }
                        } else {
                            showLoginSheet = true
                        }
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
                .background(AppdonyBg)
        ) {
            AppNavGraph(navController = navController)
        }
    }
}
