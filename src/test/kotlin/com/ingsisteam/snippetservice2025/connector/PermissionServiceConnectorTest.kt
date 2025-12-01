package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponse
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionRequest
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionResponse
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@ExtendWith(MockKExtension::class)
class PermissionServiceConnectorTest {

    @MockK(relaxed = true)
    private lateinit var webClientBuilder: WebClient.Builder

    @MockK(relaxed = true)
    private lateinit var webClient: WebClient

    @MockK(relaxed = true)
    private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec // For POST (has body)

    @MockK(relaxed = true)
    private lateinit var requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*> // For GET/DELETE (no body in initial spec)

    @MockK(relaxed = true)
    private lateinit var requestBodySpec: WebClient.RequestBodySpec // After URI for POST, before headers/retrieve

    @MockK(relaxed = true)
    private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*> // After URI for GET/DELETE or after body for POST, before retrieve

    @MockK(relaxed = true)
    private lateinit var responseSpec: WebClient.ResponseSpec

    @InjectMockKs
    private lateinit var permissionServiceConnector: PermissionServiceConnector

    private val permissionUrl = "http://localhost:8081" // Dummy URL for testing

    // Helper function to mock the WebClient chain for POST requests
    private fun mockWebClientPostChain(
        expectedResponse: PermissionResponse? = null,
        shouldThrow: Boolean = false,
    ) {
        every { webClientBuilder.baseUrl(permissionUrl) } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/api/permissions") } returns requestBodySpec
        every { requestBodySpec.contentType(MediaType.APPLICATION_JSON) } returns requestBodySpec
        every { requestBodySpec.bodyValue(any<PermissionRequest>()) } returns requestHeadersSpec // After bodyValue, it's RequestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.bodyToMono(PermissionResponse::class.java) } returns Mono.error(RuntimeException("API error"))
        } else {
            every { responseSpec.bodyToMono(PermissionResponse::class.java) } returns Mono.justOrEmpty(expectedResponse)
        }
    }

    // Helper function to mock the WebClient chain for GET requests returning a Mono
    private inline fun <reified T> mockWebClientGetMonoChain(
        uri: String,
        expectedResponse: T? = null,
        shouldThrow: Boolean = false,
    ) {
        every { webClientBuilder.baseUrl(permissionUrl) } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(uri) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.bodyToMono(T::class.java).block() } throws RuntimeException("API error")
        } else {
            every { responseSpec.bodyToMono(T::class.java).block() } returns expectedResponse
        }
    }

    // Helper function to mock the WebClient chain for GET requests returning a Flux
    private inline fun <reified T> mockWebClientGetFluxChain(
        uri: String,
        expectedResponse: List<T> = emptyList(),
        shouldThrow: Boolean = false,
    ) {
        every { webClientBuilder.baseUrl(permissionUrl) } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(uri) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.bodyToFlux(T::class.java).collectList().block() } throws RuntimeException("API error")
        } else {
            every { responseSpec.bodyToFlux(T::class.java).collectList().block() } returns expectedResponse
        }
    }

    // Helper function to mock the WebClient chain for DELETE requests
    private fun mockWebClientDeleteChain(uri: String, shouldThrow: Boolean = false) {
        every { webClientBuilder.baseUrl(permissionUrl) } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        every { webClient.delete() } returns requestHeadersUriSpec // DELETE returns RequestHeadersUriSpec
        every { requestHeadersUriSpec.uri(uri) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.bodyToMono(Void::class.java).block() } throws RuntimeException("API error")
        } else {
            every { responseSpec.bodyToMono(Void::class.java).block() } returns null
        }
    }

    // --- Tests for createPermission ---
    @Test
    fun `test createPermission success`() {
        val snippetId = "1"
        val userId = "user123"
        val role = "OWNER"
        val expectedResponse = PermissionResponse("1", snippetId, userId, role, "now")

        mockWebClientPostChain(expectedResponse = expectedResponse)

        val result = permissionServiceConnector.createPermission(snippetId, userId, role)

        assertNotNull(result)
        assertEquals(expectedResponse, result)
        verify(exactly = 1) {
            requestBodySpec.bodyValue(
                PermissionRequest(
                    snippet_id = snippetId,
                    user_id = userId,
                    role = role,
                ),
            )
        }
    }

    @Test
    fun `test createPermission returns null on API error`() {
        val snippetId = "1"
        val userId = "user123"
        val role = "OWNER"

        mockWebClientPostChain(shouldThrow = true)

        val result = permissionServiceConnector.createPermission(snippetId, userId, role)

        assertNull(result)
        verify(exactly = 1) { requestHeadersSpec.retrieve() }
    }

    // --- Tests for checkPermission ---
    @Test
    fun `test checkPermission returns true with role when user has permission`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = PermissionCheckResponse(has_permission = true, role = "OWNER")

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.checkPermission(snippetId, userId)

        assertTrue(result.has_permission)
        assertEquals("OWNER", result.role)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test checkPermission returns false when user does not have permission`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = PermissionCheckResponse(has_permission = false, role = null)

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.checkPermission(snippetId, userId)

        assertFalse(result.has_permission)
        assertNull(result.role)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test checkPermission throws RuntimeException on API error`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"

        mockWebClientGetMonoChain<PermissionCheckResponse>(uri, shouldThrow = true)

        assertThrows<RuntimeException> {
            permissionServiceConnector.checkPermission(snippetId, userId)
        }
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    // --- Tests for hasPermission ---
    @Test
    fun `test hasPermission returns true when API returns true`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = PermissionCheckResponseDTO(has_permission = true)

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.hasPermission(snippetId, userId)

        assertTrue(result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test hasPermission returns false when API returns false`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = PermissionCheckResponseDTO(has_permission = false)

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.hasPermission(snippetId, userId)

        assertFalse(result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test hasPermission returns true on API error (fail-safe)`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/check?snippetId=$snippetId&userId=$userId"

        mockWebClientGetMonoChain<PermissionCheckResponseDTO>(uri, shouldThrow = true)

        val result = permissionServiceConnector.hasPermission(snippetId, userId)

        assertTrue(result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    // --- Tests for hasWritePermission ---
    @Test
    fun `test hasWritePermission returns true when API returns true`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/write-check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = true

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.hasWritePermission(snippetId, userId)

        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test hasWritePermission returns false when API returns false`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/write-check?snippetId=$snippetId&userId=$userId"
        val expectedResponse = false

        mockWebClientGetMonoChain(uri, expectedResponse = expectedResponse)

        val result = permissionServiceConnector.hasWritePermission(snippetId, userId)

        assertFalse(result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test hasWritePermission returns false on API error (fail-secure)`() {
        val snippetId = "1"
        val userId = "user123"
        val uri = "/api/permissions/write-check?snippetId=$snippetId&userId=$userId"

        mockWebClientGetMonoChain<Boolean>(uri, shouldThrow = true)

        val result = permissionServiceConnector.hasWritePermission(snippetId, userId)

        assertFalse(result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    // --- Tests for getUserPermittedSnippets ---
    @Test
    fun `test getUserPermittedSnippets returns list of snippet IDs`() {
        val userId = "user123"
        val uri = "/api/permissions/user/$userId"
        val expectedPermissions = listOf(
            PermissionResponse("1", "101", userId, "OWNER", "now"),
            PermissionResponse("2", "102", userId, "READER", "now"),
        )
        val expectedSnippetIds = listOf("101", "102")

        mockWebClientGetFluxChain(uri, expectedResponse = expectedPermissions)

        val result = permissionServiceConnector.getUserPermittedSnippets(userId)

        assertEquals(expectedSnippetIds, result)
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    @Test
    fun `test getUserPermittedSnippets returns empty list on API error`() {
        val userId = "user123"
        val uri = "/api/permissions/user/$userId"

        mockWebClientGetFluxChain<PermissionResponse>(uri, shouldThrow = true)

        val result = permissionServiceConnector.getUserPermittedSnippets(userId)

        assertTrue(result.isEmpty())
        verify(exactly = 1) { requestHeadersUriSpec.uri(uri) }
    }

    // --- Tests for deleteSnippetPermissions ---
    @Test
    fun `test deleteSnippetPermissions success`() {
        val snippetId = "1"
        val userId1 = "user1"
        val userId2 = "user2"
        val permissionsToDel = listOf(
            PermissionResponse("1", snippetId, userId1, "OWNER", "now"),
            PermissionResponse("2", snippetId, userId2, "READER", "now"),
        )
        val fetchUri = "/api/permissions/snippet/$snippetId"
        val deleteUri1 = "/api/permissions/snippet/$snippetId/user/$userId1"
        val deleteUri2 = "/api/permissions/snippet/$snippetId/user/$userId2"

        // Mock fetching permissions
        mockWebClientGetFluxChain(fetchUri, expectedResponse = permissionsToDel)
        // Mock individual deletions
        every { webClient.delete() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(deleteUri1) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Void::class.java) } returns Mono.empty()

        every { requestHeadersUriSpec.uri(deleteUri2) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Void::class.java) } returns Mono.empty()
        // No need to mock block() again for the second delete as it's the same behavior

        permissionServiceConnector.deleteSnippetPermissions(snippetId)

        verify(exactly = 1) { requestHeadersUriSpec.uri(fetchUri) }
        verify(exactly = 1) { requestHeadersUriSpec.uri(deleteUri1) }
        verify(exactly = 1) { requestHeadersUriSpec.uri(deleteUri2) }
    }

    @Test
    fun `test deleteSnippetPermissions handles error fetching permissions`() {
        val snippetId = "1"
        val fetchUri = "/api/permissions/snippet/$snippetId"

        mockWebClientGetFluxChain<PermissionResponse>(fetchUri, shouldThrow = true)

        permissionServiceConnector.deleteSnippetPermissions(snippetId)

        verify(exactly = 1) { requestHeadersUriSpec.uri(fetchUri) }
        verify(exactly = 0) { webClient.delete() } // No deletions should be attempted
    }

    @Test
    fun `test deleteSnippetPermissions handles partial deletion errors`() {
        val snippetId = "1"
        val userId1 = "user1"
        val userId2 = "user2"
        val permissionsToDel = listOf(
            PermissionResponse("1", snippetId, userId1, "OWNER", "now"),
            PermissionResponse("2", snippetId, userId2, "READER", "now"),
        )
        val fetchUri = "/api/permissions/snippet/$snippetId"
        val deleteUri1 = "/api/permissions/snippet/$snippetId/user/$userId1"
        val deleteUri2 = "/api/permissions/snippet/$snippetId/user/$userId2"

        // Mock fetching permissions
        mockWebClientGetFluxChain(fetchUri, expectedResponse = permissionsToDel)
        // Mock first deletion to fail
        every { webClient.delete() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(deleteUri1) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Void::class.java) } throws RuntimeException("Deletion error 1")

        // Mock second deletion to succeed
        every { webClient.delete() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(deleteUri2) } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec
        every { responseSpec.bodyToMono(Void::class.java) } returns Mono.empty()

        permissionServiceConnector.deleteSnippetPermissions(snippetId)

        verify(exactly = 1) { requestHeadersUriSpec.uri(fetchUri) }
        verify(exactly = 1) { requestHeadersUriSpec.uri(deleteUri1) }
        verify(exactly = 1) { requestHeadersUriSpec.uri(deleteUri2) }
    }
}
