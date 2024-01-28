package org.deblock.exercise.infrastructure.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer
import org.deblock.exercise.infrastructure.adapters.flights.crazyair.config.CrazyAirFlightProviderConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ComponentScan(
        basePackages = ["org.deblock.exercise.infrastructure.adapters.flights.crazyair"]
)
@EnableConfigurationProperties(CrazyAirFlightProviderConfig::class)
@ConditionalOnProperty(
        value = ["adapter.flights.crazyair.enabled"],
        havingValue = "true",
        matchIfMissing = false
)
class CrazyAirConfiguration {
    companion object {
        const val CIRCUIT_BREAKER_NAME: String = "crazyair"
    }

    @Bean
    fun crazyAirCircuitBreakerCustomizer(config: CrazyAirFlightProviderConfig): CircuitBreakerConfigCustomizer {
        val cbConfig = config.circuitBreaker
        return CircuitBreakerConfigCustomizer
                .of(CIRCUIT_BREAKER_NAME) {
                    it
                            .slidingWindowSize(cbConfig.windowSize)
                            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                            .waitDurationInOpenState(Duration.ofMillis(cbConfig.waitInOpenStateMillis))
                            .minimumNumberOfCalls(cbConfig.minimumNumberOfCalls)
                            .failureRateThreshold(cbConfig.failureRateThreshold)
                            .build()
                }
    }

}