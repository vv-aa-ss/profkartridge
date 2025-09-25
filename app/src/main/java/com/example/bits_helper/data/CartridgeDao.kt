package com.example.bits_helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartridgeDao {
    @Query("SELECT * FROM cartridges ORDER BY id DESC")
    fun observeAll(): Flow<List<CartridgeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<CartridgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOne(item: CartridgeEntity)

    @Query("DELETE FROM cartridges")
    suspend fun clear()

    @Query("UPDATE cartridges SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Status)
}


