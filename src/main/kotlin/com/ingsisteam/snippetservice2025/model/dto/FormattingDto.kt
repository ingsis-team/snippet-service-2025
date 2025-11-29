package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.dto.external.Rule

// DTO para solicitar formateo
data class FormatSnippetDTO(
    val snippetId: String,
)

// DTO para respuesta de formateo
data class FormatSnippetResponseDTO(
    val formattedContent: String,
)

// DTO para solicitar linting
data class LintSnippetDTO(
    val snippetId: String,
)

// DTO para respuesta de linting
data class LintSnippetResponseDTO(
    val issues: List<LintIssue>,
)

data class LintIssue(
    val rule: String,
    val line: Int,
    val column: Int,
    val message: String,
)

// DTO para actualizar reglas
data class UpdateRulesDTO(
    val rules: List<Rule>,
)
