package com.example.bits_helper.data

import kotlinx.coroutines.flow.Flow

data class StatusUpdateResult(
    val number: String,
    val newStatus: Status,
    val room: String,
    val model: String,
    val department: String?
)

class CartridgeRepository(
    private val dao: CartridgeDao,
    private val departmentDao: DepartmentDao,
    private val historyDao: CartridgeHistoryDao
) {
    private suspend fun logHistory(number: String, date: String, status: Status, room: String? = null, model: String? = null) {
        historyDao.insert(CartridgeHistoryEntity(cartridgeNumber = number, date = date, status = status, room = room, model = model))
    }
    fun observeCartridges(): Flow<List<CartridgeEntity>> = dao.observeAll()

    suspend fun addCartridge(number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        val department = departmentDao.findByRoom(room)
        dao.insertOne(CartridgeEntity(number = number, room = room, model = model, date = date, status = status, notes = notes, department = department))
        logHistory(number, date, status, room, model)
    }

    suspend fun updateStatus(id: Long, status: Status) {
        val cartridge = dao.findById(id) ?: return
        val date = java.time.LocalDate.now().toString()
        dao.updateStatus(id, status)
        logHistory(cartridge.number, date, status, cartridge.room, cartridge.model)
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
                logHistory(item.number, today, Status.COLLECTED, item.room, item.model)
                return StatusUpdateResult(item.number, Status.COLLECTED, item.room, item.model, department)
            }
            Status.COLLECTED -> {
                // Собран -> На заправке
                val today = java.time.LocalDate.now().toString()
                dao.updateStatus(item.id, Status.IN_REFILL)
                logHistory(item.number, today, Status.IN_REFILL, item.room, item.model)
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, Status.IN_REFILL, item.room, item.model, department)
            }
            Status.IN_REFILL -> {
                // На заправке -> Принят
                val today = java.time.LocalDate.now().toString()
                dao.updateStatus(item.id, Status.RECEIVED)
                logHistory(item.number, today, Status.RECEIVED, item.room, item.model)
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, Status.RECEIVED, item.room, item.model, department)
            }
            Status.RECEIVED -> {
                // Принят -> Роздан
                val today = java.time.LocalDate.now().toString()
                dao.updateStatus(item.id, Status.ISSUED)
                logHistory(item.number, today, Status.ISSUED, item.room, item.model)
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, Status.ISSUED, item.room, item.model, department)
            }
            Status.WRITTEN_OFF, Status.LOST -> {
                // Списан/Потерян не участвуют в цикле
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, item.status, item.room, item.model, department)
            }
        }
    }

    suspend fun updateCollectedToRefill(): Int {
        val collected = dao.findAllWithStatusCollected()
        val today = java.time.LocalDate.now().toString()
        for (c in collected) {
            logHistory(c.number, today, Status.IN_REFILL, c.room, c.model)
        }
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
        val today = java.time.LocalDate.now().toString()
        dao.updateCartridge(id, number, room, model, date, status, notes, department)
        logHistory(number, today, status, room, model)
    }

    suspend fun getAllDepartments(): List<String> {
        return departmentDao.getAll().map { it.name }
    }

    suspend fun getAllDepartmentEntities(): List<DepartmentEntity> {
        return departmentDao.getAll()
    }

    suspend fun addDepartment(name: String, rooms: String) {
        departmentDao.insertOne(DepartmentEntity(name, rooms))
    }

    suspend fun updateDepartment(name: String, rooms: String) {
        departmentDao.updateRooms(name, rooms)
    }

    suspend fun deleteDepartment(name: String) {
        departmentDao.deleteByName(name)
    }

    suspend fun getDepartmentCount(): Int {
        return departmentDao.getCount()
    }

    suspend fun getCartridgeCount(): Int {
        return dao.getCartridgeCount()
    }

    suspend fun clearAllCartridges() {
        dao.clear()
        historyDao.clear()
    }

    /**
     * Обновляет подразделения для всех картриджей, у которых department = NULL
     */
    suspend fun updateMissingDepartments(): Int {
        val departments = departmentDao.getAll()
        val countBefore = dao.countCartridgesWithoutDepartment()
        
        for (dept in departments) {
            val rooms = dept.rooms.split(",").map { it.trim() }
            for (room in rooms) {
                dao.updateDepartmentByRoom(room, dept.name)
            }
        }
        
        val countAfter = dao.countCartridgesWithoutDepartment()
        return countBefore - countAfter
    }

    /**
     * Возвращает количество картриджей без подразделения
     */
    suspend fun getCartridgesWithoutDepartmentCount(): Int {
        return dao.countCartridgesWithoutDepartment()
    }

    /** История статусов картриджа по номеру */
    suspend fun getHistoryByNumber(number: String): List<CartridgeHistoryEntity> =
        historyDao.getHistoryByNumber(number)

}


