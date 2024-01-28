package org.deblock.exercise.infrastructure.adapters.flights.web

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder.allRequests
import org.deblock.exercise.domain.flights.model.Iata
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FlightControllerTest {
    companion object {
        @JvmStatic
        @RegisterExtension
        val toughJetServer: WireMockExtension = WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().dynamicPort())
                .build()

        @JvmStatic
        @RegisterExtension
        val crazyAirServer: WireMockExtension = WireMockExtension.newInstance()
                .options(WireMockConfiguration.wireMockConfig().dynamicPort())
                .build()

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("adapter.flights.toughjet.enabled") { true }
            registry.add("adapter.flights.toughjet.connection-timeout-millis") { 1000 }
            registry.add("adapter.flights.toughjet.base-url", toughJetServer::baseUrl)
            registry.add("adapter.flights.toughjet.flights-path") { "/flights" }

            registry.add("adapter.flights.crazyair.enabled") { true }
            registry.add("adapter.flights.crazyair.connection-timeout-millis") { 1000 }
            registry.add("adapter.flights.crazyair.base-url", crazyAirServer::baseUrl)
            registry.add("adapter.flights.crazyair.flights-path") { "/flights" }
        }
    }

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `should call, parse CrazyAir and ToughJet responses and order by fare`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 2

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
                                            "basePrice": 200.00,
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
                                            "tax": 0.15,
                                            "discount": 0.1,
                                            "departureAirportName": "KRK",
                                            "arrivalAirportName": "WAW",
                                            "outboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T20:15:30Z",
                                            "inboundDateTime": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T20:15:31Z"
                                        }
                                    ]
                                """.trimIndent()))
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
                                            "airline": "Lufthansa",
                                            "price": 1000.00,
                                            "cabinclass": "B",
                                            "departureAirportCode": "KRK",
                                            "destinationAirportCode": "WAW",
                                            "departureDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T15:30:30",
                                            "arrivalDate": "${departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}T16:30:31"
                                        },
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
                                        }
                                        
                                    ]
                                """.trimIndent()))
        )

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", destination.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .json("""
                    [
                        {
                           "airline":"Ryanair",
                           "supplier":"CrazyAir",
                           "fare":100.00,
                           "departureAirportCode":"KRK",
                           "destinationAirportCode":"WAW",
                           "departureDate":"2024-01-28T10:15:30",
                           "arrivalDate":"2024-01-28T11:15:31"
                        },
                        {
                           "airline":"Ryanair",
                           "supplier":"ToughJet",
                           "fare":196.80,
                           "departureAirportCode":"KRK",
                           "destinationAirportCode":"WAW",
                           "departureDate":"2024-01-28T10:15:30",
                           "arrivalDate":"2024-01-28T10:15:31"
                        },
                        {
                           "airline":"Wizzair",
                           "supplier":"CrazyAir",
                           "fare":200.00,
                           "departureAirportCode":"KRK",
                           "destinationAirportCode":"WAW",
                           "departureDate":"2024-01-28T20:15:30",
                           "arrivalDate":"2024-01-28T21:15:31"
                        },
                        {
                           "airline":"Wizzair",
                           "supplier":"ToughJet",
                           "fare":207.00,
                           "departureAirportCode":"KRK",
                           "destinationAirportCode":"WAW",
                           "departureDate":"2024-01-28T20:15:30",
                           "arrivalDate":"2024-01-28T20:15:31"
                        },
                        {
                           "airline":"Lufthansa",
                           "supplier":"CrazyAir",
                           "fare":1000.00,
                           "departureAirportCode":"KRK",
                           "destinationAirportCode":"WAW",
                           "departureDate":"2024-01-28T15:30:30",
                           "arrivalDate":"2024-01-28T16:30:31"
                        }
                    ]     
                """.trimIndent())

        // and
        toughJetServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("from", equalTo(origin.value))
                .withQueryParam("to", equalTo(destination.value))
                .withQueryParam("outboundDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("inboundDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("numberOfAdults", equalTo(numberOfPassengers.toString())))

        // and
        crazyAirServer.verify(getRequestedFor(urlPathEqualTo("/flights"))
                .withQueryParam("origin", equalTo(origin.value))
                .withQueryParam("destination", equalTo(destination.value))
                .withQueryParam("departureDate", equalTo(departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("returnDate", equalTo(arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)))
                .withQueryParam("passengerCount", equalTo(numberOfPassengers.toString())))
    }

    @Test
    fun `should not allow number of passengers higher than 4`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 5

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", destination.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""
                    {
                      "message": "Number of passengers should be between 1 and 4"
                    }
                """.trimIndent())

        // and
        crazyAirServer.verify(0, allRequests())

        // and
        toughJetServer.verify(0, allRequests())
    }

    @Test
    fun `should not allow number of passengers less than 1`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.plusDays(1L)
        val numberOfPassengers = 0

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", destination.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""
                    {
                      "message": "Number of passengers should be between 1 and 4"
                    }
                """.trimIndent())

        // and
        crazyAirServer.verify(0, allRequests())

        // and
        toughJetServer.verify(0, allRequests())
    }

    @Test
    fun `should not allow departure date in the past`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val arrivalDate = LocalDate.now()
        val departureDate = arrivalDate.minusDays(1L)
        val numberOfPassengers = 1

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", destination.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""
                    {
                      "message": "Departure date should be today or in the future"
                    }
                """.trimIndent())

        // and
        crazyAirServer.verify(0, allRequests())

        // and
        toughJetServer.verify(0, allRequests())
    }

    @Test
    fun `should not allow return date before departure date`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.minusDays(1L)
        val numberOfPassengers = 1

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", destination.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""
                    {
                      "message": "Return date should be the same or later than departure date"
                    }
                """.trimIndent())

        // and
        crazyAirServer.verify(0, allRequests())

        // and
        toughJetServer.verify(0, allRequests())
    }

    @Test
    fun `should not allow the same origin and destination`() {
        // given
        val origin = Iata("KRK")
        val departureDate = LocalDate.now()
        val arrivalDate = departureDate.minusDays(1L)
        val numberOfPassengers = 1

        // expect
        webClient.get().uri {
            it.path("/flights")
                    .queryParam("origin", origin.value)
                    .queryParam("destination", origin.value)
                    .queryParam("departureDate", departureDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("returnDate", arrivalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    .queryParam("numberOfPassengers", numberOfPassengers.toString())
                    .build()
        }
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest
                .expectBody()
                .json("""
                    {
                      "message": "Origin and destination should be different"
                    }
                """.trimIndent())

        // and
        crazyAirServer.verify(0, allRequests())

        // and
        toughJetServer.verify(0, allRequests())
    }
}