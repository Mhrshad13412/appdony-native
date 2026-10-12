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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.theme.AppdonyBg
import com.appdony.app.ui.theme.AppdonyTheme

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

    Scaffold(
        topBar = {
            AppTopBar(
                onSearchClick = { /* TODO: صفحه جستجو */ },
                onLoginClick = { /* TODO: لاگین وردپرس */ }
            )
        },
        bottomBar = {
            AppBottomNav(navController = navController)
        }
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
