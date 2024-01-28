package org.deblock.exercise.domain.flights.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.deblock.exercise.domain.flights.model.Money.Companion.money
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal

class MoneyTest {
    @ParameterizedTest
    @ValueSource(strings = ["23.35", "23.350", "23.3500", "23.35000"])
    fun `should create instance with precision equal to 2`(rawValue: String) {
        // when
        val money = money(BigDecimal(rawValue))

        // then
        assertThat(money.value)
                .isEqualTo(BigDecimal("23.35"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["23.3450", "23.3540"])
    fun `should round numbers half up`(rawValue: String) {
        // when
        val money = money(BigDecimal(rawValue))

        // then
        assertThat(money.value)
                .isEqualTo(BigDecimal("23.35"))
    }

    @Test
    fun `should add two decimal points even to integers`() {
        // given
        val rawValue = 1

        // when
        val money = money(BigDecimal(rawValue))

        // then
        assertThat(money.value)
                .isEqualTo(BigDecimal("1.00"))
    }

    @Test
    fun `should allow to create instance with zero`() {
        // given
        val rawValue = 0

        // when
        val money = money(BigDecimal(rawValue))

        // then
        assertThat(money.value)
                .isEqualTo(BigDecimal("0.00"))
    }

    @Test
    fun `should not allow to create instance with negative amount`() {
        // given
        val rawValue = -5

        // expect
        assertThatThrownBy { money(BigDecimal(rawValue)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Money should be greater than or equal to zero")
    }
}