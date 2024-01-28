package org.deblock.exercise.domain.flights.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import wiremock.org.apache.commons.lang3.RandomStringUtils


class AirlineTest {
    @Test
    fun `should allow to create Airline with random string`() {
        // given
        val rawValue = RandomStringUtils.randomAlphanumeric(5)

        // when
        val airline = Airline(rawValue)

        // then
        assertThat(airline.value)
                .isEqualTo(rawValue)
    }

    @Test
    fun `should not allow to create Airline with empty string`() {
        // given
        val rawValue = ""

        // expect
        assertThatThrownBy { Airline(rawValue) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Airline should not be empty")
    }
}