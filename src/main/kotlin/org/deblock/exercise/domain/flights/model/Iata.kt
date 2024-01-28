package org.deblock.exercise.domain.flights.model

@JvmInline
value class Iata(val value: String) {
    init {
        require(value.length == 3) { "IATA code should contain only 3 letters" }
        require(value.all { it.isUpperCase() }) { "IATA code should consist of only uppercase letters" }
    }
}
