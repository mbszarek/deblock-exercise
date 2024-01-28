package org.deblock.exercise.infrastructure.configurations

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED
import io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigCustomizer
import org.deblock.exercise.infrastructure.adapters.flights.toughjet.config.ToughJetFlightProviderConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import java.time.Duration.ofMillis

@Configuration
@ComponentScan(
        basePackages = ["org.deblock.exercise.infrastructure.adapters.flights.toughjet"]
)
@EnableConfigurationProperties(ToughJetFlightProviderConfig::class)
@ConditionalOnProperty(
        value = ["adapter.flights.toughjet.enabled"],
        havingValue = "true",
        matchIfMissing = false
)
class ToughJetConfiguration {

    companion object {
        const val CIRCUIT_BREAKER_NAME: String = "toughjet"
    }

    @Bean
    fun toughJetCircuitBreakerCustomizer(config: ToughJetFlightProviderConfig): CircuitBreakerConfigCustomizer {
        val cbConfig = config.circuitBreaker
        return CircuitBreakerConfigCustomizer
                .of(CIRCUIT_BREAKER_NAME) {
                    it
                            .slidingWindowSize(cbConfig.windowSize)
                            .slidingWindowType(COUNT_BASED)
                            .waitDurationInOpenState(ofMillis(cbConfig.waitInOpenStateMillis))
                            .minimumNumberOfCalls(cbConfig.minimumNumberOfCalls)
                            .failureRateThreshold(cbConfig.failureRateThreshold)
                            .build()
                }
    }
}