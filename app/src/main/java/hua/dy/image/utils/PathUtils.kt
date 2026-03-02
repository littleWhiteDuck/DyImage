@file:Suppress("SpellCheckingInspection")

package hua.dy.image.utils

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.documentfile.provider.DocumentFile
import hua.dy.image.bean.FileBean
import rikka.shizuku.Shizuku
import splitties.init.appCtx

const val ANDROID_SAF_PATH =
    "content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata%2F"

@Composable
fun GetDyPermission(
    needShizuku: Boolean,
    packageName: String,
    callBack: (isGranted: Boolean, isShizuku: Boolean) -> Unit
) {
    if (ShizukuUtils.isShizukuAvailable && needShizuku) {
        GetShizukuPermission(callBack)
    } else {
        GetSafPermission(packageName, callBack)
    }
}

@Composable
private fun GetShizukuPermission(
    callBack: (isGranted: Boolean, isShizuku: Boolean) -> Unit
) {
    val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode != 0) return@OnRequestPermissionResultListener
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            callBack.invoke(true, true)
            Toast.makeText(appCtx, "Shizuku 权限获取成功", Toast.LENGTH_SHORT).show()
        } else {
            callBack.invoke(false, true)
            Toast.makeText(appCtx, "Shizuku 权限获取失败", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    LaunchedEffect(Unit) {
        Shizuku.requestPermission(0)
    }
}

@Composable
private fun GetSafPermission(
    packageName: String,
    callBack: (isGranted: Boolean, isShizuku: Boolean) -> Unit
) {
    var packageShared by SharedPreferenceEntrust(packageName, "")
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) {
            Toast.makeText(appCtx, "未授予目录权限", Toast.LENGTH_SHORT).show()
            callBack.invoke(false, false)
            return@rememberLauncherForActivityResult
        }
        if (!uri.toString().endsWith(packageName)) {
            Toast.makeText(appCtx, "目录不正确，请选择目标应用目录", Toast.LENGTH_SHORT).show()
            callBack.invoke(false, false)
            return@rememberLauncherForActivityResult
        }

        val modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        appCtx.contentResolver.takePersistableUriPermission(uri, modeFlags)
        packageShared = uri.toString()
        callBack.invoke(true, false)
    }

    LaunchedEffect(Unit) {
        if (packageShared.isBlank() || !hasDyPermission(packageName)) {
            launcher.launch(Uri.parse("$ANDROID_SAF_PATH$packageName"))
        } else {
            callBack.invoke(true, false)
        }
    }
}

fun hasDyPermission(packageName: String): Boolean {
    val permissionUris = appCtx.contentResolver.persistedUriPermissions.map {
        it.uri.toString().split("data%2F", ignoreCase = true).last()
    }
    return permissionUris.indexOf(packageName) != -1
}

const val SHARED_PROVIDER = "hua.dy.image.provider2"
const val APP_SHARED_PROVIDER_TOP_PATH = "image_share"

private val pattern = "^[*]+$".toPattern()

fun DocumentFile.findDocument(cachePath: String): DocumentFile? {
    val pathList = cachePath.split("/").filter { it.isNotBlank() }
    var document: DocumentFile? = this
    pathList.forEach {
        if (document == null) return null
        val ma = pattern.matcher(it)
        if (ma.find()) {
            val index = ma.group().length - 1
            document = document.listFiles().getOrNull(index)
        } else {
            document.findFile(it)?.let { file ->
                document = file
            } ?: return null
        }
    }
    return document
}

fun FileBean.findDocument(cachePath: String): FileBean? {
    val pathList = cachePath.split("/").filter { it.isNotBlank() }
    var document: FileBean? = this
    pathList.forEach {
        if (document == null) return null
        val ma = pattern.matcher(it)
        if (ma.find()) {
            val index = ma.group().length - 1
            document = document.listFiles().getOrNull(index)
        } else {
            document.findFile(it)?.let { file ->
                document = file
            } ?: return null
        }
    }
    return document
}
