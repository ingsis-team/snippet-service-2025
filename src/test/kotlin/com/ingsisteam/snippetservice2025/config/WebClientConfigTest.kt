package com.ingsisteam.snippetservice2025.config

import com.ingsisteam.snippetservice2025.config.WebClientConfig.Companion.REQUEST_ID_HEADER
import com.ingsisteam.snippetservice2025.config.WebClientConfig.Companion.REQUEST_ID_MDC_KEY
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class WebClientConfigTest {

    private lateinit var webClientConfig: WebClientConfig
    private lateinit var mockWebServer: MockWebServer
    private lateinit var webClient: WebClient

    @BeforeEach
    fun setUp() {
        webClientConfig = WebClientConfig()
        mockWebServer = MockWebServer()
        mockWebServer.start() // Start the mock server

        // Build WebClient with the filter and mock server base URL
        webClient = webClientConfig.webClientBuilder()
            .baseUrl(mockWebServer.url("/").toString())
            .build()

        MDC.clear() // Clear MDC before each test
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown() // Shutdown the mock server
        MDC.clear() // Clear MDC after each test
    }

    @Test
    fun `requestIdPropagationFilter should add X-Request-ID header when MDC has requestId`() {
        val testRequestId = "test-request-id"
        MDC.put(REQUEST_ID_MDC_KEY, testRequestId)

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        webClient.get().uri("/test").exchangeToMono { Mono.just(it.statusCode()) }.block()

        val recordedRequest = mockWebServer.takeRequest()
        assertNotNull(recordedRequest.getHeader(REQUEST_ID_HEADER))
        assertEquals(testRequestId, recordedRequest.getHeader(REQUEST_ID_HEADER))
        assertEquals(REQUEST_ID_HEADER, "X-Request-ID")
        assertEquals(REQUEST_ID_MDC_KEY, "requestId")
    }

    @Test
    fun `requestIdPropagationFilter should not add X-Request-ID header when MDC does not have requestId`() {
        MDC.clear() // Ensure MDC is empty

        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        webClient.get().uri("/test").exchangeToMono { Mono.just(it.statusCode()) }.block()

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals(null, recordedRequest.getHeader(REQUEST_ID_HEADER))
        assertEquals(REQUEST_ID_HEADER, "X-Request-ID")
        assertEquals(REQUEST_ID_MDC_KEY, "requestId")
    }
}
