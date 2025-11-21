package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResult
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class PrintScriptServiceConnectorTest {

    private lateinit var connector: PrintScriptServiceConnector
    private lateinit var mockWebServer: MockWebServer
    private lateinit var webClientBuilder: WebClient.Builder
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        val baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        webClientBuilder = WebClient.builder().baseUrl(baseUrl) // Actual WebClient.Builder
        connector = PrintScriptServiceConnector(webClientBuilder, baseUrl)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `validateSnippet should return isValid false for invalid snippet`() {
        val validationResult = ValidationResult(isValid = false, rule = "SYNTAX_ERROR", line = 1, column = 5)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(validationResult)),
        )

        val response = connector.validateSnippet("var x = 1", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("SYNTAX_ERROR", response.errors?.get(0)?.rule)
        assertEquals(1, response.errors?.get(0)?.line)
        assertEquals(5, response.errors?.get(0)?.column)
    }

    @Test
    fun `validateSnippet should handle connection error gracefully`() {
        mockWebServer.shutdown() // Simulate connection error

        val response = connector.validateSnippet("var x = 1;", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("CONNECTION_ERROR", response.errors?.get(0)?.rule)
        assertNotNull(response.errors?.get(0)?.message)
    }

    @Test
    fun `validateSnippet should handle null response from service`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"), // Service returns null body
        )

        val response = connector.validateSnippet("var x = 1;", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("UNKNOWN_ERROR", response.errors?.get(0)?.rule)
        assertEquals("Validation service returned null", response.errors?.get(0)?.message)
    }

    @Test
    fun `triggerAutomaticFormatting should call service endpoint`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        connector.triggerAutomaticFormatting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/format/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
        assertNotNull(recordedRequest.body.readUtf8())
        // No specific mockk verify on connector as it's a direct call, verify is done by recordedRequest
    }

    @Test
    fun `triggerAutomaticFormatting should not throw exception on service error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500)) // Simulate service error

        // Should not throw
        connector.triggerAutomaticFormatting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/format/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
    }

    @Test
    fun `triggerAutomaticLinting should call service endpoint`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        connector.triggerAutomaticLinting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/lint/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
        assertNotNull(recordedRequest.body.readUtf8())
    }

    @Test
    fun `triggerAutomaticLinting should not throw exception on service error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        connector.triggerAutomaticLinting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/lint/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
    }

    @Test
    fun `triggerAutomaticTesting should call service endpoint`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200))

        connector.triggerAutomaticTesting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/test/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
        assertNotNull(recordedRequest.body.readUtf8())
    }

    @Test
    fun `triggerAutomaticTesting should not throw exception on service error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(500))

        connector.triggerAutomaticTesting("123", "user1", "content")

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("/redis/test/snippet", recordedRequest.path)
        assertEquals("PUT", recordedRequest.method)
    }
}
