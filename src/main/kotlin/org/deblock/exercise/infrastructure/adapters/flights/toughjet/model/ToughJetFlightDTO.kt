package org.deblock.exercise.infrastructure.adapters.flights.toughjet.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.deblock.exercise.domain.flights.model.Iata
import java.math.BigDecimal
import java.time.LocalDateTime

data class ToughJetFlightDTO(
        val carrier: String,
        val basePrice: BigDecimal,
        val tax: BigDecimal,
        val discount: BigDecimal,
        val departureAirportName: Iata,
        val arrivalAirportName: Iata,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        val outboundDateTime: LocalDateTime,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        val inboundDateTime: LocalDateTime
)