package org.deblock.exercise.infrastructure.adapters.flights.crazyair.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.deblock.exercise.domain.flights.model.Airline
import org.deblock.exercise.domain.flights.model.Iata
import org.deblock.exercise.domain.flights.model.Money
import java.time.LocalDateTime

data class CrazyAirFlightDTO(
        val airline: Airline,
        val price: Money,
        val cabinclass: String,
        val departureAirportCode: Iata,
        val destinationAirportCode: Iata,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        val departureDate: LocalDateTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        val arrivalDate: LocalDateTime
)