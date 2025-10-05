package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.ValidationRequest
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResponse
import org.springframework.beans.factory.annotation.Value
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

    fun validateSnippet(content: String, language: String, version: String): ValidationResponse {
        val request = ValidationRequest(
            content = content,
            language = language,
            version = version,
        )

        return client.post()
            .uri("/api/validate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ValidationResponse::class.java)
            .onErrorReturn(
                ValidationResponse(
                    isValid = false,
                    errors = listOf(
                        com.ingsisteam.snippetservice2025.model.dto.external.ValidationError(
                            rule = "CONNECTION_ERROR",
                            line = 1,
                            column = 1,
                            message = "Error connecting to validation service",
                        ),
                    ),
                ),
            )
            .block() ?: ValidationResponse(isValid = false)
    }
}
