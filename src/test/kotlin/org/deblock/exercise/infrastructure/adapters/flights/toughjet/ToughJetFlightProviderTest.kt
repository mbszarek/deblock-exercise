package org.deblock.exercise.infrastructure.adapters.flights.toughjet

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
import org.deblock.exercise.infrastructure.adapters.flights.toughjet.ToughJetFlightProvider
import org.deblock.exercise.infrastructure.configurations.ToughJetConfiguration.Companion.CIRCUIT_BREAKER_NAME
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
class ToughJetFlightProviderTest {

    companion object {
        @JvmStatic
        @RegisterExtension
        val toughJetServer: WireMockExtension = WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().dynamicPort())
                .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("adapter.flights.toughjet.enabled") { true }
            registry.add("adapter.flights.toughjet.connection-timeout-millis") { 1000 }
            registry.add("adapter.flights.toughjet.base-url", toughJetServer::baseUrl)
            registry.add("adapter.flights.toughjet.flights-path") { "/flights" }
        }
    }

    @Autowired
    private lateinit var toughJetFlightProvider: ToughJetFlightProvider

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Test
    fun `should call ToughJet API in correct format`() {
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

        toughJetServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("from", equalTo(origin.value))
                        .withQueryParam("to", equalTo(destination.value))
                        .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("[]"))
        )

        // when
        val flights = runBlocking { toughJetFlightProvider.getFlights(flightQuery) }

        // then
        assertThat(flights)
                .isEmpty()

        // and
        toughJetServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("from", equalTo(origin.value))
                .withQueryParam("to", equalTo(destination.value))
                .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString())))
    }

    @Test
    fun `should correctly parse ToughJet flights`() {
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

        toughJetServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("from", equalTo(origin.value))
                        .withQueryParam("to", equalTo(destination.value))
                        .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("""
                                    [
                                        {
                                            "carrier": "Ryanair",
                                            "basePrice": 100.00,
                                            "tax": 0.23,
                                            "discount": 0.2,
                                            "departureAirportName": "KRK",
                                            "arrivalAirportName": "WAW",
                                            "outboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T10:15:30Z",
                                            "inboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T10:15:31Z"
                                        },
                                        {
                                            "carrier": "Wizzair",
                                            "basePrice": 200.00,
                                            "tax": 0.23,
                                            "discount": 0.15,
                                            "departureAirportName": "KRK",
                                            "arrivalAirportName": "WAW",
                                            "outboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T20:15:30Z",
                                            "inboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T20:15:31Z"
                                        }
                                    ]
                                """.trimIndent()))
        )

        // when
        val flights = runBlocking { toughJetFlightProvider.getFlights(flightQuery) }

        // then
        assertThat(flights)
                .containsExactlyInAnyOrder(
                        Flight(
                                Airline("Ryanair"),
                                Supplier("ToughJet"),
                                money(BigDecimal("98.40")),
                                origin,
                                destination,
                                departureDate.atTime(10, 15, 30),
                                departureDate.atTime(10, 15, 31),
                        ),
                        Flight(
                                Airline("Wizzair"),
                                Supplier("ToughJet"),
                                money(BigDecimal("209.10")),
                                origin,
                                destination,
                                departureDate.atTime(20, 15, 30),
                                departureDate.atTime(20, 15, 31),
                        )
                )

        // and
        toughJetServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("from", equalTo(origin.value))
                .withQueryParam("to", equalTo(destination.value))
                .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString())))
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

        toughJetServer.stubFor(
                get(urlPathEqualTo("/flights"))
                        .withQueryParam("from", equalTo(origin.value))
                        .withQueryParam("to", equalTo(destination.value))
                        .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                        .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString()))
                        .willReturn(aResponse()
                                .withStatus(400)))

        // when
        runBlocking { (1..10).forEach { toughJetFlightProvider.getFlights(flightQuery) } }

        // then
        toughJetServer.verify(5, getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("from", equalTo(origin.value))
                .withQueryParam("to", equalTo(destination.value))
                .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString())))
    }
}