package org.deblock.exercise.infrastructure.adapters.flights.toughjet

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import org.deblock.exercise.infrastructure.adapters.flights.toughjet.config.ToughJetFlightProviderConfig
import org.deblock.exercise.infrastructure.adapters.flights.toughjet.model.ToughJetFlightDTO
import org.deblock.exercise.domain.flights.model.Airline
import org.deblock.exercise.domain.flights.model.Flight
import org.deblock.exercise.application.service.flights.FlightProvider
import org.deblock.exercise.application.service.flights.FlightQuery
import org.deblock.exercise.domain.flights.model.Money.Companion.money
import org.deblock.exercise.domain.flights.model.Supplier
import org.deblock.exercise.infrastructure.configurations.ToughJetConfiguration.Companion.CIRCUIT_BREAKER_NAME
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.netty.http.client.HttpClientRequest
import java.math.BigDecimal.ONE
import java.time.Duration.ofMillis

@Service
class ToughJetFlightProvider(
        webClientBuilder: WebClient.Builder,
        circuitBreakerRegistry: CircuitBreakerRegistry,
        private val config: ToughJetFlightProviderConfig
) : FlightProvider {

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(ToughJetFlightDTO::class.java)
        private val Supplier: Supplier = Supplier("ToughJet")
    }

    private val webClient: WebClient = webClientBuilder
            .baseUrl(config.baseUrl)
            .build()

    private val circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME, CIRCUIT_BREAKER_NAME)

    override suspend fun getFlights(query: FlightQuery): List<Flight> {
        return kotlin.runCatching {
            circuitBreaker.executeSuspendFunction {
                webClient
                        .get()
                        .uri {
                            it
                                    .path(config.flightsPath)
                                    .queryParam("from", "{from}")
                                    .queryParam("to", "{to}")
                                    .queryParam("outboundDate", "{outboundDate}")
                                    .queryParam("inboundDate", "{inboundDate}")
                                    .queryParam("numberOfAdults", "{numberOfAdults}")
                                    .build(query.origin.value, query.destination.value, query.departureDate, query.returnDate, query.numberOfPassengers)
                        }
                        .httpRequest {
                            val reactorRequest: HttpClientRequest = it.getNativeRequest()
                            reactorRequest.responseTimeout(ofMillis(config.connectionTimeoutMillis))
                        }
                        .accept(APPLICATION_JSON)
                        .retrieve()
                        .awaitBody<List<ToughJetFlightDTO>>()
            }
        }.fold({ flights ->
            flights.map { dto ->
                val fare = money(dto.basePrice.multiply(ONE.plus(dto.tax)).multiply(ONE.minus(dto.discount)))

                Flight(
                        Airline(dto.carrier),
                        Supplier,
                        fare,
                        dto.departureAirportName,
                        dto.arrivalAirportName,
                        dto.outboundDateTime,
                        dto.inboundDateTime
                )
            }
        }, { ex ->
            LOG.warn("Cannot fetch ToughJet flights, falling back to empty list", ex)
            return emptyList()
        })
    }
}