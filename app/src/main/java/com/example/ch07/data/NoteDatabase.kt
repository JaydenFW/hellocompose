package com.example.ch07.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// v1 -> v2: kolom tags (Tugas 4). Data lama diberi daftar tag kosong "[]".
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
    }
}

// v2 -> v3: kolom updatedAt (Tugas 3). Catatan lama dianggap terakhir
// diubah saat dibuat, bukan 0 (yang akan tampil sebagai "puluhan tahun lalu").
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE notes ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE notes SET updatedAt = createdAt")
    }
}

@Database(
    entities = [NoteEntity::class],
    version = 3,
    exportSchema = true          // JSON skema -> app/schemas/ (lihat build.gradle.kts)
)
@TypeConverters(Converters::class)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        // @Volatile: INSTANCE selalu dibaca dari memori utama, bukan cache thread
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        // synchronized: mencegah dua thread membuat instance bersamaan
        fun getInstance(context: Context): NoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database"
                )
                    // Migration eksplisit, BUKAN fallbackToDestructiveMigration:
                    // data pengguna tidak boleh hilang saat versi naik.
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
