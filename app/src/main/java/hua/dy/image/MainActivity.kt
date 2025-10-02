package hua.dy.image

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.FileProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hua.dy.image.ui.Home
import hua.dy.image.ui.theme.DyImageTheme
import hua.dy.image.utils.SHARED_PROVIDER
import splitties.init.appCtx
import java.io.File

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DyImageTheme {
                val navController = rememberNavController()
                CompositionLocalProvider(
                    LocalNavController provides navController
                ) {
                    NavHost(navController = navController, startDestination = "home") {
                        route()
                    }
                }
            }
        }
    }

}

fun NavGraphBuilder.route() {
    composable("home") {
        Home()
    }
}

fun Context.shareOtherApp(
    imagePath: String
) {
    val imageFile = File(imagePath)
    val uri = FileProvider.getUriForFile(
        appCtx, SHARED_PROVIDER, imageFile
    )
    val intent = Intent(Intent.ACTION_SEND)
    intent.type = "image/*"
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.putExtra(Intent.EXTRA_STREAM, uri)
    startActivity(Intent.createChooser(intent, "分享表情"))
}