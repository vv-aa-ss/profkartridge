package com.example.bits_helper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "departments")
data class DepartmentEntity(
    @PrimaryKey val name: String,
    val rooms: String // Список номеров кабинетов через запятую, например "428,429,430,421,415"
)
