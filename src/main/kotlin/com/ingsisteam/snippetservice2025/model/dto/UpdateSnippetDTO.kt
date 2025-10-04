package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import jakarta.validation.constraints.NotBlank

data class UpdateSnippetDTO(
    @field:NotBlank(message = "Snippet name cannot be null or empty")
    val name: String?,

    val description: String?,

    val language: SnippetLanguage?,

    val content: String?,
)
