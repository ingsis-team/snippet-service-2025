package com.ingsisteam.snippetservice2025.model.dto.external

import com.fasterxml.jackson.annotation.JsonProperty

// DTOs para PrintScript Service

// DTO para enviar snippets a PrintScript
data class SnippetDTO(
    @JsonProperty("snippet_id")
    val snippetId: String,
    @JsonProperty("correlation_id")
    val correlationId: String,
    @JsonProperty("language")
    val language: String,
    @JsonProperty("version")
    val version: String,
    @JsonProperty("input")
    val input: String,
    @JsonProperty("user_id")
    val userId: String,
)

// DTO para recibir output de PrintScript
data class SnippetOutputDTO(
    @JsonProperty("snippet")
    val snippet: String,
    @JsonProperty("correlation_id")
    val correlationId: String,
    @JsonProperty("snippet_id")
    val snippetId: String,
)

// DTO para reglas de formateo/linting
data class Rule(
    @JsonProperty("name")
    val name: String,
    @JsonProperty("value")
    val value: Any, // Puede ser Boolean, Int, o String
)

// DTO para test
data class TestDTO(
    @JsonProperty("input")
    val input: List<String>,
    @JsonProperty("output")
    val output: List<String>,
    @JsonProperty("snippet")
    val snippet: String,
    @JsonProperty("env_vars")
    val envVars: Map<String, String>,
)

// DTO para análisis estático de código
data class SCAOutput(
    @JsonProperty("rule")
    val rule: String,
    @JsonProperty("line")
    val line: Int,
    @JsonProperty("column")
    val column: Int,
    @JsonProperty("message")
    val message: String,
)
