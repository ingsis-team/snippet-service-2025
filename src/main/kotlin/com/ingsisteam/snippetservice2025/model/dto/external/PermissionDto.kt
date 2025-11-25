package com.ingsisteam.snippetservice2025.model.dto.external

data class PermissionRequest(
    val snippet_id: String,
    val user_id: String,
    val role: String = "OWNER",
)

data class PermissionResponse(
    val id: String,
    val snippet_id: String,
    val user_id: String,
    val role: String,
    val created_at: String? = null,
)

data class PermissionCheckResponseDTO(
    val has_permission: Boolean,
    val role: String? = null,
)

// Alias for compatibility
typealias PermissionCheckResponse = PermissionCheckResponseDTO
