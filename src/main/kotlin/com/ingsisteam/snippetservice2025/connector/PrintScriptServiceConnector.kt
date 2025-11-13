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
                .onErrorReturn(
                    ValidationResult(
                        isValid = false,
                        rule = "CONNECTION_ERROR",
                        line = 1,
                        column = 1,
                    ),
                )
                .block()

            // Convert ValidationResult to ValidationResponse
            if (result == null) {
                logger.error("Validation service returned null")
                ValidationResponse(
                    isValid = false,
                    errors = listOf(
                        ValidationError(
                            rule = "UNKNOWN_ERROR",
                            line = 1,
                            column = 1,
                            message = "Validation service returned null",
                        ),
                    ),
                )
            } else if (result.isValid) {
                logger.debug("Syntax validation passed")
                ValidationResponse(isValid = true, errors = null)
            } else {
                logger.warn("Syntax validation failed: {} at line {}, column {}", result.rule, result.line, result.column)
                ValidationResponse(
                    isValid = false,
                    errors = listOf(
                        ValidationError(
                            rule = result.rule,
                            line = result.line,
                            column = result.column,
                            message = "Validation failed: ${result.rule} at line ${result.line}, column ${result.column}",
                        ),
                    ),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            logger.error("Error connecting to PrintScript validation service: {}", e.message, e)
            ValidationResponse(
                isValid = false,
                errors = listOf(
                    ValidationError(
                        rule = "CONNECTION_ERROR",
                        line = 1,
                        column = 1,
                        message = "Error connecting to validation service: ${e.message}",
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
