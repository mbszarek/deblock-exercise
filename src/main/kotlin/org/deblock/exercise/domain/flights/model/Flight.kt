package org.deblock.exercise.domain.flights.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class Flight(
        val airline: Airline,
        val supplier: Supplier,
        val fare: Money,
        val departureAirportCode: Iata,
        val destinationAirportCode: Iata,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss. SSSXXX")
        val departureDate: LocalDateTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss. SSSXXX")
        val arrivalDate: LocalDateTime
) {
    init {
        require(arrivalDate >= departureDate) { "Arrival date should be equal to or later than departure date" }
    }
}
