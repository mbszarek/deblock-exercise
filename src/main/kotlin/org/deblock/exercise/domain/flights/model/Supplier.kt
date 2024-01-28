package org.deblock.exercise.domain.flights.model

@JvmInline
value class Supplier(val value: String) {
    init {
        require(value.isNotEmpty()) { "Supplier should not be empty" }
    }
}