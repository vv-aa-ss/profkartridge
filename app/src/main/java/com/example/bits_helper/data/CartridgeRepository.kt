package com.example.bits_helper.data

import kotlinx.coroutines.flow.Flow

class CartridgeRepository(
    private val dao: CartridgeDao
) {
    fun observeCartridges(): Flow<List<CartridgeEntity>> = dao.observeAll()

    suspend fun addCartridge(number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        dao.insertOne(CartridgeEntity(number = number, room = room, model = model, date = date, status = status, notes = notes))
    }

    suspend fun updateStatus(id: Long, status: Status) {
        dao.updateStatus(id, status)
    }
}


