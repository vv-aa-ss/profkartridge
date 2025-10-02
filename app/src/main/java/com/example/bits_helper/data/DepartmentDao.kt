package com.example.bits_helper.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments")
    suspend fun getAll(): List<DepartmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(departments: List<DepartmentEntity>)

    @Query("SELECT name FROM departments WHERE rooms LIKE '%' || :room || '%' LIMIT 1")
    suspend fun findByRoom(room: String): String?

    @Query("SELECT * FROM departments WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): DepartmentEntity?
}
