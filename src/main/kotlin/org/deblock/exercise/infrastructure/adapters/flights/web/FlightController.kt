package org.deblock.exercise.infrastructure.adapters.flights.web

import com.fasterxml.jackson.annotation.JsonFormat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.deblock.exercise.application.service.flights.FlightProvider
import org.deblock.exercise.application.service.flights.FlightQuery
import org.deblock.exercise.domain.flights.model.*
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/flights")
class FlightController(private val flightProviders: List<FlightProvider>) {


    @GetMapping("")
    suspend fun findFlights(
            @RequestParam("origin") origin: String,
            @RequestParam("destination") destination: String,
            @RequestParam("departureDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) departureDate: LocalDate,
            @RequestParam("returnDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) returnDate: LocalDate,
            @RequestParam("numberOfPassengers") numberOfPassengers: Int,
    ) = coroutineScope {
        val query = FlightQuery(
                origin = Iata(origin),
                destination = Iata(destination),
                departureDate = departureDate,
                returnDate = returnDate,
                numberOfPassengers = numberOfPassengers
        )

        flightProviders
                .map {
                    async {
                        it.getFlights(query)
                    }
                }
                .awaitAll()
                .flatten()
                .sortedBy { it.fare }
                .map { it.toDTO() }
    }
}

data class FlightDTO(
        val airline: Airline,
        val supplier: Supplier,
        val fare: Money,
        val departureAirportCode: Iata,
        val destinationAirportCode: Iata,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss. SSSXXX")
        val departureDate: LocalDateTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss. SSSXXX")
        val arrivalDate: LocalDateTime
)

fun Flight.toDTO(): FlightDTO = FlightDTO(airline, supplier, fare, departureAirportCode, destinationAirportCode, departureDate, arrivalDate)