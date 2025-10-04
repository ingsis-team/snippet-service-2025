package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import java.time.LocalDateTime

data class SnippetResponseDTO(
    val id: Long,
    val name: String,
    val description: String,
    val language: SnippetLanguage,
    val content: String,
    val userId: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
