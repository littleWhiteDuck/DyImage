package hua.dy.image.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanPathDao {

    @Query("SELECT * FROM scan_path WHERE scheme_id = :schemeId ORDER BY order_index ASC, id ASC")
    fun observeAllByScheme(schemeId: Long): Flow<List<ScanPathEntity>>

    @Query("SELECT * FROM scan_path WHERE scheme_id = :schemeId AND is_enabled = 1 ORDER BY order_index ASC, id ASC")
    suspend fun getEnabledByScheme(schemeId: Long): List<ScanPathEntity>

    @Query("SELECT * FROM scan_path WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanPathEntity?

    @Query("SELECT count(*) FROM scan_path WHERE scheme_id = :schemeId")
    suspend fun getCountByScheme(schemeId: Long): Int

    @Upsert
    suspend fun upsert(entity: ScanPathEntity)

    @Query("DELETE FROM scan_path WHERE scheme_id = :schemeId")
    suspend fun deleteAllByScheme(schemeId: Long)

    @Query("DELETE FROM scan_path")
    suspend fun deleteAll()

    @Delete
    suspend fun delete(entity: ScanPathEntity)
}
