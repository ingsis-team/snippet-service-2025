package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationError
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResponse
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PrintScriptServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${printscript.url}") private val printScriptUrl: String,
) {

    private val client: WebClient by lazy {
        webClient.baseUrl(printScriptUrl).build()
    }

    private val objectMapper = ObjectMapper()

    fun validateSnippet(content: String, language: String, version: String): ValidationResponse {
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
                ValidationResponse(isValid = true, errors = null)
            } else {
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
            println("⚠️ [PRINTSCRIPT] Could not validate snippet, error: ${e.message}")
            e.printStackTrace()
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
}
