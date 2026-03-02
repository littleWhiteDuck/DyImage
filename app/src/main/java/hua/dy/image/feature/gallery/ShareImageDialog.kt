package hua.dy.image.feature.gallery

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.GifDecoder
import coil3.request.ImageRequest
import hua.dy.image.utils.FileType
import hua.dy.image.utils.X2GifUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareImageDialog(
    imagePath: String,
    fileType: FileType? = null,
    onDismiss: () -> Unit,
    onShare: (path: String) -> Unit
) {
    var currentPath by remember(imagePath) { mutableStateOf(imagePath) }
    var isWorking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val sourceFile = remember(currentPath) { File(currentPath) }
    val parentPath = sourceFile.parent ?: currentPath
    val fileLabel = sourceFile.name.ifBlank { "未命名文件" }
    val fileSizeLabel = remember(sourceFile.length()) { formatSize(sourceFile.length()) }

    val isConvertible =
        (currentPath.endsWith(".webp", true) || currentPath.endsWith(".heic", true)) &&
            !currentPath.endsWith(".gif", true)
    val shouldSkipPreview = fileType == FileType.UNKNOWN || fileType == FileType.VVIC

    val imageLoader = remember {
        ImageLoader.Builder(appCtx)
            .components { add(GifDecoder.Factory()) }
            .build()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "预览与分享",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(enabled = !isWorking, onClick = onDismiss) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "关闭")
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = fileLabel,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "类型：${fileType?.displayName ?: "unknown"}    大小：$fileSizeLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = parentPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (shouldSkipPreview) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageNotSupported,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (fileType == FileType.VVIC) {
                                "VVIC 暂不支持解码预览"
                            } else {
                                "该文件类型暂不提供预览"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    AsyncImage(
                        model = ImageRequest.Builder(appCtx)
                            .data(currentPath)
                            .build(),
                        imageLoader = imageLoader,
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.Fit,
                        contentDescription = null
                    )
                }
            }

            if (isWorking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    Text("处理中，请稍候…")
                }
            }

            statusMessage?.let {
                Text(
                    text = it,
                    color = if (statusIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    enabled = !isWorking,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(parentPath))
                        statusMessage = "已复制文件所在路径"
                        statusIsError = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("复制路径")
                }
                OutlinedButton(
                    enabled = !isWorking,
                    onClick = {
                        scope.launch {
                            isWorking = true
                            val success = withContext(Dispatchers.IO) {
                                saveImageToLocal(currentPath)
                            }
                            if (success) {
                                statusMessage = "已保存到系统相册"
                                statusIsError = false
                            } else {
                                statusMessage = "保存失败，请稍后重试"
                                statusIsError = true
                            }
                            isWorking = false
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("保存到本地")
                }
            }

            if (isConvertible) {
                FilledTonalButton(
                    enabled = !isWorking,
                    onClick = {
                        scope.launch {
                            isWorking = true
                            statusMessage = null
                            val target = File(appCtx.externalCacheDir, "image_share/share.gif")
                            target.parentFile?.mkdirs()
                            val success = withContext(Dispatchers.IO) {
                                X2GifUtils.convert(currentPath, target.absolutePath)
                            }
                            if (success) {
                                currentPath = target.absolutePath
                                statusMessage = "已转换为 GIF，可直接分享"
                                statusIsError = false
                            } else {
                                statusMessage = "转换失败，请直接分享原图"
                                statusIsError = true
                            }
                            isWorking = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("转换为 GIF")
                }
            }

                Button(
                    enabled = !isWorking,
                    onClick = {
                        scope.launch {
                            isWorking = true
                            val sharePath = withContext(Dispatchers.IO) {
                                resolveSharePath(currentPath)
                            }
                            isWorking = false
                            onShare(sharePath)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("分享给其他应用")
            }
        }
    }
}

private fun saveImageToLocal(path: String): Boolean {
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

private fun resolveSharePath(path: String): String {
    val source = File(path)
    if (!source.exists()) return path
    if (!source.extension.equals("vvic", ignoreCase = true)) return path

    val targetDir = File(appCtx.externalCacheDir, "image_share/share_alias")
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

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}
