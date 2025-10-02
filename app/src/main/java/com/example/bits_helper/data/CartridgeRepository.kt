package com.example.bits_helper.data

import kotlinx.coroutines.flow.Flow

data class StatusUpdateResult(
    val number: String,
    val newStatus: Status
)

class CartridgeRepository(
    private val dao: CartridgeDao,
    private val departmentDao: DepartmentDao
) {
    fun observeCartridges(): Flow<List<CartridgeEntity>> = dao.observeAll()

    suspend fun addCartridge(number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        val department = departmentDao.findByRoom(room)
        dao.insertOne(CartridgeEntity(number = number, room = room, model = model, date = date, status = status, notes = notes, department = department))
    }

    suspend fun updateStatus(id: Long, status: Status) {
        dao.updateStatus(id, status)
    }

    suspend fun progressStatusByNumber(number: String) : StatusUpdateResult? {
        // Ищем последнюю запись этого картриджа
        val item = dao.findLatestByNumber(number) ?: return null
        
        when (item.status) {
            Status.ISSUED -> {
                // Если картридж "Роздан", создаем новую запись "Собран" с текущей датой
                val today = java.time.LocalDate.now().toString()
                val department = departmentDao.findByRoom(item.room)
                val newItem = CartridgeEntity(
                    number = item.number,
                    room = item.room,
                    model = item.model,
                    date = today,
                    status = Status.COLLECTED,
                    notes = item.notes,
                    department = department
                )
                dao.insertOne(newItem)
                return StatusUpdateResult(item.number, Status.COLLECTED)
            }
            Status.COLLECTED -> {
                // Собран -> На заправке
                dao.updateStatus(item.id, Status.IN_REFILL)
                return StatusUpdateResult(item.number, Status.IN_REFILL)
            }
            Status.IN_REFILL -> {
                // На заправке -> Принят
                dao.updateStatus(item.id, Status.RECEIVED)
                return StatusUpdateResult(item.number, Status.RECEIVED)
            }
            Status.RECEIVED -> {
                // Принят -> Роздан
                dao.updateStatus(item.id, Status.ISSUED)
                return StatusUpdateResult(item.number, Status.ISSUED)
            }
            Status.WRITTEN_OFF, Status.LOST -> {
                // Списан/Потерян не участвуют в цикле
                return StatusUpdateResult(item.number, item.status)
            }
        }
    }

    suspend fun updateCollectedToRefill(): Int {
        return dao.updateCollectedToRefill()
    }

    suspend fun findById(id: Long): CartridgeEntity? {
        return dao.findById(id)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    suspend fun updateCartridge(id: Long, number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        val department = departmentDao.findByRoom(room)
        dao.updateCartridge(id, number, room, model, date, status, notes, department)
    }

    suspend fun getAllDepartments(): List<String> {
        return departmentDao.getAll().map { it.name }
    }

}


