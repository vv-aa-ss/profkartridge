package com.example.bits_helper.data

enum class Status {
    ISSUED,       // роздан (зелёный)
    IN_REFILL,    // на заправке (оранжевый)
    COLLECTED,    // собран (серый)
    RECEIVED,     // получен (синий)
    LOST,         // потерян (красный)
    WRITTEN_OFF   // списан (розовый)
}

fun Status.getRussianName(): String {
    return when (this) {
        Status.ISSUED -> "Роздан"
        Status.IN_REFILL -> "На заправке"
        Status.COLLECTED -> "Собран"
        Status.RECEIVED -> "Принят"
        Status.LOST -> "Потерян"
        Status.WRITTEN_OFF -> "Списан"
    }
}


