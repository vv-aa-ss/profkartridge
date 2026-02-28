package com.example.bits_helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartridgeHistoryDao {
    @Insert
    suspend fun insert(entry: CartridgeHistoryEntity)

    @Query("SELECT * FROM cartridge_history WHERE cartridgeNumber = :number ORDER BY date DESC, id DESC")
    suspend fun getHistoryByNumber(number: String): List<CartridgeHistoryEntity>

    @Query("SELECT * FROM cartridge_history ORDER BY date DESC, id DESC")
    fun observeAllHistory(): Flow<List<CartridgeHistoryEntity>>

    @Query("DELETE FROM cartridge_history")
    suspend fun clear()
}
