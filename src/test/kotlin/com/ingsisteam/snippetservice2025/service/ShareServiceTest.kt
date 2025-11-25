package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.Auth0Connector
import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponse
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionResponse
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class ShareServiceTest {

    @MockK
    private lateinit var auth0Connector: Auth0Connector

    @MockK
    private lateinit var permissionServiceConnector: PermissionServiceConnector

    @InjectMockKs
    private lateinit var shareService: ShareService

    @Test
    fun `test getAvailableUsers`() {
        // Given
        val users = listOf(
            Auth0UserDTO("1", "user1@example.com", "user1", "user1.jpg", "user1"),
            Auth0UserDTO("2", "user2@example.com", "user2", "user2.jpg", "user2"),
        )
        every { auth0Connector.getUsers(any()) } returns users

        // When
        val result = shareService.getAvailableUsers(null)
        println("Actual result: $result")

        // Then
        assertEquals(2, result.size)
        assertEquals("user1", result[0].name)
        assertEquals("user2", result[1].name)
    }

    @Test
    fun `test shareSnippet success`() {
        // Given
        val snippetId = "1"
        val currentUserId = "owner"
        val targetUserId = "reader"
        val shareSnippetDTO = ShareSnippetDTO(snippetId, targetUserId)
        val ownerPermissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")
        val readerPermissionCheck = PermissionCheckResponse(has_permission = false, role = null)
        val createdPermission = PermissionResponse(
            id = "1",
            snippet_id = snippetId,
            user_id = targetUserId,
            role = "READ",
            created_at = LocalDateTime.now().toString(),
        )

        every { permissionServiceConnector.checkPermission(snippetId, currentUserId) } returns ownerPermissionCheck
        every { permissionServiceConnector.checkPermission(snippetId, targetUserId) } returns readerPermissionCheck
        every { permissionServiceConnector.createPermission(snippetId, targetUserId, "READ") } returns createdPermission

        // When
        val result = shareService.shareSnippet(shareSnippetDTO, currentUserId)

        // Then
        assertEquals(snippetId, result.snippetId)
        assertEquals(targetUserId, result.sharedWithUserId)
        assertEquals("READ", result.role)
        assertEquals("Snippet compartido exitosamente con permisos de lectura", result.message)
    }

    @Test
    fun `test shareSnippet not as owner`() {
        // Given
        val snippetId = "1"
        val currentUserId = "not-owner"
        val targetUserId = "reader"
        val shareSnippetDTO = ShareSnippetDTO(snippetId, targetUserId)
        val notOwnerPermissionCheck = PermissionCheckResponse(has_permission = true, role = "READER")

        every { permissionServiceConnector.checkPermission(snippetId, currentUserId) } returns notOwnerPermissionCheck

        // When & Then
        assertThrows<com.ingsisteam.snippetservice2025.exception.PermissionDeniedException> {
            shareService.shareSnippet(shareSnippetDTO, currentUserId)
        }
    }

    @Test
    fun `test shareSnippet with existing permission`() {
        // Given
        val snippetId = "1"
        val currentUserId = "owner"
        val targetUserId = "reader"
        val shareSnippetDTO = ShareSnippetDTO(snippetId, targetUserId)
        val ownerPermissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")
        val readerPermissionCheck = PermissionCheckResponse(has_permission = true, role = "READER")

        every { permissionServiceConnector.checkPermission(snippetId, currentUserId) } returns ownerPermissionCheck
        every { permissionServiceConnector.checkPermission(snippetId, targetUserId) } returns readerPermissionCheck

        // When
        val result = shareService.shareSnippet(shareSnippetDTO, currentUserId)

        // Then
        assertEquals(snippetId, result.snippetId)
        assertEquals(targetUserId, result.sharedWithUserId)
        assertEquals("READER", result.role)
        assertEquals("El usuario ya tiene acceso a este snippet con rol: READER", result.message)
    }

    @Test
    fun `test shareSnippet permission creation fails`() {
        // Given
        val snippetId = "1"
        val currentUserId = "owner"
        val targetUserId = "reader"
        val shareSnippetDTO = ShareSnippetDTO(snippetId, targetUserId)
        val ownerPermissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")
        val readerPermissionCheck = PermissionCheckResponse(has_permission = false, role = null)

        every { permissionServiceConnector.checkPermission(snippetId, currentUserId) } returns ownerPermissionCheck
        every { permissionServiceConnector.checkPermission(snippetId, targetUserId) } returns readerPermissionCheck
        every { permissionServiceConnector.createPermission(snippetId, targetUserId, "READ") } returns null

        // When & Then
        assertThrows<RuntimeException> {
            shareService.shareSnippet(shareSnippetDTO, currentUserId)
        }
    }
}
