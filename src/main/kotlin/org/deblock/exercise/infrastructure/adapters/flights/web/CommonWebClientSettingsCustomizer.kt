package org.deblock.exercise.infrastructure.adapters.flights.web

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.netty.channel.ChannelOption
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Component
class CommonWebClientSettingsCustomizer(
        @Value("\${web.connection-timeout-millis:1000}") val connectionTimeoutMillis: Int
) : WebClientCustomizer {
    override fun customize(webClientBuilder: WebClient.Builder) {
        val httpClient: HttpClient = HttpClient
                .create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)

        webClientBuilder
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs {
                                    it.defaultCodecs().jackson2JsonDecoder(
                                            Jackson2JsonDecoder(
                                                    ObjectMapper().registerModule(KotlinModule.Builder().build()),
                                                    MediaType.APPLICATION_JSON
                                            )
                                    )
                                }
                                .build()
                )
    }
}