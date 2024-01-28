package org.deblock.exercise.domain.flights.model

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IataTest {
    @Test
    fun `should construct Iata from 3 uppercase letters`() {
        // given
        val rawValue = "KRK"

        // expect
        val iata = Iata(rawValue)

        // then
        assertThat(iata.value).isEqualTo(rawValue)
    }

    @ParameterizedTest
    @ValueSource(strings = ["KR", "KRKR"])
    fun `should not allow uppercase strings that are not of 3 letters`(rawValue: String) {
        // expect
        assertThatThrownBy { Iata(rawValue) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("IATA code should contain only 3 letters")
    }

    @ParameterizedTest
    @ValueSource(strings = ["Krk", "krk", "KrK"])
    fun `should not allow non-uppercase strings that are of 3 letters`(rawValue: String) {
        // expect
        assertThatThrownBy { Iata(rawValue) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("IATA code should consist of only uppercase letters")
    }
}