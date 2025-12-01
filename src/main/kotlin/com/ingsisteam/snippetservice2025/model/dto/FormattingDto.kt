package com.ingsisteam.snippetservice2025.model.dto

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

// DTO para reglas de formateo
data class FormatterRulesFileDTO(
    val spaceBeforeColon: Boolean,
    val spaceAfterColon: Boolean,
    val spaceAroundEquals: Boolean,
    val lineBreak: Int,
    val lineBreakPrintln: Int,
    val conditionalIndentation: Int,
)
