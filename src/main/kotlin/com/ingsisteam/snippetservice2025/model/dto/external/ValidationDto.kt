package com.ingsisteam.snippetservice2025.model.dto.external

import com.fasterxml.jackson.annotation.JsonProperty

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

// DTO matching printscript-service ValidationResult
// PrintScript service returns snake_case JSON, so we need to map it explicitly
data class ValidationResult(
    @JsonProperty("is_valid")
    val isValid: Boolean,
    val rule: String,
    val line: Int,
    val column: Int,
)
