package hua.dy.image

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import hua.dy.image.data.settings.AppSettings
import hua.dy.image.data.settings.AppSettingsStore
import hua.dy.image.data.settings.ThemeMode
import hua.dy.image.ui.EImageApp
import hua.dy.image.ui.theme.EImageTheme
import hua.dy.image.utils.SHARED_PROVIDER
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by AppSettingsStore.settingsFlow.collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
                ThemeMode.System -> isSystemInDarkTheme()
            }
            val activity = LocalContext.current as? Activity
            activity?.window?.let { window ->
                UpdateStatusBar(window = window, darkIcons = !darkTheme)
            }
            EImageTheme(
                themeMode = settings.themeMode,
                followSystemDynamicColor = settings.followSystemDynamicColor
            ) {
                EImageApp()
            }
        }
    }
}

@Composable
fun UpdateStatusBar(window: Window, darkIcons: Boolean) {
    LaunchedEffect(window, darkIcons) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = darkIcons
    }
}

fun Context.shareOtherApp(imagePath: String) {
    val imageFile = File(imagePath)
    if (!imageFile.exists()) return
    val uri = FileProvider.getUriForFile(this, SHARED_PROVIDER, imageFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    startActivity(Intent.createChooser(intent, "分享图片"))
}
