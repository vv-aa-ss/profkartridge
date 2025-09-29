package com.example.bits_helper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cartridges")
data class CartridgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val room: String,
    val model: String,
    val date: String,
    val status: Status = Status.ISSUED,
    val notes: String? = null,
    val department: String? = null
)


