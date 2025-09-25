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

    suspend fun progressStatusByNumber(number: String) : Status? {
        val item = dao.findByNumber(number) ?: return null
        val next = when (item.status) {
            Status.ISSUED -> Status.COLLECTED      // Роздан -> Собран
            Status.COLLECTED -> Status.IN_REFILL   // Собран -> На заправке
            Status.IN_REFILL -> Status.RECEIVED    // На заправке -> Принят
            Status.RECEIVED -> Status.ISSUED        // Принят -> Роздан (замыкаем круг)
            Status.WRITTEN_OFF, Status.LOST -> item.status // Списан/Потерян не участвуют в цикле
        }
        if (next != item.status) dao.updateStatus(item.id, next)
        return next
    }
}


