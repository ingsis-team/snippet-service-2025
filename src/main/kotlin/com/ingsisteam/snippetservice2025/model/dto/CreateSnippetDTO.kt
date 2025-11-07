package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateSnippetDTO(
    @field:NotBlank(message = "Snippet name cannot be null or empty")
    val name: String,

    @field:NotBlank(message = "Snippet description cannot be null or empty")
    val description: String,

    @field:NotNull(message = "Snippet language cannot be null")
    val language: SnippetLanguage,

    @field:NotBlank(message = "Content cannot be null or empty")
    val content: String,

    @field:NotBlank(message = "Version cannot be null or empty")
    val version: String,
)
