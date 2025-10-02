package com.example.bits_helper.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStatus(value: Status?): String? = value?.name

    @TypeConverter
    fun toStatus(value: String?): Status? = value?.let { Status.valueOf(it) }
}


