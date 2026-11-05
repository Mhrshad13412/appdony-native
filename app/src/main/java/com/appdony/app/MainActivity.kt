package com.appdony.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appdony.app.data.api.UserAccess
import com.appdony.app.data.model.UserData
import com.appdony.app.data.preferences.FontSize
import com.appdony.app.data.preferences.SettingsManager
import com.appdony.app.data.preferences.ThemeManager
import com.appdony.app.data.preferences.ThemeMode
import com.appdony.app.data.preferences.ThemeState
import com.appdony.app.data.session.SessionManager
import com.appdony.app.ui.components.AboutDialog
import com.appdony.app.ui.components.AccountSheet
import com.appdony.app.ui.components.AppBottomNav
import com.appdony.app.ui.components.AppTopBar
import com.appdony.app.ui.components.FloatingAssistant
import com.appdony.app.ui.components.SupportFloating
import com.appdony.app.ui.components.SupportPhase
import com.appdony.app.ui.components.SupportPresence
import com.appdony.app.ui.components.LocalBottomBarClearance
import com.appdony.app.ui.components.LoginSheet
import com.appdony.app.ui.components.SplashScreen
import com.appdony.app.ui.navigation.AppNavGraph
import com.appdony.app.ui.screens.rememberSearchViewModel
import com.appdony.app.ui.theme.AppdonyTheme
import com.appdony.app.util.BlogUpdateWorker
import com.appdony.app.util.CrashReporter
import com.appdony.app.util.PendingRoute
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // اندروید ۱۳+: بدون این اجازه اعلان درصد دانلود نشون داده نمی‌شه
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { }
                .launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        enableEdgeToEdge()
        // مقصدی که پنجره‌ی نصب سریع یا پاپیِ بیرونی خواسته
        PendingRoute.set(intent?.getStringExtra("route"))
        setContent { AppRoot() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PendingRoute.set(intent.getStringExtra("route"))
    }
}

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val settingsManager = remember { SettingsManager(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    val showAiAssistant by settingsManager.showAiAssistant.collectAsStateWithLifecycle(initialValue = true)
    val fixedAi by settingsManager.fixedAi.collectAsStateWithLifecycle(initialValue = true)
    val uiStyle by settingsManager.uiStyle.collectAsStateWithLifecycle(initialValue = SettingsManager.STYLE_GLASS)
    val isLoggedIn by sessionManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val username by sessionManager.username.collectAsStateWithLifecycle(initialValue = "")
    val displayName by sessionManager.displayName.collectAsStateWithLifecycle(initialValue = "")
    val canWritePost by sessionManager.canWritePost.collectAsStateWithLifecycle(initialValue = false)
    val canWriteBlog by sessionManager.canWriteBlog.collectAsStateWithLifecycle(initialValue = false)

    var showSplash by remember { mutableStateOf(true) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    var crashReport by remember { mutableStateOf(CrashReporter.read(context)) }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    // رفتن به مقصدی که از پنجره‌ی نصب سریع یا پاپیِ بیرونی اومده
    val pendingRoute by PendingRoute.route.collectAsStateWithLifecycle()
    LaunchedEffect(pendingRoute, showSplash) {
        val r = pendingRoute ?: return@LaunchedEffect
        if (showSplash) return@LaunchedEffect
        delay(150)
        runCatching {
            if (r == "downloads") navController.navigateToTab(r)
            else navController.navigate(r) { launchSingleTop = true }
        }
        PendingRoute.set(null)
    }

    // کاربرهایی که قبل از این آپدیت وارد شده بودن هم توی لیست پشتیبان ثبت بشن
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val id = sessionManager.userId.first()
            val name = sessionManager.displayName.first().ifBlank { sessionManager.username.first() }
            if (id > 0) com.appdony.app.data.api.SupportApi.registerUser(
                com.appdony.app.data.api.SupportApi.threadOf(id), name
            )
            // سطح دسترسی نوشتن (ایجاد نوشته / ایجاد بلاگ) رو از وردپرس تازه می‌کنه
            UserAccess.refresh(context, sessionManager)
        }
    }

    LaunchedEffect(Unit) {
        try {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val workRequest = PeriodicWorkRequestBuilder<BlogUpdateWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("BlogUpdateWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val fontScale = when (themeState.fontSize) {
        FontSize.SMALL -> 0.85f
        FontSize.MEDIUM -> 1.0f
        FontSize.LARGE -> 1.15f
    }
    val density = LocalDensity.current

    val isDark = themeState.mode == ThemeMode.DARK
    val primaryColor = ThemeManager.themeColors.getOrElse(themeState.colorIndex) { ThemeManager.themeColors[0] }

    DisposableEffect(isDark) {
        val activity = context as? ComponentActivity
        val transparent = android.graphics.Color.TRANSPARENT
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isDark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent),
            navigationBarStyle = if (isDark) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        )
        onDispose { }
    }

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        AppdonyTheme(darkTheme = isDark, primaryColor = primaryColor) {
          CompositionLocalProvider(
            com.appdony.app.ui.components.LocalGlassMode provides (uiStyle == SettingsManager.STYLE_GLASS)
          ) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (showSplash) {
                        SplashScreen()
                    } else {
                        MainContent(
                            navController = navController,
                            isLoggedIn = isLoggedIn,
                            username = username,
                            displayName = displayName,
                            showAiAssistant = showAiAssistant,
                            fixedAi = fixedAi,
                            onLoginClick = { showLoginSheet = true },
                            onAccountClick = { showAccountSheet = true }
                        )
                    }
                }

                if (showLoginSheet) {
                    LoginSheet(onDismiss = { showLoginSheet = false }, onLoginSuccess = { user: UserData -> showLoginSheet = false })
                }

                if (showAccountSheet) {
                    AccountSheet(
                        isLoggedIn = isLoggedIn,
                        displayName = if (displayName.isNotBlank()) displayName else username,
                        username = username,
                        onDismiss = { showAccountSheet = false },
                        onDownloads = { navController.navigateToTab("downloads") },
                        onSettings = { navController.navigate("settings") { launchSingleTop = true } },
                        onFavorites = { navController.navigate("favorites") { launchSingleTop = true } },
                        onAbout = { showAbout = true },
                        onLogin = { showLoginSheet = true },
                        onLogout = { scope.launch { sessionManager.clearSession() } },
                        canWritePost = canWritePost,
                        canWriteBlog = canWriteBlog,
                        onCreatePost = { navController.navigate("editor/post") { launchSingleTop = true } },
                        onCreateBlog = { navController.navigate("editor/blog") { launchSingleTop = true } }
                    )
                }

                if (showAbout) {
                    val versionName = remember {
                        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
                    }
                    AboutDialog(versionName = versionName, onDismiss = { showAbout = false })
                }

                val report = crashReport
                if (report != null) {
                    val clipboard = LocalClipboardManager.current
                    AlertDialog(
                        onDismissRequest = {
                            CrashReporter.clear(context)
                            crashReport = null
                        },
                        title = { Text("گزارش کرش قبلی") },
                        text = {
                            Text(
                                text = report.take(4000),
                                fontSize = 10.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = { clipboard.setText(AnnotatedString(report)) }) {
                                Text("کپی")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                CrashReporter.clear(context)
                                crashReport = null
                            }) {
                                Text("بستن")
                            }
                        }
                    )
                }
            }
          }
        }
    }
}

@Composable
fun MainContent(
    navController: NavHostController,
    isLoggedIn: Boolean,
    username: String,
    displayName: String,
    showAiAssistant: Boolean,
    fixedAi: Boolean,
    onLoginClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "games"
    val isMainRoute = currentRoute in listOf("games", "apps", "blog", "downloads")
    val isSearchRoute = currentRoute == "search"
    // صفحه‌ی جزئیات هم مثل صفحه‌های اصلی نوار بالا (لوگو، جستجو، ورود) و منوی پایین رو داره
    val isDetailRoute = currentRoute.startsWith("detail/")
    val showBottomNav = isMainRoute || isDetailRoute
    // توی جزئیات، تبی که ازش اومدیم توی منو روشن می‌مونه
    var lastMainRoute by remember { mutableStateOf("games") }
    LaunchedEffect(currentRoute) { if (isMainRoute) lastMainRoute = currentRoute }
    val navHighlightRoute = if (isDetailRoute) lastMainRoute else currentRoute

    // نوار بالا خودش فیلد جستجوئه؛ پس متن جستجو اینجا هم لازمه
    val searchViewModel = rememberSearchViewModel()
    val searchQuery by searchViewModel.query.collectAsStateWithLifecycle()
    val openSearch: () -> Unit = {
        if (currentRoute != "search") {
            searchViewModel.onQueryChange("")
            navController.navigate("search") { launchSingleTop = true }
        }
    }

    // منبع بلور: محتوای صفحه؛ منوی پایین از همین نمونه‌برداری می‌کنه و بلورش می‌کنه
    val hazeState = remember { HazeState() }
    val layoutDirection = LocalLayoutDirection.current
    val systemNavInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // ارتفاع منو (۸۸ دپ) + نوار سیستم؛ لیست‌ها این‌قدر ته صفحه جا می‌ذارن
    val bottomClearance = if (showBottomNav) 88.dp + systemNavInset else 0.dp

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (isMainRoute || isSearchRoute || isDetailRoute) {
                    AppTopBar(
                        isLoggedIn = isLoggedIn,
                        userInitial = if (displayName.isNotBlank()) displayName else username,
                        onSearchClick = openSearch,
                        // مهمان هم منو رو می‌بینه (ورود از داخل منوئه)
                        onLoginClick = { onAccountClick() },
                        searchMode = isSearchRoute,
                        query = searchQuery,
                        onQueryChange = searchViewModel::onQueryChange,
                        onSearchBack = { navController.popBackStack() }
                    )
                }
            }
        ) { padding ->
            // صفحه‌های اصلی: محتوا تا ته صفحه ادامه داره و از زیر منوی شیشه‌ای رد می‌شه
            val contentPadding = if (showBottomNav) {
                PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection),
                    bottom = 0.dp
                )
            } else {
                padding
            }
            Box(modifier = Modifier.fillMaxSize().padding(contentPadding).consumeWindowInsets(padding)) {
                CompositionLocalProvider(LocalBottomBarClearance provides bottomClearance) {
                    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                        AppNavGraph(navController = navController)
                    }
                }

                com.appdony.app.ui.components.AppBannerHost(modifier = Modifier.align(Alignment.TopCenter))
            }
        }

        // منوی پایین: شناور روی محتوا، موقع رفتن به جستجو آروم می‌ره پایین و موقع برگشتن میاد بالا
        AnimatedVisibility(
            visible = showBottomNav,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(380)),
            exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200))
        ) {
            AppBottomNav(
                currentRoute = navHighlightRoute,
                hazeState = hazeState,
                onTabSelected = { route ->
                    when {
                        // روی تبِ همین صفحه‌ی اصلی نیازی به رفتن نیست
                        isMainRoute && route == currentRoute -> Unit
                        // توی جزئیات، زدن تبِ روشن از جزئیات خارج می‌شه و برمی‌گرده به همون لیست (با همون جای اسکرول)
                        isDetailRoute && route == lastMainRoute ->
                            if (!navController.popBackStack(route, false)) navController.navigateToTab(route)
                        else -> navController.navigateToTab(route)
                    }
                },
                // هوش مصنوعی ثابت: دکمه‌ی گرد وسط منو (شیشه‌ای و کلاسیک) چت هوش مصنوعی رو باز می‌کنه
                fixedAi = fixedAi,
                onAiClick = { navController.navigate("ai_chat") { launchSingleTop = true } },
                // پشتیبان توی حالت ثابت جای آیکن وسط منو می‌شینه و با زدنش چت پشتیبانی باز می‌شه
                onSupportClick = { navController.navigate("support") { launchSingleTop = true } }
            )
        }

        // دکتر طاعون روی کل اپ (همه‌ی صفحه‌ها)، جز صفحه‌ی خود چت
        LaunchedEffect(showAiAssistant) {
            if (!showAiAssistant) SupportPresence.reset()
        }
        if (showAiAssistant) {
            val assistantVisible = currentRoute != "ai_chat" && currentRoute != "chat" &&
                !currentRoute.startsWith("support") && !currentRoute.startsWith("editor")
            // توی حالت «هوش مصنوعی ثابت» دستیار شناور اصلاً ساخته نمی‌شه؛ ربات و پشتیبان فقط توی دکمه‌ی
            // وسط منو هستن. قبلاً اینجا هم یه آواتار پشتیبان شناور کنار دکمه‌ی وسط ظاهر می‌شد (دوتایی).
            if (!fixedAi) {
                FloatingAssistant(
                    currentRoute = currentRoute,
                    // وقتی پشتیبان جای آیکن نشسته، آیکن قایمه
                    visible = assistantVisible && SupportPresence.phase == SupportPhase.OFF,
                    bottomBarHeight = if (showBottomNav) 88.dp else 0.dp,
                    onSearchClick = openSearch,
                    onDownloadsClick = { navController.navigateToTab("downloads") },
                    onHelpClick = { navController.navigateToTab("blog") },
                    onChatClick = { navController.navigate("ai_chat") { launchSingleTop = true } }
                )
            }
            // توی حالت ثابت، پشتیبان جای آیکن وسط منو می‌شینه و دیگه شناور نمی‌شه
            SupportFloating(
                visible = assistantVisible && !fixedAi,
                onOpenSupport = { navController.navigate("support") { launchSingleTop = true } }
            )
        }
    }
}

fun NavController.navigateToTab(route: String) {
    // منطق ساده و بدون ذخیره/بازیابی state: پشته همیشه «بازی‌ها» + حداکثر یک تب دیگه‌ست.
    // قبلاً saveState/restoreState بعد از خروج از جزئیات باعث می‌شد تب بازی‌ها قفل بشه و برنگرده.
    val start = graph.findStartDestination()
    if (route == start.route) {
        // برگشت به بازی‌ها: همه‌ی چیزای بالاش (تب دیگه، جزئیات) پاک می‌شن
        if (!popBackStack(route, inclusive = false)) {
            navigate(route) {
                popUpTo(start.id) { inclusive = false }
                launchSingleTop = true
            }
        }
        return
    }
    navigate(route) {
        popUpTo(start.id) { saveState = false }
        launchSingleTop = true
        restoreState = false
    }
}
