package hua.dy.image.data.repository

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.paging.PagingSource
import hua.dy.image.bean.FileBean
import hua.dy.image.bean.ImageBean
import hua.dy.image.db.ScanPathEntity
import hua.dy.image.db.ScanSchemeEntity
import hua.dy.image.db.dyImageDao
import hua.dy.image.db.scanPathDao
import hua.dy.image.db.scanSchemeDao
import hua.dy.image.service.FileExplorerService
import hua.dy.image.utils.APP_SHARED_PROVIDER_TOP_PATH
import hua.dy.image.utils.FileType
import hua.dy.image.utils.FileTypeChecker
import hua.dy.image.utils.SharedPreferenceEntrust
import hua.dy.image.utils.findDocument
import hua.dy.image.utils.hasDyPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import splitties.init.appCtx
import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import kotlin.math.min

data class ScanSummary(
    val useShizuku: Boolean,
    val scanned: Int,
    val added: Int,
    val duplicates: Int,
    val skippedSmallFiles: Int,
    val failed: Int
)

data class ClearAndRescanSummary(
    val schemeId: Long,
    val scanSummary: ScanSummary
)

data class ImagePathIntegritySummary(
    val sampled: Int,
    val missing: Int,
    val threshold: Int
) {
    val shouldRebuild: Boolean
        get() = sampled > 0 && missing >= threshold
}

object ImageRepository {

    private val defaultDouyinPaths = listOf(
        "/cache/picture/im_fresco_cache/*" to "聊天页面",
        "/cache/picture/fresco_cache/*" to "通用表情"
    )

    fun observeSchemes(): Flow<List<ScanSchemeEntity>> = scanSchemeDao.observeAll()

    suspend fun getSchemeById(id: Long): ScanSchemeEntity? = scanSchemeDao.getById(id)

    suspend fun ensureDefaultSchemeAndPaths() {
        if (scanSchemeDao.getCount() == 0) {
            scanSchemeDao.upsert(
                ScanSchemeEntity(
                    id = 1L,
                    name = "抖音",
                    packageName = "com.ss.android.ugc.aweme",
                    rootPath = "/sdcard/Android/data/com.ss.android.ugc.aweme",
                    saveFolder = "douyin"
                )
            )
        }
        val douyinScheme = scanSchemeDao.getById(1L) ?: return
        if (scanPathDao.getCountByScheme(douyinScheme.id) == 0) {
            defaultDouyinPaths.forEachIndexed { index, (path, note) ->
                scanPathDao.upsert(
                    ScanPathEntity(
                        schemeId = douyinScheme.id,
                        relativePath = path,
                        note = note,
                        isEnabled = true,
                        orderIndex = index
                    )
                )
            }
        }
    }

    fun observePathConfigs(schemeId: Long): Flow<List<ScanPathEntity>> {
        return scanPathDao.observeAllByScheme(schemeId)
    }

    suspend fun upsertScheme(entity: ScanSchemeEntity) {
        scanSchemeDao.upsert(entity)
    }

    suspend fun deleteScheme(entity: ScanSchemeEntity) {
        scanSchemeDao.delete(entity)
        scanPathDao.deleteAllByScheme(entity.id)
    }

    suspend fun restoreDefaultPathConfigs(schemeId: Long) {
        val scheme = scanSchemeDao.getById(schemeId) ?: return
        scanPathDao.deleteAllByScheme(scheme.id)
        if (scheme.packageName == "com.ss.android.ugc.aweme") {
            defaultDouyinPaths.forEachIndexed { index, (path, note) ->
                scanPathDao.upsert(
                    ScanPathEntity(
                        schemeId = scheme.id,
                        relativePath = path,
                        note = note,
                        isEnabled = true,
                        orderIndex = index
                    )
                )
            }
        }
    }

    suspend fun upsertPathConfig(config: ScanPathEntity) {
        scanPathDao.upsert(config)
    }

    suspend fun deletePathConfig(config: ScanPathEntity) {
        scanPathDao.delete(config)
    }

    suspend fun checkImagePathIntegrity(
        schemeId: Long,
        sampleSize: Int = 10,
        missingThreshold: Int = 2
    ): ImagePathIntegritySummary = withContext(Dispatchers.IO) {
        val resolvedSampleSize = sampleSize.coerceAtLeast(1)
        val resolvedThreshold = missingThreshold.coerceAtLeast(1)
        val sample = dyImageDao.getRandomImagePathsBySchemeTag(
            schemeTag = schemeTag(schemeId),
            limit = resolvedSampleSize
        )
        if (sample.isEmpty()) {
            return@withContext ImagePathIntegritySummary(
                sampled = 0,
                missing = 0,
                threshold = resolvedThreshold
            )
        }
        val missing = sample.count { path ->
            path.isBlank() || !File(path).exists()
        }
        ImagePathIntegritySummary(
            sampled = sample.size,
            missing = missing,
            threshold = resolvedThreshold
        )
    }

    suspend fun clearAllDatabaseAndRescan(
        preferredSchemeId: Long,
        minSizeKb: Int,
        preferShizuku: Boolean
    ): ClearAndRescanSummary = withContext(Dispatchers.IO) {
        // Keep user schemes/path configs/settings. Only clear scanned image index + copied cache files.
        clearScannedImageData()
        clearImageCacheFiles()
        ensureDefaultSchemeAndPaths()

        val scheme = scanSchemeDao.getById(preferredSchemeId) ?: scanSchemeDao.getFirst()
        if (scheme == null) {
            return@withContext ClearAndRescanSummary(
                schemeId = preferredSchemeId,
                scanSummary = ScanSummary(false, 0, 0, 0, 0, 0)
            )
        }

        val summary = scanConfiguredPaths(scheme, minSizeKb, preferShizuku)
        ClearAndRescanSummary(schemeId = scheme.id, scanSummary = summary)
    }

    fun getImagePagingSource(
        type: FileType,
        sortType: Int,
        schemeTag: String
    ): PagingSource<Int, ImageBean> {
        return when (sortType) {
            1 -> dyImageDao.getImageListByScanTime(type, schemeTag)
            2 -> dyImageDao.getImageListByFileLength(type, schemeTag)
            else -> dyImageDao.getImageListByFileTime(type, schemeTag)
        }
    }

    fun schemeTag(schemeId: Long): String = "scheme_$schemeId"

    suspend fun getSchemeImageCount(schemeId: Long): Int {
        return dyImageDao.getImageCountBySchemeTag(schemeTag(schemeId))
    }

    fun getPathFilterLabel(path: ScanPathEntity): String {
        val note = path.note.trim()
        if (note.isNotBlank()) return note
        return path.relativePath
            .trimEnd('/')
            .split('/')
            .lastOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: path.relativePath
    }

    fun isSafPermissionGranted(packageName: String): Boolean = hasDyPermission(packageName)

    suspend fun scanConfiguredPaths(
        scheme: ScanSchemeEntity,
        minFileSizeKb: Int,
        preferShizuku: Boolean
    ): ScanSummary = withContext(Dispatchers.IO) {
        val pathConfigs = scanPathDao.getEnabledByScheme(scheme.id)
        if (pathConfigs.isEmpty()) {
            return@withContext ScanSummary(false, 0, 0, 0, 0, 0)
        }

        val minSizeBytes = minFileSizeKb.toLong() * 1024L
        val useShizuku = preferShizuku &&
            hua.dy.image.utils.ShizukuUtils.isShizukuAvailable &&
            hua.dy.image.utils.ShizukuUtils.isShizukuPermission &&
            FileExplorerService.service != null

        if (useShizuku) {
            scanWithShizuku(scheme, pathConfigs, minSizeBytes)
        } else {
            scanWithSaf(scheme, pathConfigs, minSizeBytes)
        }
    }

    private fun resolveSaveDir(scheme: ScanSchemeEntity): File {
        val dir = File(appCtx.externalCacheDir, "$APP_SHARED_PROVIDER_TOP_PATH/${scheme.saveFolder}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private suspend fun clearScannedImageData() {
        dyImageDao.deleteAll()
    }

    private fun clearImageCacheFiles() {
        val externalCache = appCtx.externalCacheDir ?: return
        val imageShareDir = File(externalCache, APP_SHARED_PROVIDER_TOP_PATH)
        if (imageShareDir.exists()) {
            imageShareDir.deleteRecursively()
        }
    }

    private suspend fun scanWithSaf(
        scheme: ScanSchemeEntity,
        pathConfigs: List<ScanPathEntity>,
        minSizeBytes: Long
    ): ScanSummary {
        var permissionUri by SharedPreferenceEntrust(scheme.packageName, "")
        if (permissionUri.isBlank()) return ScanSummary(false, 0, 0, 0, 0, 0)
        val root = DocumentFile.fromTreeUri(appCtx, Uri.parse(permissionUri))
            ?: return ScanSummary(false, 0, 0, 0, 0, 0)

        val saveDir = resolveSaveDir(scheme)
        val schemeTag = schemeTag(scheme.id)
        var scanned = 0
        var added = 0
        var duplicates = 0
        var skippedSmallFiles = 0
        var failed = 0

        pathConfigs.forEach { pathConfig ->
            val target = root.findDocument(pathConfig.relativePath) ?: return@forEach
            val stack = ArrayDeque<DocumentFile>()
            stack.add(target)
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (node.isDirectory) {
                    node.listFiles().forEach(stack::addLast)
                    continue
                }
                if (!node.isFile) continue
                scanned += 1
                val length = node.length()
                if (length < minSizeBytes) {
                    skippedSmallFiles += 1
                    continue
                }
                val fingerprint = runCatching { node.fingerprint() }.getOrNull()
                if (fingerprint == null) {
                    failed += 1
                    continue
                }

                val extType = if (fingerprint.fileType == FileType.UNKNOWN) {
                    FileType.PNG
                } else {
                    fingerprint.fileType
                }
                val saveFile = File(saveDir, "${fingerprint.md5}.${extType.displayName}")
                if (!saveFile.exists()) {
                    val copied = runCatching {
                        appCtx.contentResolver.openInputStream(node.uri)?.use { input ->
                            saveFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: false
                    }.isSuccess
                    if (!copied) {
                        failed += 1
                        continue
                    }
                }

                val imageBean = ImageBean(
                    md5 = "${schemeTag}_${fingerprint.md5}",
                    imagePath = saveFile.absolutePath,
                    fileLength = length,
                    fileTime = node.lastModified(),
                    fileType = fingerprint.fileType,
                    fileName = saveFile.name,
                    secondMenu = schemeTag,
                    scanTime = System.currentTimeMillis(),
                    cachePath = pathConfig.relativePath
                )
                val rowId = dyImageDao.insertOrIgnore(imageBean)
                if (rowId == -1L) duplicates += 1 else added += 1
            }
        }
        return ScanSummary(false, scanned, added, duplicates, skippedSmallFiles, failed)
    }

    private suspend fun scanWithShizuku(
        scheme: ScanSchemeEntity,
        pathConfigs: List<ScanPathEntity>,
        minSizeBytes: Long
    ): ScanSummary {
        val service = FileExplorerService.service ?: return ScanSummary(true, 0, 0, 0, 0, 0)
        val root = runCatching { service.getFileBean(scheme.rootPath) }.getOrNull()
            ?: return ScanSummary(true, 0, 0, 0, 0, 0)

        val saveDir = resolveSaveDir(scheme)
        val schemeTag = schemeTag(scheme.id)
        var scanned = 0
        var added = 0
        var duplicates = 0
        var skippedSmallFiles = 0
        var failed = 0

        pathConfigs.forEach { pathConfig ->
            val target = root.findDocument(pathConfig.relativePath) ?: return@forEach
            val stack = ArrayDeque<FileBean>()
            stack.add(target)
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (node.isDirectory == true) {
                    node.listFiles().forEach(stack::addLast)
                    continue
                }
                if (!node.isFile) continue
                scanned += 1
                if ((node.length ?: 0L) < minSizeBytes) {
                    skippedSmallFiles += 1
                    continue
                }
                val rawImageBean = runCatching {
                    service.copyToMyFile(
                        node,
                        minSizeBytes,
                        0,
                        schemeTag,
                        saveDir.path,
                        listOf(pathConfig.relativePath)
                    )
                }.getOrNull()
                val imageBean = rawImageBean?.copy(
                    md5 = "${schemeTag}_${rawImageBean.md5}",
                    secondMenu = schemeTag,
                    cachePath = pathConfig.relativePath
                )
                if (imageBean == null) {
                    failed += 1
                    continue
                }
                val rowId = dyImageDao.insertOrIgnore(imageBean)
                if (rowId == -1L) duplicates += 1 else added += 1
            }
        }

        return ScanSummary(true, scanned, added, duplicates, skippedSmallFiles, failed)
    }

    private data class Fingerprint(
        val md5: String,
        val fileType: FileType
    )

    private fun DocumentFile.fingerprint(): Fingerprint {
        val stream = appCtx.contentResolver.openInputStream(uri)
            ?: error("openInputStream failed")
        stream.use { return it.fingerprint() }
    }

    private fun InputStream.fingerprint(): Fingerprint {
        val md5 = MessageDigest.getInstance("MD5")
        val header = ByteArray(12)
        var headerOffset = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            md5.update(buffer, 0, read)
            if (headerOffset < header.size) {
                val copySize = min(header.size - headerOffset, read)
                System.arraycopy(buffer, 0, header, headerOffset, copySize)
                headerOffset += copySize
            }
        }
        val md5Value = BigInteger(1, md5.digest()).toString(16).padStart(32, '0')
        val type = FileTypeChecker.getType(header.copyOf(headerOffset))
        return Fingerprint(md5 = md5Value, fileType = type)
    }
}
