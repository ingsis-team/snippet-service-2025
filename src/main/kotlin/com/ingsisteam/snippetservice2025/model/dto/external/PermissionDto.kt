package com.ingsisteam.snippetservice2025.model.dto.external

data class PermissionRequest(
    val snippet_id: Long,
    val user_id: String,
    val role: String = "OWNER",
)

data class PermissionResponse(
    val id: Long,
    val snippetId: Long,
    val userId: String,
    val role: String,
    val createdAt: String,
)
