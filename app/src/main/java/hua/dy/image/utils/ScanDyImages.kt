package hua.dy.image.utils

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import hua.dy.image.app.AppBean
import hua.dy.image.app.DyAppBean
import hua.dy.image.bean.ImageBean
import hua.dy.image.db.dyImageDao
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import splitties.init.appCtx
import java.io.BufferedInputStream
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

private val handlerException = CoroutineExceptionHandler { _, throwable ->
    Log.e("TAG", "异常 $throwable")
}

private val job = SupervisorJob()

private val scope = CoroutineScope(job + handlerException)

const val scopeCount = 4

@Volatile
private var scopeRunningCount = 0

fun scanDyImages(
    appBean: AppBean = DyAppBean
) {
    if (scopeRunningCount > 0) {
        scope.launch(Dispatchers.Main) {
            Toast.makeText(appCtx, "正在刷新", Toast.LENGTH_SHORT).show()
        }
    }
    val shared by SharedPreferenceEntrust(appBean.packageName, "")
    val documentDir = DocumentFile.fromTreeUri(appCtx, Uri.parse(shared)) ?: return
    if (!documentDir.exists() || !documentDir.isDirectory) return
    repeat(appBean.cachePath.size) { index ->
        val path = appBean.cachePath[index]
        val targetFile = documentDir.findDocument(path) ?: return
        if (targetFile.isDirectory && targetFile.listFiles().isEmpty()) return
        targetFile.saveFile(index, appBean)
    }
}

private fun DocumentFile.getRealScopeCount(): Pair<Int, Int> {
    val fileSum = listFiles().size
    val interval = fileSum.toFloat() / scopeCount
    val scopeCount = if (fileSize.toFloat() % scopeCount == 0f) {
        scopeCount
    } else {
        if (interval < 1 && interval > 0) 1 else if (interval <= 0) 0 else scopeCount + 1
    }
    return Pair(scopeCount, if (interval < 1) fileSum else interval.toInt())
}

private fun DocumentFile.saveFile(
    cacheIndex: Int,
    appBean: AppBean
) {
    val (realScopeCount, interval) = getRealScopeCount()
    repeat(realScopeCount) { index ->
        scope.launch(Dispatchers.IO) {
            scopeRunningCount++
            for (i in (index * interval) until ((index + 1) * interval)) {
                this@saveFile.listFiles()[i].saveImage(cacheIndex, appBean)
            }
        }.invokeOnCompletion {
            if (--scopeRunningCount == 0) {
                scope.launch(Dispatchers.Main) {
                    Toast.makeText(appCtx, "刷新完成", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

private suspend fun DocumentFile.saveImage(
    cacheIndex: Int,
    appBean: AppBean = DyAppBean
) {
    when {
        isDirectory -> {
            listFiles().forEach { document ->
                document.saveImage(cacheIndex)
            }
        }

        isFile -> {
            if (length() < fileSize) return
            val md5 = this.md5
            val count = dyImageDao.selectMd5Exist(md5)
            if (count > 0) return
            val endType = imageType
            val fileNameWithType =
                "${this.generalFileName()}.${endType.takeIf { it != FileType.UNKNOWN }?.displayName ?: FileType.PNG.displayName}"
            val newFile = FileProvider.getUriForFile(
                appCtx,
                SHARED_PROVIDER,
                File(appBean.saveImagePath, fileNameWithType)
            )
            appCtx.contentResolver.openOutputStream(newFile)?.use { fos ->
                appCtx.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.copyTo(fos)
                }
            }
            val imageBean = ImageBean(
                md5 = md5,
                imagePath = newFile.toString(),
                fileLength = this.length(),
                fileTime = this.lastModified(),
                fileType = endType,
                fileName = fileNameWithType,
                secondMenu = appBean.providerSecond,
                scanTime = System.currentTimeMillis(),
                cachePath = appBean.cachePath.getOrNull(cacheIndex) ?: appBean.cachePath.first()
            )
            dyImageDao.insert(imageBean)
        }
    }
}

private val DocumentFile.md5: String
    get() {
        val ins = appCtx.contentResolver.openInputStream(uri)
        val md5 = MessageDigest.getInstance("MD5")
        BufferedInputStream(ins, 1024).use {
            md5.update(it.readBytes())
        }
        return BigInteger(1, md5.digest()).toString(16).padStart(32, '0')
    }

val DocumentFile.imageType: FileType
    get() {
        val ins = appCtx.contentResolver.openInputStream(uri) ?: return FileType.UNKNOWN
        val byteArray = ByteArray(12) // webp 12
        ins.read(byteArray)
        ins.close()
        Log.e("FileType", byteArray.map { it.toInt().toChar() }.joinToString("."))
        return FileTypeChecker.getType(byteArray)
    }

/**
 * 以byte为单位
 */
val fileSize by SharedPreferenceEntrust("fileSize", 256)


fun DocumentFile.generalFileName(): String {
//    val subNameResult = runCatching {
//        this.name?.substring(0,5)
//    }
//    val subName = if (subNameResult.isFailure) {
//        this.name
//    } else subNameResult.getOrThrow()
//    return "${subName}_${simpleDateFormat.format(lastModified())}"
    return "${name?.replace(".cnt", "")}"
}

//private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.CHINA)
