package com.hillbeater.remindora.utils

import androidx.compose.ui.graphics.Color
import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromColor(color: Color): Long {
        return color.value.toLong()
    }

    @TypeConverter
    fun toColor(colorLong: Long): Color {
        return Color(colorLong)
    }
}