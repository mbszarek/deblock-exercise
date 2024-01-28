package org.deblock.exercise.application.service.flights

import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.deblock.exercise.domain.flights.model.Iata
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import kotlin.random.Random

class FlightQueryTest {
    @Test
    fun `should construct flight query`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val returnDate = LocalDate.now()
        val numberOfPassengers = Random.nextInt(1, 5)

        // expect
        assertThatCode { FlightQuery(origin, destination, departureDate, returnDate, numberOfPassengers) }
                .doesNotThrowAnyException()
    }

    @Test
    fun `should not allow the same origin as destination`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("KRK")
        val departureDate = LocalDate.now()
        val returnDate = LocalDate.now()
        val numberOfPassengers = Random.nextInt(1, 5)

        // expect
        assertThatThrownBy { FlightQuery(origin, destination, departureDate, returnDate, numberOfPassengers) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Origin and destination should be different")
    }

    @Test
    fun `should not allow departure date in the past`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now().minusDays(1L)
        val returnDate = LocalDate.now()
        val numberOfPassengers = Random.nextInt(1, 5)

        // expect
        assertThatThrownBy { FlightQuery(origin, destination, departureDate, returnDate, numberOfPassengers) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Departure date should be today or in the future")
    }

    @Test
    fun `should not allow return date before arrival date`() {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val returnDate = LocalDate.now().minusDays(1L)
        val numberOfPassengers = Random.nextInt(1, 5)

        // expect
        assertThatThrownBy { FlightQuery(origin, destination, departureDate, returnDate, numberOfPassengers) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Return date should be the same or later than departure date")
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 5])
    fun `should not allow passengers less than 1 or higher than 4`(numberOfPassengers: Int) {
        // given
        val origin = Iata("KRK")
        val destination = Iata("WAW")
        val departureDate = LocalDate.now()
        val returnDate = LocalDate.now()

        // expect
        assertThatThrownBy { FlightQuery(origin, destination, departureDate, returnDate, numberOfPassengers) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Number of passengers should be between 1 and 4")
    }
}