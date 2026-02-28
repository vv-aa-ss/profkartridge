package com.example.bits_helper.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Запись в журнале изменений статуса картриджа.
 * Позволяет отследить историю: когда и в какой статус переходил картридж.
 * Пример: 00091 — 01.02 собран, 05.02 на заправке, 08.02 принят, 08.02 роздан.
 */
@Entity(tableName = "cartridge_history")
data class CartridgeHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cartridgeNumber: String,
    val date: String,           // yyyy-MM-dd
    val status: Status,
    val room: String? = null,   // кабинет на момент изменения
    val model: String? = null   // модель на момент изменения
)
