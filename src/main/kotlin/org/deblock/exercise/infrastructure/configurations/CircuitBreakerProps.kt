package org.deblock.exercise.infrastructure.configurations

data class CircuitBreakerProps(
        val windowSize: Int = 10,
        val waitInOpenStateMillis: Long = 5000L,
        val failureRateThreshold: Float = 0.50f,
        val minimumNumberOfCalls: Int = 5,
)
