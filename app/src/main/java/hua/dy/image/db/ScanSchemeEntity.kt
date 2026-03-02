package hua.dy.image.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_scheme")
data class ScanSchemeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "root_path")
    val rootPath: String,
    @ColumnInfo(name = "save_folder")
    val saveFolder: String,
    @ColumnInfo(name = "created_at", defaultValue = "0")
    val createdAt: Long = System.currentTimeMillis()
)

