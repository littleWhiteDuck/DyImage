package hua.dy.image.service

import android.os.RemoteException
import hua.dy.image.bean.FileBean
import hua.dy.image.bean.ImageBean
import hua.dy.image.utils.FileType
import hua.dy.image.utils.FileTypeChecker
import java.io.BufferedInputStream
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.min

class FileExplorerService : IFileExplorerService.Stub() {
    @Throws(RemoteException::class)
    override fun listFiles(path: String?): List<FileBean> {
        if (path == null) return emptyList()
        val file = File(path)
        if (file.isFile) return emptyList()
        return file.listFiles()?.map { it.toFileBean() } ?: emptyList()
    }

    override fun getFileBean(path: String?): FileBean {
        if (path == null) throw NullPointerException("Path is Empty")
        return File(path).toFileBean()
    }

    override fun copyToMyFile(
        bean: FileBean?,
        fileSize: Long,
        cacheIndex: Int,
        providerSecond: String?,
        saveImagePath: String?,
        cachePath: List<String>?
    ): ImageBean {
        if (bean == null) throw NullPointerException("Bean is Empty")
        if ((bean.length ?: 0L) < fileSize) throw Exception("File is too small")
        val sourceFile = File(bean.path ?: throw NullPointerException("Bean path is Empty"))
        val (md5, endType) = sourceFile.inputStream().use { input ->
            input.fingerprint()
        }
        val fileNameWithType =
            "${md5}.${endType.takeIf { it != FileType.UNKNOWN }?.displayName ?: FileType.PNG.displayName}"
        val generalFilePath = File(saveImagePath, fileNameWithType)
        if (!generalFilePath.exists()) {
            generalFilePath.outputStream().use { fos ->
                sourceFile.inputStream().use { ins ->
                    ins.copyTo(fos)
                }
            }
        }
        return ImageBean(
            md5 = md5,
            imagePath = generalFilePath.toString(),
            fileLength = bean.length ?: 0L,
            fileTime = bean.lastModified ?: 0L,
            fileType = endType,
            fileName = fileNameWithType,
            secondMenu = providerSecond ?: "",
            scanTime = System.currentTimeMillis(),
            cachePath = cachePath?.getOrNull(cacheIndex) ?: cachePath?.first() ?: ""
        )
    }

    private fun File.toFileBean(): FileBean {
        return FileBean(
            name = name,
            path = path,
            length = length(),
            lastModified = lastModified(),
            isDirectory = isDirectory
        )
    }

    private fun InputStream.fingerprint(): Pair<String, FileType> {
        val md5 = MessageDigest.getInstance("MD5")
        val header = ByteArray(12)
        var headerOffset = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        val bis = BufferedInputStream(this, 8 * 1024)
        while (true) {
            val read = bis.read(buffer)
            if (read <= 0) break
            md5.update(buffer, 0, read)
            if (headerOffset < header.size) {
                val copySize = min(header.size - headerOffset, read)
                System.arraycopy(buffer, 0, header, headerOffset, copySize)
                headerOffset += copySize
            }
        }
        val md5Text = BigInteger(1, md5.digest()).toString(16).padStart(32, '0')
        val type = FileTypeChecker.getType(header.copyOf(headerOffset))
        return md5Text to type
    }

    fun FileBean.generalFileName(): String {
//    val subNameResult = runCatching {
//        this.name?.substring(0,5)
//    }
//    val subName = if (subNameResult.isFailure) {
//        this.name
//    } else subNameResult.getOrThrow()
//    return "${subName}_${simpleDateFormat.format(lastModified())}"
        return "${name?.replace(".cnt", "")}"
    }


    companion object {
        private const val TAG = "FileExplorerService"

        var service: IFileExplorerService? = null
    }
}
