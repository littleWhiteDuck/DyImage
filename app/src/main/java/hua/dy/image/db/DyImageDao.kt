package hua.dy.image.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import hua.dy.image.bean.ImageBean
import hua.dy.image.utils.FileType

@Dao
interface DyImageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(imageBean: ImageBean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(imageBean: ImageBean): Long

    @Query("SELECT * FROM dy_image ORDER BY file_time COLLATE NOCASE DESC")
    fun getImageListByFileTime(): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image ORDER BY file_length COLLATE NOCASE DESC")
    fun getImageListByFileLength(): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image ORDER BY scan_time COLLATE NOCASE DESC")
    fun getImageListByScanTime(): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image where file_type =:type ORDER BY file_time COLLATE NOCASE DESC")
    fun getImageListByFileTime(type: FileType): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image WHERE second_menu = :schemeTag AND file_type = :type ORDER BY file_time COLLATE NOCASE DESC")
    fun getImageListByFileTime(type: FileType, schemeTag: String): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image where file_type =:type ORDER BY file_length COLLATE NOCASE DESC")
    fun getImageListByFileLength(type: FileType): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image WHERE second_menu = :schemeTag AND file_type = :type ORDER BY file_length COLLATE NOCASE DESC")
    fun getImageListByFileLength(type: FileType, schemeTag: String): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image where file_type =:type ORDER BY scan_time COLLATE NOCASE DESC")
    fun getImageListByScanTime(type: FileType): PagingSource<Int, ImageBean>

    @Query("SELECT * FROM dy_image WHERE second_menu = :schemeTag AND file_type = :type ORDER BY scan_time COLLATE NOCASE DESC")
    fun getImageListByScanTime(type: FileType, schemeTag: String): PagingSource<Int, ImageBean>

    @Delete
    suspend fun deleteImage(imageBean: ImageBean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(cheeses: List<ImageBean>)

    @Query("SELECT count(*) FROM dy_image WHERE md5 = :md5")
    suspend fun selectMd5Exist(md5: String): Int

    @Query("SELECT count(*) FROM dy_image")
    suspend fun getImageCount(): Int

    @Query("SELECT count(*) FROM dy_image WHERE second_menu = :schemeTag")
    suspend fun getImageCountBySchemeTag(schemeTag: String): Int

    @Query("SELECT image_path FROM dy_image WHERE second_menu = :schemeTag ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomImagePathsBySchemeTag(schemeTag: String, limit: Int): List<String>

    @Query("SELECT count(*) FROM dy_image WHERE cache_path IN (:cachePaths)")
    suspend fun getImageCountByCachePaths(cachePaths: List<String>): Int

    @Query(
        """
        UPDATE dy_image
        SET image_path = :imagePath,
            file_length = :fileLength,
            file_time = :fileTime,
            file_type = :fileType,
            file_name = :fileName,
            scan_time = :scanTime,
            cache_path = :cachePath
        WHERE md5 = :md5
        """
    )
    suspend fun refreshImageMeta(
        md5: String,
        imagePath: String,
        fileLength: Long,
        fileTime: Long,
        fileType: FileType,
        fileName: String,
        scanTime: Long,
        cachePath: String
    ): Int

    @Query("delete from dy_image")
    suspend fun deleteAll(): Int

}
