package org.deblock.exercise.domain.flights.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import wiremock.org.apache.commons.lang3.RandomStringUtils

class SupplierTest {
    @Test
    fun `should allow to create Supplier with random string`() {
        // given
        val rawValue = RandomStringUtils.randomAlphanumeric(5)

        // when
        val supplier = Supplier(rawValue)

        // then
        assertThat(supplier.value)
                .isEqualTo(rawValue)
    }

    @Test
    fun `should not allow to create Supplier with empty string`() {
        // given
        val rawValue = ""

        // expect
        assertThatThrownBy { Supplier(rawValue) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("Supplier should not be empty")
    }
}