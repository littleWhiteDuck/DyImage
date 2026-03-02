package hua.dy.image.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import hua.dy.image.bean.ImageBean
import splitties.init.appCtx

@Database(
    entities = [ImageBean::class, ScanPathEntity::class, ScanSchemeEntity::class],
    version = 3
)
abstract class DyImageDataBase : RoomDatabase() {
    abstract val dyImageDao: DyImageDao
    abstract val scanPathDao: ScanPathDao
    abstract val scanSchemeDao: ScanSchemeDao
}

private val migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scan_path (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                relative_path TEXT NOT NULL,
                note TEXT NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                is_chat_path INTEGER NOT NULL DEFAULT 0,
                order_index INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }
}

private val migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scan_scheme (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                package_name TEXT NOT NULL,
                root_path TEXT NOT NULL,
                save_folder TEXT NOT NULL,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT OR IGNORE INTO scan_scheme(id, name, package_name, root_path, save_folder, created_at)
            VALUES (
                1,
                '抖音',
                'com.ss.android.ugc.aweme',
                '/sdcard/Android/data/com.ss.android.ugc.aweme',
                'douyin',
                strftime('%s','now') * 1000
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS scan_path_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scheme_id INTEGER NOT NULL DEFAULT 1,
                relative_path TEXT NOT NULL,
                note TEXT NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                order_index INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO scan_path_new(id, scheme_id, relative_path, note, is_enabled, order_index, created_at)
            SELECT id, 1, relative_path, note, is_enabled, order_index, created_at
            FROM scan_path
            """.trimIndent()
        )

        db.execSQL("DROP TABLE scan_path")
        db.execSQL("ALTER TABLE scan_path_new RENAME TO scan_path")
    }
}

val dyImageDb: DyImageDataBase by lazy {
    Room.databaseBuilder(appCtx, DyImageDataBase::class.java, "dy_image.db")
        .addMigrations(migration1To2, migration2To3)
        .fallbackToDestructiveMigration(false)
        .build()
}

val dyImageDao: DyImageDao by lazy { dyImageDb.dyImageDao }
val scanPathDao: ScanPathDao by lazy { dyImageDb.scanPathDao }
val scanSchemeDao: ScanSchemeDao by lazy { dyImageDb.scanSchemeDao }

