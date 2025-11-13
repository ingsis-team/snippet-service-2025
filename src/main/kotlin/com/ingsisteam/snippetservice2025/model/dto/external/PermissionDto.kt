package com.ingsisteam.snippetservice2025.model.dto.external

import com.fasterxml.jackson.annotation.JsonProperty

data class PermissionRequest(
    val snippet_id: Long,
    val user_id: String,
    val role: String = "OWNER",
)

data class PermissionResponse(
    val id: Long,
    @JsonProperty("snippet_id")
    val snippetId: Long,
    @JsonProperty("user_id")
    val userId: String,
    val role: String,
    @JsonProperty("created_at")
    val createdAt: String,
)

data class PermissionCheckResponseDTO(
    @JsonProperty("has_permission")
    val hasPermission: Boolean,
    val role: String? = null,
)

// Alias for compatibility
typealias PermissionCheckResponse = PermissionCheckResponseDTO
