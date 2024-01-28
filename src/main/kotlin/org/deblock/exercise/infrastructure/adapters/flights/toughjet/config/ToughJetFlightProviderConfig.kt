package org.deblock.exercise.infrastructure.adapters.flights.toughjet.config

import org.deblock.exercise.infrastructure.configurations.CircuitBreakerProps
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "adapter.flights.toughjet")
data class ToughJetFlightProviderConfig(
        val connectionTimeoutMillis: Long,
        val baseUrl: String,
        val flightsPath: String,
        val circuitBreaker: CircuitBreakerProps = CircuitBreakerProps()
)