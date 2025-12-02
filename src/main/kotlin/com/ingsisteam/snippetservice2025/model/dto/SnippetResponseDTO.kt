package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import java.time.LocalDateTime

data class SnippetResponseDTO(
    val id: String,
    val name: String,
    val description: String,
    val language: SnippetLanguage,
    val content: String,
    val userId: String,
    val version: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val compliance: String = "compliant", // Default to compliant since snippets are validated on creation
)
