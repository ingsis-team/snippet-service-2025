package com.ingsisteam.snippetservice2025.model.dto

data class ExecuteSnippetResponseDTO(
    val outputs: List<String>,
    val errors: List<String> = emptyList(),
)

