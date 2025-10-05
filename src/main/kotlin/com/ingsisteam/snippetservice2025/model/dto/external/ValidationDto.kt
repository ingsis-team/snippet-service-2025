package com.ingsisteam.snippetservice2025.model.dto.external

data class ValidationRequest(
    val content: String,
    val language: String,
    val version: String,
)

data class ValidationResponse(
    val isValid: Boolean,
    val errors: List<ValidationError>? = null,
)

data class ValidationError(
    val rule: String,
    val line: Int,
    val column: Int,
    val message: String,
)
