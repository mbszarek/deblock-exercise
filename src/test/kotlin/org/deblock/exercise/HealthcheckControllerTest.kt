package org.deblock.exercise

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthcheckControllerTest {

    @Autowired
    private lateinit var webClient: WebTestClient

    @Test
    fun `should return 200 OK`() {
        // expect
        webClient.get()
                .uri { it.path("/actuator").path("/health").build() }
                .exchange()
                .expectStatus()
                .isOk
    }
}