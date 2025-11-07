package com.ingsisteam.snippetservice2025.model.dto

import jakarta.validation.constraints.NotBlank

data class UpdateSnippetDTO(
    @field:NotBlank(message = "Content cannot be null or empty")
    val content: String,
)
