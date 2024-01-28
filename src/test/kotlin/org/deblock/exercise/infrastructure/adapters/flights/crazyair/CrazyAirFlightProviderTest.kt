package org.deblock.exercise.infrastructure.adapters.flights.crazyair

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.deblock.exercise.application.service.flights.FlightQuery
import org.deblock.exercise.domain.flights.model.Airline
import org.deblock.exercise.domain.flights.model.Flight
import org.deblock.exercise.domain.flights.model.Iata
import org.deblock.exercise.domain.flights.model.Money.Companion.money
import org.deblock.exercise.domain.flights.model.Supplier
import org.deblock.exercise.infrastructure.adapters.flights.crazyair.CrazyAirFlightProvider
import org.deblock.exercise.infrastructure.configurations.CrazyAirConfiguration.Companion.CIRCUIT_BREAKER_NAME
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class CrazyAirFlightProviderTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val crazyAirServer: WireMockExtension = WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().dynamicPort())
                .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("adapter.flights.crazyair.enabled") { true }
            registry.add("adapter.flights.crazyair.connection-timeout-millis") { 1000 }
            registry.add("adapter.flights.crazyair.base-url", crazyAirServer::baseUrl)
            registry.add("adapter.flights.crazyair.flights-path") { "/flights" }
        }
    }

    @Autowired
    private lateinit var crazyAirFlightProvider: CrazyAirFlightProvider

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Test
    fun `should call CrazyAir API in correct format`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 2
        val flightQuery = FlightQuery(
                origin,
                destination,
                departureDate,
                departureDate.plusDays(1L),
                numberOfPassengers
        )

        crazyAirServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("origin", equalTo(origin.value))
                        .withQueryParam("destination", equalTo(destination.value))
                        .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("[]"))
        )

        // when
        val flights = runBlocking { crazyAirFlightProvider.getFlights(flightQuery) }

        // then
        assertThat(flights)
                .isEmpty()

        // and
        crazyAirServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("origin", equalTo(origin.value))
                .withQueryParam("destination", equalTo(destination.value))
                .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString())))
    }

    @Test
    fun `should correctly parse CrazyAir flights`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 2
        val flightQuery = FlightQuery(
                origin,
                destination,
                departureDate,
                departureDate.plusDays(1L),
                numberOfPassengers
        )

        crazyAirServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("origin", equalTo(origin.value))
                        .withQueryParam("destination", equalTo(destination.value))
                        .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                    [
                                        {
                                            "airline": "Ryanair",
                                            "price": 100.00,
                                            "cabinclass": "E",
                                            "departureAirportCode": "KRK",
                                            "destinationAirportCode": "WAW",
                                            "departureDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T10:15:30",
                                            "arrivalDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T11:15:31"
                                        },
                                        {
                                            "airline": "Wizzair",
                                            "price": 200.00,
                                            "cabinclass": "E",
                                            "departureAirportCode": "KRK",
                                            "destinationAirportCode": "WAW",
                                            "departureDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T20:15:30",
                                            "arrivalDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T21:15:31"
                                        },
                                        {
                                            "airline": "Lufthansa",
                                            "price": 1000.00,
                                            "cabinclass": "B",
                                            "departureAirportCode": "KRK",
                                            "destinationAirportCode": "WAW",
                                            "departureDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T15:30:30",
                                            "arrivalDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T16:30:31"
                                        }
                                    ]
                                """.trimIndent()))
        )

        // when
        val flights = runBlocking { crazyAirFlightProvider.getFlights(flightQuery) }

        // then
        assertThat(flights)
                .containsExactlyInAnyOrder(
                        Flight(
                                Airline("Ryanair"),
                                Supplier("CrazyAir"),
                                money(BigDecimal("100.00")),
                                origin,
                                destination,
                                departureDate.atTime(10, 15, 30),
                                departureDate.atTime(11, 15, 31),
                        ),
                        Flight(
                                Airline("Wizzair"),
                                Supplier("CrazyAir"),
                                money(BigDecimal("200.00")),
                                origin,
                                destination,
                                departureDate.atTime(20, 15, 30),
                                departureDate.atTime(21, 15, 31),
                        ),
                        Flight(
                                Airline("Lufthansa"),
                                Supplier("CrazyAir"),
                                money(BigDecimal("1000.00")),
                                origin,
                                destination,
                                departureDate.atTime(15, 30, 30),
                                departureDate.atTime(16, 30, 31),
                        )
                )

        // and
        crazyAirServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("origin", equalTo(origin.value))
                .withQueryParam("destination", equalTo(destination.value))
                .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString())))
    }

    @Test
    fun `should open circuit breaker`() {
        // given
        circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME, CIRCUIT_BREAKER_NAME).reset()

        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 2
        val flightQuery = FlightQuery(
                origin,
                destination,
                departureDate,
                departureDate.plusDays(1L),
                numberOfPassengers
        )

        crazyAirServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("origin", equalTo(origin.value))
                        .withQueryParam("destination", equalTo(destination.value))
                        .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withStatus(400)))

        // when
        runBlocking { (1..10).forEach { crazyAirFlightProvider.getFlights(flightQuery) } }

        // then
        crazyAirServer.verify(5, getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("origin", equalTo(origin.value))
                .withQueryParam("destination", equalTo(destination.value))
                .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString())))
    }
}