package com.ingsisteam.snippetservice2025.config

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

/**
 * Configuración de WebClient con un filtro para propagar el Request ID.
 * El filtro agrega el header X-Request-ID a todas las peticiones salientes tomando el valor desde el MDC.
 */
@Configuration
class WebClientConfig {

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    @Bean
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
            .filter(requestIdPropagationFilter())
    }

    /**
     * Filtro para propagar el Request ID a las llamadas HTTP salientes.
     * Agrega el header X-Request-ID desde el MDC a todas las peticiones.
     */
    private fun requestIdPropagationFilter(): ExchangeFilterFunction {
        return ExchangeFilterFunction.ofRequestProcessor { clientRequest ->
            val requestId = MDC.get(REQUEST_ID_MDC_KEY)

            if (requestId != null) {
                val modifiedRequest = ClientRequest.from(clientRequest)
                    .header(REQUEST_ID_HEADER, requestId)
                    .build()
                Mono.just(modifiedRequest)
            } else {
                Mono.just(clientRequest)
            }
        }
    }
}
