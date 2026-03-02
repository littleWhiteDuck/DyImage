package hua.dy.image.feature.gallery

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import hua.dy.image.utils.FileType
import hua.dy.image.utils.X2GifUtils
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun isGifConvertible(path: String, fileType: FileType? = null): Boolean {
    if (path.endsWith(".gif", ignoreCase = true)) return false
    if (path.endsWith(".webp", ignoreCase = true)) return true
    if (path.endsWith(".heic", ignoreCase = true)) return true
    return fileType == FileType.WEBP || fileType == FileType.HEIC
}

internal fun convertToGif(sourcePath: String, nameSeed: String): String? {
    if (!isGifConvertible(sourcePath)) return null
    val source = File(sourcePath)
    if (!source.exists()) return null

    val baseDir = appCtx.externalCacheDir ?: appCtx.cacheDir
    val outputDir = File(baseDir, "image_share/converted_gif")
    if (!outputDir.exists()) outputDir.mkdirs()

    val safeSeed = nameSeed.ifBlank { source.nameWithoutExtension.ifBlank { "image" } }
    val target = File(outputDir, "${safeSeed}_${System.currentTimeMillis()}.gif")
    val success = runCatching { X2GifUtils.convert(sourcePath, target.absolutePath) }.getOrDefault(false)
    if (!success) {
        if (target.exists()) target.delete()
        return null
    }
    return target.absolutePath
}

internal fun saveImageToLocal(path: String): Boolean {
    val source = File(path)
    if (!source.exists()) return false
    val rawExtension = source.extension.ifBlank { "png" }.lowercase(Locale.ROOT)
    val extension = if (rawExtension == "vvic") "heic" else rawExtension
    val mime = when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "heic" -> "image/heic"
        else -> "image/png"
    }
    val fileName = "EImage_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(Date())}.$extension"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/EImage")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }
    val resolver = appCtx.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    val copied = runCatching {
        resolver.openOutputStream(uri)?.use { out ->
            source.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: return false
    }.isSuccess
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    if (!copied) {
        resolver.delete(uri, null, null)
    }
    return copied
}

internal fun resolveSharePath(path: String): String {
    val source = File(path)
    if (!source.exists()) return path
    if (!source.extension.equals("vvic", ignoreCase = true)) return path

    val baseDir = appCtx.externalCacheDir ?: appCtx.cacheDir
    val targetDir = File(baseDir, "image_share/share_alias")
    if (!targetDir.exists()) targetDir.mkdirs()
    val target = File(targetDir, "${source.nameWithoutExtension}.heic")
    return runCatching {
        source.inputStream().use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        target.absolutePath
    }.getOrDefault(path)
}
