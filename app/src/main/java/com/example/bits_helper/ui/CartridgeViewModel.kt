package com.example.bits_helper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bits_helper.data.CartridgeRepository
import com.example.bits_helper.data.CartridgeEntity
import com.example.bits_helper.data.DepartmentEntity
import com.example.bits_helper.data.Status
import com.example.bits_helper.data.StatusUpdateResult
import com.example.bits_helper.data.AppDatabase
import android.content.Context
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class CartridgeUi(
    val id: Long,
    val number: String,
    val room: String,
    val model: String,
    val date: String,
    val status: Status,
    val notes: String?,
    val department: String?
)

class CartridgeViewModel(
    private val repository: CartridgeRepository,
    private val context: Context
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0)
    private val forceRefreshTrigger = MutableStateFlow(0)
    
    private val allCartridges: StateFlow<List<CartridgeUi>> = combine(
        repository.observeCartridges(),
        refreshTrigger,
        forceRefreshTrigger
    ) { list, _, _ -> 
        list.map { it.toUi() } 
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val selectedStatus = MutableStateFlow<Status?>(null)

    val cartridges: StateFlow<List<CartridgeUi>> = combine(allCartridges, selectedStatus) { list, filter ->
        if (filter == null) list else list.filter { it.status == filter }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val countsByStatus: StateFlow<Map<Status, Int>> = allCartridges
        .map { list -> list.groupingBy { it.status }.eachCount() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun setFilter(status: Status?) { selectedStatus.value = status }

    fun add(number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        viewModelScope.launch { repository.addCartridge(number, room, model, date, status, notes) }
    }

    fun updateStatus(id: Long, status: Status) {
        viewModelScope.launch { repository.updateStatus(id, status) }
    }

    fun progressByNumber(number: String, onResult: (StatusUpdateResult?) -> Unit) {
        viewModelScope.launch {
            val result = repository.progressStatusByNumber(number)
            onResult(result)
        }
    }

    fun updateCollectedToRefill(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val updatedCount = repository.updateCollectedToRefill()
            onResult(updatedCount)
        }
    }

    fun findById(id: Long, onResult: (CartridgeUi?) -> Unit) {
        viewModelScope.launch {
            val entity = repository.findById(id)
            onResult(entity?.toUi())
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun updateCartridge(id: Long, number: String, room: String, model: String, date: String, status: Status, notes: String?) {
        viewModelScope.launch {
            repository.updateCartridge(id, number, room, model, date, status, notes)
        }
    }

    fun getFilteredStatistics(
        dateFrom: String,
        dateTo: String,
        department: String?,
        status: Status?
    ): StateFlow<List<CartridgeUi>> {
        return allCartridges.map { list ->
            list.filter { item ->
                var matches = true
                
                // Фильтр по датам
                if (dateFrom.isNotBlank() && item.date < dateFrom) matches = false
                if (dateTo.isNotBlank() && item.date > dateTo) matches = false
                
                // Фильтр по филиалу
                if (department != null && item.department != department) matches = false
                
                // Фильтр по статусу
                if (status != null && item.status != status) matches = false
                
                matches
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun getAllDepartments(onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            val departments = repository.getAllDepartments()
            onResult(departments)
        }
    }

    fun getAllDepartmentEntities(onResult: (List<DepartmentEntity>) -> Unit) {
        viewModelScope.launch {
            val departments = repository.getAllDepartmentEntities()
            onResult(departments)
        }
    }

    fun addDepartment(name: String, rooms: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.addDepartment(name, rooms)
                onResult(true)
                refreshData() // Обновляем данные после добавления
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun updateDepartment(name: String, rooms: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateDepartment(name, rooms)
                onResult(true)
                refreshData() // Обновляем данные после изменения
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteDepartment(name: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteDepartment(name)
                onResult(true)
                refreshData() // Обновляем данные после удаления
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /**
     * Обновляет подразделения для всех картриджей без подразделения
     */
    fun updateMissingDepartments(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val updatedCount = repository.updateMissingDepartments()
            onResult(updatedCount)
            // Обновляем данные после изменения
            refreshData()
        }
    }

    /**
     * Проверяет количество картриджей без подразделения
     */
    fun getCartridgesWithoutDepartmentCount(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.getCartridgesWithoutDepartmentCount()
            onResult(count)
        }
    }

    fun getCartridgeCount(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.getCartridgeCount()
            onResult(count)
        }
    }

    fun clearAllCartridges(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                repository.clearAllCartridges()
                onResult(true)
                refreshData() // Обновляем данные после очистки
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun clearAllSyncData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val syncManager = com.example.bits_helper.data.SyncManager(context)
                syncManager.clearAllSyncData()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    
    /**
     * Принудительно обновляет данные из базы
     */
    fun refreshData() {
        refreshTrigger.value++
    }
    
    /**
     * Принудительно перезагружает все данные из базы
     * Используется после синхронизации или импорта
     */
    fun forceRefreshData() {
        viewModelScope.launch {
            // Принудительно обновляем оба триггера
            forceRefreshTrigger.value++
            kotlinx.coroutines.delay(50) // Небольшая задержка
            refreshTrigger.value++
            kotlinx.coroutines.delay(50) // Еще одна задержка
            forceRefreshTrigger.value++
        }
    }
    
    /**
     * Принудительно пересоздает подключение к базе данных
     * Используется после синхронизации или импорта для полного обновления
     */
    fun forceReconnectDatabase() {
        viewModelScope.launch {
            // Принудительно пересоздаем подключение к базе данных
            AppDatabase.forceReconnect(context)
            
            // Обновляем триггеры для принудительного обновления UI
            forceRefreshTrigger.value++
            kotlinx.coroutines.delay(200) // Даем больше времени на переподключение
            refreshTrigger.value++
            kotlinx.coroutines.delay(100) // Еще одна задержка для гарантии
            forceRefreshTrigger.value++
        }
    }

}

private fun CartridgeEntity.toUi(): CartridgeUi =
    CartridgeUi(id = id, number = number, room = room, model = model, date = date, status = status, notes = notes, department = department)


