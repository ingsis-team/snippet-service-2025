package com.ingsisteam.snippetservice2025.model.dto

// DTO para respuesta de ejecución de todos los tests de un snippet
data class RunAllTestsResponseDTO(
    val snippetId: String,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val results: List<TestExecutionResult>,
)

data class TestExecutionResult(
    val testId: String,
    val testName: String,
    val passed: Boolean,
    val actualOutputs: List<String>,
    val expectedOutputs: List<String>,
    val errors: List<String>,
)

// DTO para respuesta de formateo masivo
data class FormatAllSnippetsResponseDTO(
    val totalSnippets: Int,
    val successfullyFormatted: Int,
    val failed: Int,
    val results: List<FormatResult>,
)

data class FormatResult(
    val snippetId: String,
    val snippetName: String,
    val success: Boolean,
    val errorMessage: String? = null,
)

// DTO para respuesta de linting masivo
data class LintAllSnippetsResponseDTO(
    val totalSnippets: Int,
    val snippetsWithIssues: Int,
    val snippetsWithoutIssues: Int,
    val results: List<LintResult>,
)

data class LintResult(
    val snippetId: String,
    val snippetName: String,
    val issuesCount: Int,
    val issues: List<LintIssue>,
)
