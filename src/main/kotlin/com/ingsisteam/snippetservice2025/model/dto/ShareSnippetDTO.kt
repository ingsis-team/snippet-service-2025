package com.ingsisteam.snippetservice2025.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO para compartir un snippet con un usuario
 */
data class ShareSnippetDTO(
    @JsonProperty("snippet_id")
    val snippetId: Long,
    @JsonProperty("target_user_id")
    val targetUserId: String, // ID del usuario con el que se comparte
)

/**
 * DTO de respuesta al compartir un snippet
 */
data class ShareSnippetResponseDTO(
    val snippetId: Long,
    val sharedWithUserId: String,
    val role: String,
    val message: String,
)
