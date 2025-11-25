package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationError
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResponse
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PrintScriptServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${printscript.url}") private val printScriptUrl: String,
) {
    private val logger = LoggerFactory.getLogger(PrintScriptServiceConnector::class.java)

    private val client: WebClient by lazy {
        webClient.baseUrl(printScriptUrl).build()
    }

    private val objectMapper = ObjectMapper()

    fun validateSnippet(content: String, language: String, version: String): ValidationResponse {
        logger.debug("Validating snippet with PrintScript service: language={}, version={}", language, version)

        return try {
            // PrintScript service expects just the content string as @RequestBody String
            // We send it as a JSON string value (content wrapped in quotes)
            val result = client.post()
                .uri("/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(content) // WebClient will serialize it properly as JSON string
                .retrieve()
                .bodyToMono(ValidationResult::class.java)
                .block()

            // Convert ValidationResult to ValidationResponse
            if (result == null) {
                logger.error("Validation service returned null")
                ValidationResponse(
                    isValid = false,
                    errors = listOf(
                        ValidationError(
                            rule = "SERVICE_ERROR",
                            line = 1,
                            column = 1,
                            message = "El servicio de validación no devolvió una respuesta",
                        ),
                    ),
                )
            } else if (result.isValid) {
                logger.debug("Syntax validation passed")
                ValidationResponse(isValid = true, errors = null)
            } else {
                // Propagate validation error from PrintScript service
                logger.warn("Syntax validation failed: {} at line {}, column {}", result.rule, result.line, result.column)
                ValidationResponse(
                    isValid = false,
                    errors = listOf(
                        ValidationError(
                            rule = result.rule,
                            line = result.line,
                            column = result.column,
                            message = result.rule, // Use the rule as message, PrintScript service should provide descriptive rules
                        ),
                    ),
                )
            }
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            // Connection error - service is down or unreachable
            logger.error("Cannot connect to PrintScript validation service: {}", e.message)
            ValidationResponse(
                isValid = false,
                errors = listOf(
                    ValidationError(
                        rule = "CONNECTION_ERROR",
                        line = 1,
                        column = 1,
                        message = "No se pudo conectar al servicio de validación",
                    ),
                ),
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            // HTTP error response from the service (4xx, 5xx) - try to parse error message
            logger.error("PrintScript validation service returned error: {} - {}", e.statusCode, e.responseBodyAsString)

            // Try to extract message from service's error response
            val errorMessage = try {
                if (e.responseBodyAsString.isNotBlank()) {
                    val errorBody = objectMapper.readTree(e.responseBodyAsString)
                    errorBody.get("message")?.asText() ?: "Error del servicio de validación"
                } else {
                    "Error del servicio de validación: ${e.statusCode.value()}"
                }
            } catch (parseEx: Exception) {
                // If we can't parse it, provide a clear message based on status code
                when (e.statusCode.value()) {
                    401 -> "El servicio de validación requiere autenticación"
                    403 -> "El servicio de validación denegó el acceso"
                    404 -> "Endpoint de validación no encontrado"
                    500 -> "Error interno del servicio de validación"
                    else -> "Error del servicio de validación: ${e.statusCode.value()}"
                }
            }

            ValidationResponse(
                isValid = false,
                errors = listOf(
                    ValidationError(
                        rule = "VALIDATION_SERVICE_ERROR",
                        line = 1,
                        column = 1,
                        message = errorMessage,
                    ),
                ),
            )
        } catch (e: Exception) {
            // Any other unexpected error
            logger.error("Unexpected error during validation: {}", e.message, e)
            ValidationResponse(
                isValid = false,
                errors = listOf(
                    ValidationError(
                        rule = "UNKNOWN_ERROR",
                        line = 1,
                        column = 1,
                        message = e.message ?: "Error inesperado",
                    ),
                ),
            )
        }
    }

    fun triggerAutomaticFormatting(snippetId: String, userId: String, content: String) {
        try {
            // Create Snippet DTO for Redis stream - WebClient will serialize map to JSON
            // The endpoint expects: { userId, id, content, correlationID }
            val snippetDto = mapOf(
                "userId" to userId,
                "id" to snippetId,
                "content" to content,
                "correlationID" to java.util.UUID.randomUUID().toString(),
            )

            // Call the endpoint to trigger automatic formatting
            client.put()
                .uri("/redis/format/snippet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snippetDto)
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorComplete() // Don't fail snippet creation/update if formatting fails
                .block()
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting is optional
        }
    }

    fun triggerAutomaticLinting(snippetId: String, userId: String, content: String) {
        try {
            // Create Snippet DTO for Redis stream - WebClient will serialize map to JSON
            // The endpoint expects: { userId, id, content, correlationID }
            val snippetDto = mapOf(
                "userId" to userId,
                "id" to snippetId,
                "content" to content,
                "correlationID" to java.util.UUID.randomUUID().toString(),
            )

            // Call the endpoint to trigger automatic linting
            client.put()
                .uri("/redis/lint/snippet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snippetDto)
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorComplete() // Don't fail snippet creation/update if linting fails
                .block()
        } catch (e: Exception) {
            // Log but don't fail - automatic linting is optional
        }
    }

    fun triggerAutomaticTesting(snippetId: String, userId: String, content: String) {
        try {
            // Create Snippet DTO for Redis stream - WebClient will serialize map to JSON
            // The endpoint expects: { userId, id, content, correlationID }
            val snippetDto = mapOf(
                "userId" to userId,
                "id" to snippetId,
                "content" to content,
                "correlationID" to java.util.UUID.randomUUID().toString(),
            )

            // Call the endpoint to trigger automatic testing
            client.put()
                .uri("/redis/test/snippet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snippetDto)
                .retrieve()
                .bodyToMono(String::class.java)
                .onErrorComplete() // Don't fail snippet creation/update if testing fails
                .block()
        } catch (e: Exception) {
            // Log but don't fail - automatic testing is optional
        }
    }
}
