package com.example.bits_helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartridgeDao {
    @Query("SELECT * FROM cartridges ORDER BY id ASC")
    fun observeAll(): Flow<List<CartridgeEntity>>

    @Query("SELECT * FROM cartridges WHERE number = :number LIMIT 1")
    suspend fun findByNumber(number: String): CartridgeEntity?

    @Query("SELECT * FROM cartridges WHERE number = :number ORDER BY id DESC LIMIT 1")
    suspend fun findLatestByNumber(number: String): CartridgeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(items: List<CartridgeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOne(item: CartridgeEntity)

    @Query("DELETE FROM cartridges")
    suspend fun clear()

    @Query("UPDATE cartridges SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: Status)

    @Query("UPDATE cartridges SET status = 'IN_REFILL' WHERE status = 'COLLECTED'")
    suspend fun updateCollectedToRefill(): Int

    @Query("SELECT * FROM cartridges WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CartridgeEntity?

    @Query("DELETE FROM cartridges WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE cartridges SET number = :number, room = :room, model = :model, date = :date, status = :status, notes = :notes, department = :department WHERE id = :id")
    suspend fun updateCartridge(id: Long, number: String, room: String, model: String, date: String, status: Status, notes: String?, department: String?)
}


