package com.example.the_perfect_blend.utils
import com.example.the_perfect_blend.data.database.ColorEntity

object Cache {
    private val colorCache = mutableMapOf<String, ColorEntity>()

    fun getColor(hex: String): ColorEntity? {
        return colorCache[hex]
    }

    fun putColor(color: ColorEntity) {
        colorCache[color.hex] = color
    }

    fun clear() {
        colorCache.clear()
    }
}
