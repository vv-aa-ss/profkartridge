package com.example.bits_helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name")
    suspend fun getAll(): List<DepartmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(departments: List<DepartmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOne(department: DepartmentEntity)

    @Query("SELECT name FROM departments WHERE rooms LIKE '%' || :room || '%' LIMIT 1")
    suspend fun findByRoom(room: String): String?

    @Query("SELECT * FROM departments WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): DepartmentEntity?

    @Query("UPDATE departments SET rooms = :rooms WHERE name = :name")
    suspend fun updateRooms(name: String, rooms: String)

    @Query("DELETE FROM departments WHERE name = :name")
    suspend fun deleteByName(name: String)

    @Query("SELECT COUNT(*) FROM departments")
    suspend fun getCount(): Int
}
