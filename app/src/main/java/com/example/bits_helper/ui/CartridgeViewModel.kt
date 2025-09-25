package com.example.bits_helper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bits_helper.data.CartridgeRepository
import com.example.bits_helper.data.CartridgeEntity
import com.example.bits_helper.data.Status
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CartridgeUi(
    val id: Long,
    val number: String,
    val room: String,
    val model: String,
    val date: String,
    val status: Status,
    val notes: String?
)

class CartridgeViewModel(
    private val repository: CartridgeRepository
) : ViewModel() {

    private val allCartridges: StateFlow<List<CartridgeUi>> = repository
        .observeCartridges()
        .map { list -> list.map { it.toUi() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun progressByNumber(number: String, onResult: (Status?) -> Unit) {
        viewModelScope.launch {
            val next = repository.progressStatusByNumber(number)
            onResult(next)
        }
    }
}

private fun CartridgeEntity.toUi(): CartridgeUi =
    CartridgeUi(id = id, number = number, room = room, model = model, date = date, status = status, notes = notes)


