package com.example.the_perfect_blend.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ColorEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun colorDao(): ColorDao
}