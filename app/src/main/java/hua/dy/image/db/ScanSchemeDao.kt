package hua.dy.image.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSchemeDao {

    @Query("SELECT * FROM scan_scheme ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<ScanSchemeEntity>>

    @Query("SELECT * FROM scan_scheme WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanSchemeEntity?

    @Query("SELECT * FROM scan_scheme ORDER BY created_at ASC, id ASC LIMIT 1")
    suspend fun getFirst(): ScanSchemeEntity?

    @Query("SELECT count(*) FROM scan_scheme")
    suspend fun getCount(): Int

    @Query("DELETE FROM scan_scheme")
    suspend fun deleteAll()

    @Upsert
    suspend fun upsert(entity: ScanSchemeEntity)

    @Delete
    suspend fun delete(entity: ScanSchemeEntity)
}
