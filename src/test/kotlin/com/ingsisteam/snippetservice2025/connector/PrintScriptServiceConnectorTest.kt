package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsisteam.snippetservice2025.exception.PrintScriptServiceException
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.model.dto.external.SCAOutput
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResult
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.assertFalse

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
    fun `validateSnippet should return isValid true for valid snippet`() {
        val validationResult = ValidationResult(isValid = true, rule = "dummy_rule", line = 1, column = 1)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"is_valid\": true, \"rule\": \"dummy_rule\", \"line\": 1, \"column\": 1 }"),
        )

        val response = connector.validateSnippet("var x = 1;", "PRINTSCRIPT", "1.0")

        assertEquals(true, response.isValid)
        assertNull(response.errors)
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
        assertEquals("SERVICE_ERROR", response.errors?.get(0)?.rule)
        assertEquals("El servicio de validación no devolvió una respuesta", response.errors?.get(0)?.message)
    }

    @Test
    fun `validateSnippet should handle HTTP error with parsable message`() {
        val errorMessage = "{\"message\": \"Invalid input format\"}"
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setHeader("Content-Type", "application/json")
                .setBody(errorMessage),
        )

        val response = connector.validateSnippet("invalid", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("VALIDATION_SERVICE_ERROR", response.errors?.get(0)?.rule)
        assertEquals("Invalid input format", response.errors?.get(0)?.message)
    }

    @Test
    fun `validateSnippet should handle HTTP error without parsable message`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setHeader("Content-Type", "text/plain")
                .setBody("Internal server error"),
        )

        val response = connector.validateSnippet("invalid", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("VALIDATION_SERVICE_ERROR", response.errors?.get(0)?.rule)
        assertEquals("Error interno del servicio de validación", response.errors?.get(0)?.message)
    }

    @Test
    fun `validateSnippet should handle generic exception`() {
        mockWebServer.shutdown()

        val response = connector.validateSnippet("var x = 1;", "PRINTSCRIPT", "1.0")

        assertEquals(false, response.isValid)
        assertEquals(1, response.errors?.size)
        assertEquals("CONNECTION_ERROR", response.errors?.get(0)?.rule)
    }

    @Test
    fun `validateSnippet should log warning for blank content`() {
        val validationResult = ValidationResult(isValid = true, rule = "dummy_rule", line = 1, column = 1)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(validationResult)),
        )

        val response = connector.validateSnippet("   ", "PRINTSCRIPT", "1.0") // Blank content

        assertFalse(response.isValid)
        assertNotNull(response.errors)
        val request = mockWebServer.takeRequest()
        assertEquals("/validate", request.path)
        assertEquals("POST", request.method)
    }

    @Test
    fun `formatSnippet should throw PrintScriptServiceException on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.formatSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertEquals("El servicio no devolvió una respuesta", exception.message)
        assertEquals("formatear snippet", exception.operation)
    }

    @Test
    fun `formatSnippet should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.formatSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("formatear snippet", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `formatSnippet should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.formatSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("formatear snippet", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `formatSnippet should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.formatSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertFalse(exception.message!!.contains("Unexpected error while formatting snippet: Unrecognized token 'invalid'"))
        assertEquals("formatear snippet", exception.operation)
        assertNotNull(exception.statusCode)
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
    fun `lintSnippet should return list of SCAOutput on success`() {
        val scaOutputList = listOf(SCAOutput("rule1", 1, 1, "msg1"))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(scaOutputList)),
        )

        val result = connector.lintSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")

        assertEquals(1, result.size)
        assertEquals("rule1", result[0].rule)
        val request = mockWebServer.takeRequest()
        assertEquals("/lint", request.path)
        assertEquals("POST", request.method)
        assertNotNull(request.body.readUtf8())
    }

    @Test
    fun `lintSnippet should return empty list on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.lintSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `lintSnippet should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.lintSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("analizar snippet (lint)", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `lintSnippet should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.lintSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("analizar snippet (lint)", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `lintSnippet should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.lintSnippet("1", "corr1", "PRINTSCRIPT", "1.0", "content", "user1")
        }

        assertNotNull(exception.message)
        assertEquals("analizar snippet (lint)", exception.operation)
        assertNotNull(exception.statusCode)
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

    @Test
    fun `getFormattingRules should return list of rules on success`() {
        val rules = listOf(Rule("rule1", true))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(rules)),
        )

        val result = connector.getFormattingRules("user1", "corr1")

        assertEquals(1, result.size)
        assertEquals("rule1", result[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/rules/format/user1", request.path)
        assertEquals("GET", request.method)
        assertEquals("corr1", request.headers["Correlation-id"])
        assertEquals("user1", request.headers["X-User-Id"])
    }

    @Test
    fun `getFormattingRules should return empty list on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.getFormattingRules("user1", "corr1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getFormattingRules should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getFormattingRules("user1", "corr1")
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("obtener reglas de formateo", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `getFormattingRules should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getFormattingRules("user1", "corr1")
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("obtener reglas de formateo", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `getFormattingRules should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getFormattingRules("user1", "corr1")
        }

        assertNotNull(exception.message)
        assertEquals("obtener reglas de formateo", exception.operation)
        assertNotNull(exception.statusCode)
    }

    @Test
    fun `getLintingRules should return list of rules on success`() {
        val rules = listOf(Rule("rule2", false))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(rules)),
        )

        val result = connector.getLintingRules("user1", "corr1")

        assertEquals(1, result.size)
        assertEquals("rule2", result[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/rules/lint/user1", request.path)
        assertEquals("GET", request.method)
        assertEquals("corr1", request.headers["Correlation-id"])
        assertEquals("user1", request.headers["X-User-Id"])
    }

    @Test
    fun `getLintingRules should return empty list on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.getLintingRules("user1", "corr1")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getLintingRules should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getLintingRules("user1", "corr1")
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("obtener reglas de linting", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `getLintingRules should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getLintingRules("user1", "corr1")
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("obtener reglas de linting", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `getLintingRules should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.getLintingRules("user1", "corr1")
        }

        assertNotNull(exception.message)
        assertEquals("obtener reglas de linting", exception.operation)
        assertNotNull(exception.statusCode)
    }

    @Test
    fun `saveFormattingRules should return list of rules on success`() {
        val rules = listOf(Rule("rule3", true))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(rules)),
        )

        val result = connector.saveFormattingRules("user1", "corr1", rules)

        assertEquals(1, result.size)
        assertEquals("rule3", result[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/rules/format/user1", request.path)
        assertEquals("POST", request.method)
        assertEquals("corr1", request.headers["Correlation-id"])
        assertEquals("user1", request.headers["X-User-Id"])
        assertEquals(objectMapper.writeValueAsString(rules), request.body.readUtf8())
    }

    @Test
    fun `saveFormattingRules should return empty list on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.saveFormattingRules("user1", "corr1", emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `saveFormattingRules should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRules("user1", "corr1", emptyList())
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `saveFormattingRules should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRules("user1", "corr1", emptyList())
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `saveFormattingRules should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRules("user1", "corr1", emptyList())
        }

        assertNotNull(exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertNotNull(exception.statusCode)
    }

    @Test
    fun `saveLintingRules should return list of rules on success`() {
        val rules = listOf(Rule("rule4", false))
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(rules)),
        )

        val result = connector.saveLintingRules("user1", "corr1", rules)

        assertEquals(1, result.size)
        assertEquals("rule4", result[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/rules/lint/user1", request.path)
        assertEquals("POST", request.method)
        assertEquals("corr1", request.headers["Correlation-id"])
        assertEquals("user1", request.headers["X-User-Id"])
        assertEquals(objectMapper.writeValueAsString(rules), request.body.readUtf8())
    }

    @Test
    fun `saveLintingRules should return empty list on null response`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.saveLintingRules("user1", "corr1", emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun `saveLintingRules should throw PrintScriptServiceException on HTTP error`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveLintingRules("user1", "corr1", emptyList())
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("guardar reglas de linting", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `saveLintingRules should throw PrintScriptServiceException on connection error`() {
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveLintingRules("user1", "corr1", emptyList())
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("guardar reglas de linting", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `saveLintingRules should throw PrintScriptServiceException on generic exception`() {
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveLintingRules("user1", "corr1", emptyList())
        }

        assertNotNull(exception.message)
        assertEquals("guardar reglas de linting", exception.operation)
        assertNotNull(exception.statusCode)
    }

    @Test
    fun `saveFormattingRulesFile should return list of rules on success`() {
        val rules = listOf(Rule("rule5", true))
        val dto = FormatterRulesFileDTO(true, false, true, 2, 2, 6)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(rules)),
        )

        val result = connector.saveFormattingRulesFile("user1", "corr1", dto)

        assertEquals(1, result.size)
        assertEquals("rule5", result[0].name)
        val request = mockWebServer.takeRequest()
        assertEquals("/rules/format/user1", request.path)
        assertEquals("POST", request.method)
        assertEquals("corr1", request.headers["Correlation-id"])
        assertEquals("user1", request.headers["X-User-Id"])
        assertEquals(objectMapper.writeValueAsString(dto), request.body.readUtf8())
    }

    @Test
    fun `saveFormattingRulesFile should return empty list on null response`() {
        val dto = FormatterRulesFileDTO(true, false, true, 2, 2, 6)
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("null"),
        )

        val result = connector.saveFormattingRulesFile("user1", "corr1", dto)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `saveFormattingRulesFile should throw PrintScriptServiceException on HTTP error`() {
        val dto = FormatterRulesFileDTO(true, false, true, 2, 2, 6)
        mockWebServer.enqueue(MockResponse().setResponseCode(400).setBody("Bad Request"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRulesFile("user1", "corr1", dto)
        }

        assertEquals("Error HTTP 400: Bad Request", exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun `saveFormattingRulesFile should throw PrintScriptServiceException on connection error`() {
        val dto = FormatterRulesFileDTO(true, false, true, 2, 2, 6)
        mockWebServer.shutdown()

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRulesFile("user1", "corr1", dto)
        }

        assertEquals("No se pudo conectar al servicio de PrintScript", exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertNull(exception.statusCode)
    }

    @Test
    fun `saveFormattingRulesFile should throw PrintScriptServiceException on generic exception`() {
        val dto = FormatterRulesFileDTO(true, false, true, 2, 2, 6)
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("invalid json"))

        val exception = assertThrows(PrintScriptServiceException::class.java) {
            connector.saveFormattingRulesFile("user1", "corr1", dto)
        }

        assertNotNull(exception.message)
        assertEquals("guardar reglas de formateo", exception.operation)
        assertNotNull(exception.statusCode)
    }
}
