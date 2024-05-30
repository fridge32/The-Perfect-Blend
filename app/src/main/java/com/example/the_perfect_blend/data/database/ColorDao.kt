package com.example.the_perfect_blend.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ColorDao {
    @Query("SELECT * FROM colors WHERE hex = :hex")
    suspend fun getColorByHex(hex: String): ColorEntity?

    @Query("SELECT * FROM colors")
    suspend fun getAllColors(): List<ColorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(colors: List<ColorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(color: ColorEntity)
}
