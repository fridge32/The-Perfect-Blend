package com.example.the_perfect_blend.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ColorEntity::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Wipe the database and recreate it
                database.execSQL("DROP TABLE IF EXISTS colors")
                database.execSQL("""
                    CREATE TABLE colors (
                        hex TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        seen INTEGER NOT NULL DEFAULT 0,
                        matched INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
    }
}
