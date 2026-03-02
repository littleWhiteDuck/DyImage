package hua.dy.image.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_path")
data class ScanPathEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "scheme_id", defaultValue = "1")
    val schemeId: Long = 1L,
    @ColumnInfo(name = "relative_path")
    val relativePath: String,
    @ColumnInfo(name = "note")
    val note: String,
    @ColumnInfo(name = "is_enabled", defaultValue = "1")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "order_index", defaultValue = "0")
    val orderIndex: Int = 0,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)
