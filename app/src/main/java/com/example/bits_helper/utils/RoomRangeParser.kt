package com.example.bits_helper.utils

/**
 * Утилиты для работы с диапазонами кабинетов
 */
object RoomRangeParser {
    
    /**
     * Парсит строку с кабинетами и возвращает список номеров кабинетов
     * Поддерживает форматы:
     * - "101,102,103" - отдельные номера через запятую
     * - "101-105" - диапазон от 101 до 105 включительно
     * - "101,102,201-205,301" - смешанный формат
     * 
     * @param roomsString строка с кабинетами
     * @return список номеров кабинетов
     */
    fun parseRooms(roomsString: String): List<String> {
        if (roomsString.isBlank()) return emptyList()
        
        val rooms = mutableSetOf<String>()
        val parts = roomsString.split(",").map { it.trim() }
        
        for (part in parts) {
            if (part.contains("-")) {
                // Диапазон (например, "101-105")
                val rangeParts = part.split("-")
                if (rangeParts.size == 2) {
                    val start = rangeParts[0].trim().toIntOrNull()
                    val end = rangeParts[1].trim().toIntOrNull()
                    
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) {
                            rooms.add(i.toString())
                        }
                    }
                }
            } else {
                // Отдельный номер
                val roomNumber = part.trim()
                if (roomNumber.isNotBlank()) {
                    rooms.add(roomNumber)
                }
            }
        }
        
        return rooms.sortedBy { it.toIntOrNull() ?: 0 }
    }
    
    /**
     * Форматирует список кабинетов в компактную строку
     * Группирует последовательные номера в диапазоны
     * 
     * @param rooms список номеров кабинетов
     * @return отформатированная строка
     */
    fun formatRooms(rooms: List<String>): String {
        if (rooms.isEmpty()) return ""
        
        val sortedRooms = rooms.mapNotNull { it.toIntOrNull() }.sorted()
        if (sortedRooms.isEmpty()) return rooms.joinToString(",")
        
        val ranges = mutableListOf<String>()
        var start = sortedRooms[0]
        var end = sortedRooms[0]
        
        for (i in 1 until sortedRooms.size) {
            if (sortedRooms[i] == end + 1) {
                // Продолжаем диапазон
                end = sortedRooms[i]
            } else {
                // Завершаем текущий диапазон и начинаем новый
                ranges.add(formatRange(start, end))
                start = sortedRooms[i]
                end = sortedRooms[i]
            }
        }
        
        // Добавляем последний диапазон
        ranges.add(formatRange(start, end))
        
        return ranges.joinToString(",")
    }
    
    private fun formatRange(start: Int, end: Int): String {
        return if (start == end) {
            start.toString()
        } else {
            "$start-$end"
        }
    }
    
    /**
     * Проверяет, содержит ли строка с кабинетами указанный номер кабинета
     * 
     * @param roomsString строка с кабинетами
     * @param roomNumber номер кабинета для поиска
     * @return true, если кабинет найден
     */
    fun containsRoom(roomsString: String, roomNumber: String): Boolean {
        val rooms = parseRooms(roomsString)
        return rooms.contains(roomNumber)
    }
    
    /**
     * Валидирует строку с кабинетами
     * 
     * @param roomsString строка для валидации
     * @return результат валидации с сообщением об ошибке (если есть)
     */
    fun validateRoomsString(roomsString: String): ValidationResult {
        if (roomsString.isBlank()) {
            return ValidationResult(false, "Строка с кабинетами не может быть пустой")
        }
        
        val parts = roomsString.split(",").map { it.trim() }
        
        for (part in parts) {
            if (part.isBlank()) {
                return ValidationResult(false, "Найдена пустая часть в списке кабинетов")
            }
            
            if (part.contains("-")) {
                // Проверяем диапазон
                val rangeParts = part.split("-")
                if (rangeParts.size != 2) {
                    return ValidationResult(false, "Неверный формат диапазона: $part")
                }
                
                val start = rangeParts[0].trim().toIntOrNull()
                val end = rangeParts[1].trim().toIntOrNull()
                
                if (start == null || end == null) {
                    return ValidationResult(false, "Номера кабинетов должны быть числами: $part")
                }
                
                if (start > end) {
                    return ValidationResult(false, "Начальный номер не может быть больше конечного: $part")
                }
                
                if (start < 1 || end > 9999) {
                    return ValidationResult(false, "Номера кабинетов должны быть от 1 до 9999: $part")
                }
            } else {
                // Проверяем отдельный номер
                val roomNumber = part.toIntOrNull()
                if (roomNumber == null) {
                    return ValidationResult(false, "Номер кабинета должен быть числом: $part")
                }
                
                if (roomNumber < 1 || roomNumber > 9999) {
                    return ValidationResult(false, "Номер кабинета должен быть от 1 до 9999: $part")
                }
            }
        }
        
        return ValidationResult(true, "")
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String
)
