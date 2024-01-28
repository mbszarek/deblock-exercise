package org.deblock.exercise.domain.flights.model

import com.fasterxml.jackson.annotation.JsonCreator
import java.math.BigDecimal
import java.math.RoundingMode

@JvmInline
value class Money private constructor(val value: BigDecimal) : Comparable<Money> {
    init {
        require(value >= BigDecimal.ZERO) { "Money should be greater than or equal to zero" }
    }

    companion object {
        @JsonCreator
        @JvmStatic
        fun money(value: BigDecimal): Money = Money(value.setScale(2, RoundingMode.HALF_UP))
    }

    override fun compareTo(other: Money): Int = value.compareTo(other.value)
}