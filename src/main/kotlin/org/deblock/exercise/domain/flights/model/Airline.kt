package org.deblock.exercise.domain.flights.model

@JvmInline
value class Airline(val value: String) {
    init {
        require(value.isNotEmpty()) { "Airline should not be empty" }
    }
}