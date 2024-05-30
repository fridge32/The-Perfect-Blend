package com.example.the_perfect_blend.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "colors")
data class ColorEntity(
    @PrimaryKey val hex: String,
    val name: String
)
