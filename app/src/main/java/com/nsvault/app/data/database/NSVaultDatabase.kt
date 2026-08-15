package com.nsvault.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RecordingEntity::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class NSVaultDatabase : RoomDatabase() {

    abstract fun recordingDao(): RecordingDao

    companion object {

        /**
         * v1 → v2: recordings gain a stable UUID identity, a status
         * lifecycle, and a container column. Existing rows are treated
         * as COMPLETED and receive generated UUIDs.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE recordings_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        uuid TEXT NOT NULL,
                        title TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        duration_ms INTEGER NOT NULL,
                        size_bytes INTEGER NOT NULL,
                        relative_path TEXT NOT NULL,
                        container TEXT NOT NULL,
                        status TEXT NOT NULL,
                        is_favorite INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO recordings_new
                        (id, uuid, title, created_at, duration_ms, size_bytes,
                         relative_path, container, status, is_favorite)
                    SELECT id,
                           lower(hex(randomblob(4))) || '-' ||
                           lower(hex(randomblob(2))) || '-4' ||
                           substr(lower(hex(randomblob(2))), 2) || '-' ||
                           substr('89ab', (abs(random()) % 4) + 1, 1) ||
                           substr(lower(hex(randomblob(2))), 2) || '-' ||
                           lower(hex(randomblob(6))),
                           title, created_at, duration_ms, size_bytes,
                           relative_path, 'm4a', 'COMPLETED', is_favorite
                    FROM recordings
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE recordings")
                db.execSQL("ALTER TABLE recordings_new RENAME TO recordings")
                db.execSQL("CREATE INDEX index_recordings_created_at ON recordings(created_at)")
                db.execSQL("CREATE UNIQUE INDEX index_recordings_uuid ON recordings(uuid)")
            }
        }

        /** v2 → v3: recordings gain an optional waveform preview blob. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN waveform BLOB")
            }
        }

        /** v3 → v4: recordings gain a resume position column. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recordings ADD COLUMN resume_position_ms INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
