package hua.dy.image

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.FileProvider
import hua.dy.image.data.settings.AppSettings
import hua.dy.image.data.settings.AppSettingsStore
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
            EImageTheme(
                themeMode = settings.themeMode,
                followSystemDynamicColor = settings.followSystemDynamicColor
            ) {
                EImageApp()
            }
        }
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
