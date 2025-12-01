package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsisteam.snippetservice2025.exception.PrintScriptServiceException
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.model.dto.external.SCAOutput
import com.ingsisteam.snippetservice2025.model.dto.external.SnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.external.SnippetOutputDTO
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationError
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResponse
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
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
        logger.info(
            "Validating snippet with PrintScript service: language={}, version={}, content length={}, content preview='{}'",
            language,
            version,
            content.length,
            content.take(100).replace("\n", "\\n").replace("\r", "\\r"),
        )
        if (content.isBlank()) {
            logger.warn("Content is blank or empty!")
        }

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
            } catch (_: Exception) {
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

    fun formatSnippet(snippetId: String, correlationId: String, language: String, version: String, input: String, userId: String): SnippetOutputDTO {
        logger.debug("Formatting snippet with PrintScript service: snippetId={}", snippetId)

        return try {
            val snippetDto = SnippetDTO(
                snippetId = snippetId,
                correlationId = correlationId,
                language = language.lowercase(),
                version = version,
                input = input,
                userId = userId,
            )

            logger.debug("Sending format request to PrintScript: {}", snippetDto)

            val result = client.post()
                .uri("/format")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snippetDto)
                .retrieve()
                .bodyToMono(SnippetOutputDTO::class.java)
                .block()

            logger.debug("Received format response from PrintScript: {}", result)

            result ?: throw PrintScriptServiceException(
                message = "El servicio no devolvió una respuesta",
                operation = "formatear snippet",
                statusCode = null,
            )
        } catch (e: PrintScriptServiceException) {
            throw e
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error formatting snippet: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "formatear snippet",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error formatting snippet: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "formatear snippet",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error formatting snippet: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "formatear snippet",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun lintSnippet(snippetId: String, correlationId: String, language: String, version: String, input: String, userId: String): List<SCAOutput> {
        logger.debug("Linting snippet with PrintScript service: snippetId={}", snippetId)

        return try {
            val snippetDto = SnippetDTO(
                snippetId = snippetId,
                correlationId = correlationId,
                language = language,
                version = version,
                input = input,
                userId = userId,
            )

            val result = client.post()
                .uri("/lint")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(snippetDto)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<SCAOutput>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error linting snippet: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "analizar snippet (lint)",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error linting snippet: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "analizar snippet (lint)",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error linting snippet: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "analizar snippet (lint)",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun getFormattingRules(userId: String, correlationId: String): List<Rule> {
        logger.debug("Getting formatting rules for user: {}", userId)

        return try {
            val path = "/rules/format/$userId"
            logger.debug("Calling PrintScript GET {}", path)
            val result = client.get()
                .uri(path)
                .header("Correlation-id", correlationId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Rule>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error getting formatting rules: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "obtener reglas de formateo",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error getting formatting rules: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "obtener reglas de formateo",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error getting formatting rules: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "obtener reglas de formateo",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun getLintingRules(userId: String, correlationId: String): List<Rule> {
        logger.debug("Getting linting rules for user: {}", userId)

        return try {
            val path = "/rules/lint/$userId"
            logger.debug("Calling PrintScript GET {}", path)
            val result = client.get()
                .uri(path)
                .header("Correlation-id", correlationId)
                .header("X-User-Id", userId)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Rule>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error getting linting rules: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "obtener reglas de linting",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error getting linting rules: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "obtener reglas de linting",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error getting linting rules: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "obtener reglas de linting",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun saveFormattingRules(userId: String, correlationId: String, rules: List<Rule>): List<Rule> {
        logger.debug("Saving formatting rules for user: {}", userId)

        return try {
            val path = "/rules/format/$userId"
            logger.debug("Calling PrintScript POST {} with rulesCount={}", path, rules.size)
            val result = client.post()
                .uri(path)
                .header("Correlation-id", correlationId)
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rules)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Rule>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error saving formatting rules: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "guardar reglas de formateo",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error saving formatting rules: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "guardar reglas de formateo",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error saving formatting rules: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "guardar reglas de formateo",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun saveLintingRules(userId: String, correlationId: String, rules: List<Rule>): List<Rule> {
        logger.debug("Saving linting rules for user: {}", userId)

        return try {
            val path = "/rules/lint/$userId"
            logger.debug("Calling PrintScript POST {} with rulesCount={}", path, rules.size)
            val result = client.post()
                .uri(path)
                .header("Correlation-id", correlationId)
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(rules)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Rule>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error saving linting rules: {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "guardar reglas de linting",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error saving linting rules: {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "guardar reglas de linting",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error saving linting rules: {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "guardar reglas de linting",
                statusCode = null,
                cause = e,
            )
        }
    }

    fun saveFormattingRulesFile(userId: String, correlationId: String, dto: FormatterRulesFileDTO): List<Rule> {
        logger.debug("Saving formatting rules (file DTO) for user: {}", userId)

        return try {
            val path = "/rules/format/$userId"
            logger.debug("Calling PrintScript POST {} with FormatterRulesFileDTO", path)
            val result = client.post()
                .uri(path)
                .header("Correlation-id", correlationId)
                .header("X-User-Id", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<Rule>>() {})
                .block()

            result ?: emptyList()
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Error saving formatting rules (file DTO): {} - {}", e.statusCode, e.responseBodyAsString)
            throw PrintScriptServiceException(
                message = "Error HTTP ${e.statusCode.value()}: ${e.statusText}",
                operation = "guardar reglas de formateo",
                statusCode = e.statusCode.value(),
                cause = e,
            )
        } catch (e: org.springframework.web.reactive.function.client.WebClientRequestException) {
            logger.error("Connection error saving formatting rules (file DTO): {}", e.message)
            throw PrintScriptServiceException(
                message = "No se pudo conectar al servicio de PrintScript",
                operation = "guardar reglas de formateo",
                statusCode = null,
                cause = e,
            )
        } catch (e: Exception) {
            logger.error("Unexpected error saving formatting rules (file DTO): {}", e.message, e)
            throw PrintScriptServiceException(
                message = e.message ?: "Error inesperado",
                operation = "guardar reglas de formateo",
                statusCode = null,
                cause = e,
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
