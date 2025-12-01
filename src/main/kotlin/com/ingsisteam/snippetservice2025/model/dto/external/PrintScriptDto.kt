package com.ingsisteam.snippetservice2025.model.dto.external

// DTOs para PrintScript Service

// DTO para enviar snippets a PrintScript
data class SnippetDTO(
    val snippetId: String,
    val correlationId: String,
    val language: String,
    val version: String,
    val input: String,
    val userId: String,
)

// DTO para recibir output de PrintScript
data class SnippetOutputDTO(
    val output: String,
    val correlationId: String,
    val snippetId: String,
)

// DTO para reglas de formateo/linting
data class Rule(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val value: Any, // Puede ser Boolean, String, etc.
)

// DTO para test
data class TestDTO(
    val input: List<String>,
    val output: List<String>,
    val snippet: String,
    val envVars: Map<String, String>,
)

// DTO para análisis estático de código
data class SCAOutput(
    val rule: String,
    val line: Int,
    val column: Int,
    val message: String,
)
