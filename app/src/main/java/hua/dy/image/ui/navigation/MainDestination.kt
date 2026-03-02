package hua.dy.image.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class MainDestination(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    Gallery(route = "gallery", title = "图库", icon = Icons.Outlined.PhotoLibrary),
    Paths(route = "paths", title = "路径", icon = Icons.Outlined.Tune),
    Settings(route = "settings", title = "设置", icon = Icons.Outlined.Settings)
}
