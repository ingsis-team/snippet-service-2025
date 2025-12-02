package com.ingsisteam.snippetservice2025.model.dto.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PermissionDtoTest {

    @Test
    fun `test PermissionRequest properties`() {
        val dto = PermissionRequest(
            snippet_id = "snippet123",
            user_id = "user456",
            role = "EDITOR"
        )

        assertEquals("snippet123", dto.snippet_id)
        assertEquals("user456", dto.user_id)
        assertEquals("EDITOR", dto.role)
    }

    @Test
    fun `test PermissionRequest with default role`() {
        val dto = PermissionRequest(
            snippet_id = "snippet789",
            user_id = "user012"
        )

        assertEquals("snippet789", dto.snippet_id)
        assertEquals("user012", dto.user_id)
        assertEquals("OWNER", dto.role) // Default value
    }

    @Test
    fun `test PermissionResponse properties`() {
        val dto = PermissionResponse(
            id = "permId123",
            snippet_id = "snippet123",
            user_id = "user456",
            role = "VIEWER",
            created_at = "2025-12-02T10:00:00Z"
        )

        assertEquals("permId123", dto.id)
        assertEquals("snippet123", dto.snippet_id)
        assertEquals("user456", dto.user_id)
        assertEquals("VIEWER", dto.role)
        assertEquals("2025-12-02T10:00:00Z", dto.created_at)
    }

    @Test
    fun `test PermissionResponse with null created_at`() {
        val dto = PermissionResponse(
            id = "permId456",
            snippet_id = "snippet789",
            user_id = "user012",
            role = "OWNER"
        )

        assertEquals("permId456", dto.id)
        assertEquals("snippet789", dto.snippet_id)
        assertEquals("user012", dto.user_id)
        assertEquals("OWNER", dto.role)
        assertEquals(null, dto.created_at)
    }

    @Test
    fun `test PermissionCheckResponseDTO properties`() {
        val dto = PermissionCheckResponseDTO(
            has_permission = true,
            role = "OWNER"
        )

        assertEquals(true, dto.has_permission)
        assertEquals("OWNER", dto.role)
    }

    @Test
    fun `test PermissionCheckResponseDTO with null role`() {
        val dto = PermissionCheckResponseDTO(
            has_permission = false
        )

        assertEquals(false, dto.has_permission)
        assertEquals(null, dto.role)
    }
}
