package com.example.bits_helper.data

enum class Status {
    ISSUED,       // роздан (зелёный)
    IN_REFILL,    // на заправке (оранжевый)
    COLLECTED,    // собран (серый)
    RECEIVED,     // получен (синий)
    LOST,         // потерян (красный)
    WRITTEN_OFF   // списан (розовый)
}


