package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.UnauthorizedException
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.service.ShareService
import com.ingsisteam.snippetservice2025.service.SnippetService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import java.time.LocalDateTime

class SnippetControllerTest {

    private lateinit var snippetService: SnippetService
    private lateinit var shareService: ShareService
    private lateinit var snippetController: SnippetController

    private val userId = "another-user@example.com"
    private val SNIPPET_ID = "1"

    @BeforeEach
    fun setUp() {
        io.mockk.clearAllMocks()
        snippetService = mockk(relaxed = true)
        shareService = mockk(relaxed = true)
        snippetController = SnippetController(snippetService, shareService)
    }

    // Helper function to create a dummy SnippetResponseDTO
    private fun createDummySnippetResponseDTO(
        id: String = SNIPPET_ID,
        name: String = "Test Snippet",
        description: String = "Test Description",
        language: SnippetLanguage = SnippetLanguage.PRINTSCRIPT,
        content: String = "test content",
        version: String = "1.0",
    ) = SnippetResponseDTO(
        id = id,
        name = name,
        description = description,
        language = language,
        content = content,
        userId = userId,
        version = version,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    // Helper function to create a dummy Auth0UserDTO
    private fun createDummyAuth0UserDTO(id: String = "user1") = Auth0UserDTO(
        userId = id,
        email = "$id@example.com",
        name = "User $id",
        nickname = id,
        picture = "pic",
    )

    // --- Tests for getUserId helper (implicitly tested by using it in controller methods) ---

    // Minimal test to verify basic setup
    @Test
    fun `getSnippet should return 200 on success (minimal test)`() {
        val expectedResponse = createDummySnippetResponseDTO(id = SNIPPET_ID)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.getSnippet(SNIPPET_ID, userId) } returns expectedResponse

        val response: ResponseEntity<SnippetResponseDTO> = snippetController.getSnippet(SNIPPET_ID, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedResponse.id, response.body?.id)
        assertEquals(expectedResponse.name, response.body?.name)

        verify(exactly = 1) { snippetService.getSnippet(SNIPPET_ID, userId) }
    }

    @Test
    fun `createSnippet should return 201 on success`() {
        val createSnippetDTO = CreateSnippetDTO(
            name = "New Snippet",
            description = "Description of new snippet",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "print('Hello, World!')",
            version = "1.0",
        )
        val expectedResponse = createDummySnippetResponseDTO(
            id = "2",
            name = createSnippetDTO.name,
            description = createSnippetDTO.description,
            language = createSnippetDTO.language,
            content = createSnippetDTO.content,
            version = "1.0",
        )
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.createSnippet(createSnippetDTO, userId) } returns expectedResponse

        val response = snippetController.createSnippet(createSnippetDTO, mockJwt)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(expectedResponse.id, response.body?.id)
        assertEquals(expectedResponse.name, response.body?.name)
        verify(exactly = 1) { snippetService.createSnippet(createSnippetDTO, userId) }
    }

    @Test
    fun `getAllSnippets should return 200 with list of snippets when no filter is provided`() {
        val snippet1 = createDummySnippetResponseDTO(id = "10", name = "Snippet 1")
        val snippet2 = createDummySnippetResponseDTO(id = "11", name = "Snippet 2")
        val expectedSnippets = listOf(snippet1, snippet2)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.getAllSnippets(userId, null) } returns expectedSnippets

        val response: ResponseEntity<List<SnippetResponseDTO>> = snippetController.getAllSnippets(null, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(expectedSnippets[0].name, response.body?.get(0)?.name)
        verify(exactly = 1) { snippetService.getAllSnippets(userId, null) }
    }

    @Test
    fun `getAllSnippets should return 200 with filtered list of snippets when filter is provided`() {
        val filterName = "Filtered Snippet"
        val snippet1 = createDummySnippetResponseDTO(id = "20", name = filterName)
        val expectedSnippets = listOf(snippet1)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.getAllSnippets(userId, filterName) } returns expectedSnippets

        val response: ResponseEntity<List<SnippetResponseDTO>> = snippetController.getAllSnippets(filterName, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(expectedSnippets[0].name, response.body?.get(0)?.name)
        verify(exactly = 1) { snippetService.getAllSnippets(userId, filterName) }
    }

    @Test
    fun `deleteSnippet should return 200 on successful deletion`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.deleteSnippet(SNIPPET_ID, userId) } answers { nothing }

        val response: ResponseEntity<SnippetController.SuccessResponse> = snippetController.deleteSnippet(SNIPPET_ID, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(true, response.body?.success)
        assertEquals("Snippet con ID $SNIPPET_ID eliminado exitosamente", response.body?.message)
        verify(exactly = 1) { snippetService.deleteSnippet(SNIPPET_ID, userId) }
    }

    @Test
    fun `getSnippet should return 404 when snippetService throws SnippetNotFoundException`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.getSnippet(SNIPPET_ID, userId) } throws SnippetNotFoundException("Snippet not found")

        val exception = assertThrows<SnippetNotFoundException> {
            snippetController.getSnippet(SNIPPET_ID, mockJwt)
        }

        assertEquals("Snippet not found", exception.message)
        verify(exactly = 1) { snippetService.getSnippet(SNIPPET_ID, userId) }
    }

    @Test
    fun `getSnippet should return 403 when snippetService throws PermissionDeniedException`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetService.getSnippet(SNIPPET_ID, userId) } throws PermissionDeniedException("Permission denied")

        val exception = assertThrows<PermissionDeniedException> {
            snippetController.getSnippet(SNIPPET_ID, mockJwt)
        }

        assertEquals("Permission denied", exception.message)
        verify(exactly = 1) { snippetService.getSnippet(SNIPPET_ID, userId) }
    }

    @Test
    fun `getAvailableUsers should return 200 OK with a list of users when no filter is provided`() {
        val user1 = createDummyAuth0UserDTO(id = "user10")
        val user2 = createDummyAuth0UserDTO(id = "user11")
        val expectedUsers = listOf(user1, user2)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { shareService.getAvailableUsers(null) } returns expectedUsers

        val response: ResponseEntity<List<Auth0UserDTO>> = snippetController.getAvailableUsers(null, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(expectedUsers[0].email, response.body?.get(0)?.email)
        verify(exactly = 1) { shareService.getAvailableUsers(null) }
    }

    @Test
    fun `getAvailableUsers should return 200 OK with a filtered list of users when a filter is provided`() {
        val searchFilter = "john"
        val user1 = createDummyAuth0UserDTO(id = "john.doe")
        val expectedUsers = listOf(user1)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { shareService.getAvailableUsers(searchFilter) } returns expectedUsers

        val response: ResponseEntity<List<Auth0UserDTO>> = snippetController.getAvailableUsers(searchFilter, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals(expectedUsers[0].email, response.body?.get(0)?.email)
        verify(exactly = 1) { shareService.getAvailableUsers(searchFilter) }
    }

    @Test
    fun `getAvailableUsers should throw RuntimeException when shareService throws RuntimeException`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { shareService.getAvailableUsers(any()) } throws RuntimeException("Auth0 connection error")

        val exception = assertThrows<RuntimeException> {
            snippetController.getAvailableUsers(null, mockJwt)
        }

        assertEquals("Auth0 connection error", exception.message)
        verify(exactly = 1) { shareService.getAvailableUsers(any()) }
    }

    @Test
    fun `getSnippet should throw UnauthorizedException when JWT is null`() {
        assertThrows<UnauthorizedException> {
            snippetController.getSnippet("1", null)
        }
    }

    @Test
    fun `getSnippet should throw UnauthorizedException for test user`() {
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.getSnippet("1", mockJwt)
        }
    }

    @Test
    fun `createSnippet should throw UnauthorizedException when JWT is null`() {
        val createSnippetDTO = CreateSnippetDTO(
            name = "New Snippet",
            description = "Description of new snippet",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "print('Hello, World!')",
            version = "1.0",
        )
        assertThrows<UnauthorizedException> {
            snippetController.createSnippet(createSnippetDTO, null)
        }
    }

    @Test
    fun `deleteSnippet should throw UnauthorizedException when JWT is null`() {
        assertThrows<UnauthorizedException> {
            snippetController.deleteSnippet("1", null)
        }
    }

    @Test
    fun `getAvailableUsers should throw UnauthorizedException when JWT is null`() {
        assertThrows<UnauthorizedException> {
            snippetController.getAvailableUsers(null, null)
        }
    }

    @Test
    fun `shareSnippet should throw UnauthorizedException when JWT is null`() {
        val shareSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO(
            snippetId = "1",
            targetUserId = "user2",
        )
        assertThrows<UnauthorizedException> {
            snippetController.shareSnippet(shareSnippetDTO, null)
        }
    }

    @Test
    fun `executeSnippet should throw UnauthorizedException when JWT is null`() {
        val executeSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO(
            inputs = listOf("a"),
        )
        assertThrows<UnauthorizedException> {
            snippetController.executeSnippet("1", executeSnippetDTO, null)
        }
    }

    @Test
    fun `updateSnippet should throw UnauthorizedException when JWT is null`() {
        val updateSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO(
            content = "new content",
        )
        assertThrows<UnauthorizedException> {
            snippetController.updateSnippet("1", updateSnippetDTO, null)
        }
    }

    @Test
    fun `createSnippet should throw UnauthorizedException for test user`() {
        val createSnippetDTO = CreateSnippetDTO(
            name = "New Snippet",
            description = "Description of new snippet",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "print('Hello, World!')",
            version = "1.0",
        )
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.createSnippet(createSnippetDTO, mockJwt)
        }
    }

    @Test
    fun `deleteSnippet should throw UnauthorizedException for test user`() {
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.deleteSnippet("1", mockJwt)
        }
    }

    @Test
    fun `getAvailableUsers should throw UnauthorizedException for test user`() {
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.getAvailableUsers(null, mockJwt)
        }
    }

    @Test
    fun `shareSnippet should throw UnauthorizedException for test user`() {
        val shareSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO(
            snippetId = "1",
            targetUserId = "user2",
        )
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.shareSnippet(shareSnippetDTO, mockJwt)
        }
    }

    @Test
    fun `executeSnippet should throw UnauthorizedException for test user`() {
        val executeSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO(
            inputs = listOf("a"),
        )
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.executeSnippet("1", executeSnippetDTO, mockJwt)
        }
    }

    @Test
    fun `updateSnippet should throw UnauthorizedException for test user`() {
        val updateSnippetDTO = com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO(
            content = "new content",
        )
        val mockJwt = mockk<Jwt> {
            every { subject } returns "test-user@example.com"
        }
        assertThrows<UnauthorizedException> {
            snippetController.updateSnippet("1", updateSnippetDTO, mockJwt)
        }
    }
}
