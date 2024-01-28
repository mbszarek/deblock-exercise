package org.deblock.exercise.infrastructure.adapters.flights.crazyair

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import org.deblock.exercise.application.service.flights.FlightProvider
import org.deblock.exercise.application.service.flights.FlightQuery
import org.deblock.exercise.domain.flights.model.Flight
import org.deblock.exercise.domain.flights.model.Supplier
import org.deblock.exercise.infrastructure.adapters.flights.crazyair.config.CrazyAirFlightProviderConfig
import org.deblock.exercise.infrastructure.adapters.flights.crazyair.model.CrazyAirFlightDTO
import org.deblock.exercise.infrastructure.configurations.CrazyAirConfiguration.Companion.CIRCUIT_BREAKER_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClientRequest
import java.time.Duration.ofMillis

@Service
class CrazyAirFlightProvider(
        webClientBuilder: WebClient.Builder,
        circuitBreakerRegistry: CircuitBreakerRegistry,
        private val config: CrazyAirFlightProviderConfig
) : FlightProvider {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(CrazyAirFlightProvider::class.java)
        private val Supplier: Supplier = Supplier("CrazyAir")
    }

    private val webClient: WebClient = webClientBuilder
            .clone()
            .baseUrl(config.baseUrl)
            .build()

    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME, CIRCUIT_BREAKER_NAME)

    override suspend fun getFlights(query: FlightQuery): List<Flight> {
        return runCatching {
            circuitBreaker.executeSuspendFunction {
                webClient
                        .get()
                        .uri {
                            it
                                    .path(config.flightsPath)
                                    .queryParam("origin", "{origin}")
                                    .queryParam("destination", "{destination}")
                                    .queryParam("departureDate", "{departureDate}")
                                    .queryParam("returnDate", "{returnDate}")
                                    .queryParam("passengerCount", "{passengerCount}")
                                    .build(query.origin.value, query.destination.value, query.departureDate, query.returnDate, query.numberOfPassengers)
                        }
                        .httpRequest {
                            val reactorRequest: HttpClientRequest = it.getNativeRequest()
                            reactorRequest.responseTimeout(ofMillis(config.connectionTimeoutMillis))
                        }
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .awaitBody<List<CrazyAirFlightDTO>>()
            }
        }.fold({ flights ->
            flights.map { dto ->
                Flight(
                        dto.airline,
                        Supplier,
                        dto.price,
                        dto.departureAirportCode,
                        dto.destinationAirportCode,
                        dto.departureDate,
                        dto.arrivalDate
                )
            }
        }, { ex ->
            LOG.warn("Cannot fetch CrazyAir flights, falling back to empty list", ex)
            return emptyList()
        })
    }
}