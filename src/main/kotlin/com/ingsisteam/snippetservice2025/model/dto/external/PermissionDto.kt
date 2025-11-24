package com.ingsisteam.snippetservice2025.model.dto.external

data class PermissionRequest(
    val snippetId: Long,
    val userId: String,
    val role: String = "OWNER",
)

data class PermissionResponse(
    val id: Long,
    val snippetId: Long,
    val userId: String,
    val role: String,
    val createdAt: String? = null,
)

data class PermissionCheckResponseDTO(
    val hasPermission: Boolean,
    val role: String? = null,
)

// Alias for compatibility
typealias PermissionCheckResponse = PermissionCheckResponseDTO
