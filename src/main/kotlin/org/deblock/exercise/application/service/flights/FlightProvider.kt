package org.deblock.exercise.application.service.flights

import org.deblock.exercise.domain.flights.model.Flight
import org.deblock.exercise.domain.flights.model.Iata
import java.time.LocalDate

interface FlightProvider {
    suspend fun getFlights(query: FlightQuery): List<Flight>
}

data class FlightQuery(
        val origin: Iata,
        val destination: Iata,
        val departureDate: LocalDate,
        val returnDate: LocalDate,
        val numberOfPassengers: Int
) {
    init {
        require(origin != destination) { "Origin and destination should be different" }
        require(departureDate >= LocalDate.now()) { "Departure date should be today or in the future" }
        require(returnDate >= departureDate) { "Return date should be the same or later than departure date" }
        require(numberOfPassengers in 1..4) { "Number of passengers should be between 1 and 4" }
    }
}