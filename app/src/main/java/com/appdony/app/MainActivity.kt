// ... (بقیه import ها مثل قبل)
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.appdony.app.util.DownloadHelper
import com.appdony.app.util.UpdateHelper

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val themeManager = remember { ThemeManager(context) }
    val themeState by themeManager.state.collectAsStateWithLifecycle(initialValue = ThemeState())
    
    // 👇 سیستم چک کردن آپدیت
    var updateInfo by remember { mutableStateOf<UpdateHelper.UpdateInfo?>(null) }
    
    LaunchedEffect(Unit) {
        val currentVersion = com.appdony.app.BuildConfig.VERSION_NAME // مثلا 2.9.35
        val info = UpdateHelper.checkForUpdate()
        if (info != null && info.latestVersion != currentVersion) {
            updateInfo = info
        }
    }

    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("نسخه جدید در دسترس است! 🎉") },
            text = { 
                Column {
                    Text("نسخه ${updateInfo!!.latestVersion} منتشر شد.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(updateInfo!!.changelog, fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val context = context
                    // دانلود مستقیم فایل آپدیت
                    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                        DownloadHelper.startDownload(context, updateInfo!!.downloadUrl, "Appdony-Update-${updateInfo!!.latestVersion}.apk")
                    }
                    updateInfo = null
                }) {
                    Text("دانلود و نصب")
                }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) {
                    Text("بعداً")
                }
            }
        )
    }

    val fontScale = when (themeState.fontSize) { FontSize.SMALL -> 0.85f; FontSize.MEDIUM -> 1.0f; FontSize.LARGE -> 1.2f }
    val density = LocalDensity.current

    CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
        AppdonyTheme(darkTheme = themeState.mode == ThemeMode.DARK, primaryColor = ThemeManager.themeColors[themeState.colorIndex]) {
            MainScreen()
        }
    }
}
