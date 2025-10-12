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
                return StatusUpdateResult(item.number, Status.COLLECTED, item.room, item.model, department)
            }
            Status.COLLECTED -> {
                // Собран -> На заправке
                dao.updateStatus(item.id, Status.IN_REFILL)
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, Status.IN_REFILL, item.room, item.model, department)
            }
            Status.IN_REFILL -> {
                // На заправке -> Принят
                dao.updateStatus(item.id, Status.RECEIVED)
                val department = departmentDao.findByRoom(item.room)
                return StatusUpdateResult(item.number, Status.RECEIVED, item.room, item.model, department)
            }
            Status.RECEIVED -> {
                // Принят -> Роздан
                dao.updateStatus(item.id, Status.ISSUED)
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

}


