package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.SyntaxValidationException
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponse
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionResponse
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationError
import com.ingsisteam.snippetservice2025.model.dto.external.ValidationResponse
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
@Timeout(value = 1, unit = TimeUnit.SECONDS)
class SnippetServiceTest {

    @MockK
    private lateinit var snippetRepository: SnippetRepository

    @MockK
    private lateinit var printScriptServiceConnector: PrintScriptServiceConnector

    @MockK
    private lateinit var permissionServiceConnector: PermissionServiceConnector

    @InjectMockKs
    private lateinit var snippetService: SnippetService

    @Test
    fun `test createSnippet success`() {
        // Given
        val createSnippetDTO = CreateSnippetDTO(
            name = "testSnippet",
            description = "description",
            content = "content",
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"
        val savedSnippet = Snippet(
            id = 1,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())
        val permissionResponse = PermissionResponse(
            id = 1,
            snippetId = 1,
            userId = userId,
            role = "OWNER",
            createdAt = LocalDateTime.now().toString(),
        )

        every { snippetRepository.existsByUserIdAndName(userId, "testSnippet") } returns false
        every {
            printScriptServiceConnector.validateSnippet(
                content = "content",
                language = SnippetLanguage.PRINTSCRIPT.name,
                version = "1.0",
            )
        } returns validationResponse
        every { snippetRepository.save(any()) } returns savedSnippet
        every {
            permissionServiceConnector.createPermission(
                snippetId = any(),
                userId = any(),
                role = any(),
            )
        } returns permissionResponse

        // When
        val result = snippetService.createSnippet(createSnippetDTO, userId)

        // Then
        assertEquals("testSnippet", result.name)
        assertEquals("content", result.content)
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { permissionServiceConnector.createPermission(1, userId, "OWNER") }
    }

    @Test
    fun `test createSnippet duplicate name`() {
        // Given
        val createSnippetDTO = CreateSnippetDTO(
            name = "testSnippet",
            description = "description",
            content = "content",
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"

        every { snippetRepository.existsByUserIdAndName(userId, "testSnippet") } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.createSnippet(createSnippetDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test getSnippet with permission`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)

        // When
        val result = snippetService.getSnippet(snippetId, userId)

        // Then
        assertEquals(snippetId, result.id)
        assertEquals("testSnippet", result.name)
    }

    @Test
    fun `test getSnippet without permission`() {
        // Given
        val snippetId = 1L
        val userId = "user123"

        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns false

        // When & Then
        assertThrows<NoSuchElementException> {
            snippetService.getSnippet(snippetId, userId)
        }
    }

    @Test
    fun `test deleteSnippet as owner`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(hasPermission = true, role = "OWNER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck
        every { snippetRepository.existsById(snippetId) } returns true
        every { snippetRepository.deleteById(snippetId) } returns Unit
        every { permissionServiceConnector.deleteSnippetPermissions(snippetId) } returns Unit

        // When
        snippetService.deleteSnippet(snippetId, userId)

        // Then
        verify(exactly = 1) { snippetRepository.deleteById(snippetId) }
        verify(exactly = 1) { permissionServiceConnector.deleteSnippetPermissions(snippetId) }
    }

    @Test
    fun `test deleteSnippet not as owner`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(hasPermission = true, role = "READER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck

        // When & Then
        assertThrows<IllegalAccessException> {
            snippetService.deleteSnippet(snippetId, userId)
        }
    }

    // --- Tests for createSnippetFromFile ---

    @Test
    fun `test createSnippetFromFile success`() {
        // Given
        val fileContent = "println(\"hello from file\")"
        val mockFile = MockMultipartFile("file", "test.kts", "text/plain", fileContent.toByteArray())
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "fileSnippet",
            description = "description from file",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"
        val savedSnippet = Snippet(
            id = 2,
            name = "fileSnippet",
            description = "description from file",
            language = SnippetLanguage.PRINTSCRIPT,
            content = fileContent,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())
        val permissionResponse = PermissionResponse(
            id = 2,
            snippetId = 2,
            userId = userId,
            role = "OWNER",
            createdAt = LocalDateTime.now().toString(),
        )

        every { snippetRepository.existsByUserIdAndName(userId, "fileSnippet") } returns false
        every {
            printScriptServiceConnector.validateSnippet(
                content = fileContent,
                language = SnippetLanguage.PRINTSCRIPT.name,
                version = "1.0",
            )
        } returns validationResponse
        every { snippetRepository.save(any()) } returns savedSnippet
        every {
            permissionServiceConnector.createPermission(
                snippetId = any(),
                userId = any(),
                role = any(),
            )
        } returns permissionResponse
        every { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) } returns Unit

        // When
        val result = snippetService.createSnippetFromFile(createSnippetFileDTO, userId)

        // Then
        assertEquals("fileSnippet", result.name)
        assertEquals(fileContent, result.content)
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { permissionServiceConnector.createPermission(2, userId, "OWNER") }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) }
    }

    @Test
    fun `test createSnippetFromFile duplicate name`() {
        // Given
        val fileContent = "println(\"hello from file\")"
        val mockFile = MockMultipartFile("file", "test.kts", "text/plain", fileContent.toByteArray())
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "fileSnippet",
            description = "description from file",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"

        every { snippetRepository.existsByUserIdAndName(userId, "fileSnippet") } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
        verify(exactly = 0) { permissionServiceConnector.createPermission(any(), any(), any()) }
    }

    @Test
    fun `test createSnippetFromFile empty file`() {
        // Given
        val mockFile = MockMultipartFile("file", "empty.kts", "text/plain", ByteArray(0))
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "emptyFileSnippet",
            description = "empty file",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"

        every { snippetRepository.existsByUserIdAndName(userId, "emptyFileSnippet") } returns false

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test createSnippetFromFile syntax validation failure`() {
        // Given
        val fileContent = "invalid code"
        val mockFile = MockMultipartFile("file", "invalid.kts", "text/plain", fileContent.toByteArray())
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "invalidSyntaxSnippet",
            description = "invalid syntax",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"
        val validationResponse = ValidationResponse(
            isValid = false,
            errors = listOf(ValidationError("rule", 1, 1, "Invalid syntax")),
        )

        every { snippetRepository.existsByUserIdAndName(userId, "invalidSyntaxSnippet") } returns false
        every {
            printScriptServiceConnector.validateSnippet(
                content = fileContent,
                language = SnippetLanguage.PRINTSCRIPT.name,
                version = "1.0",
            )
        } returns validationResponse

        // When & Then
        assertThrows<SyntaxValidationException> {
            snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test createSnippetFromFile permission creation failure`() {
        // Given
        val fileContent = "println(\"hello from file\")"
        val mockFile = MockMultipartFile("file", "test.kts", "text/plain", fileContent.toByteArray())
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "fileSnippet",
            description = "description from file",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"
        val savedSnippet = Snippet(
            id = 2,
            name = "fileSnippet",
            description = "description from file",
            language = SnippetLanguage.PRINTSCRIPT,
            content = fileContent,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())

        every { snippetRepository.existsByUserIdAndName(userId, "fileSnippet") } returns false
        every {
            printScriptServiceConnector.validateSnippet(
                content = fileContent,
                language = SnippetLanguage.PRINTSCRIPT.name,
                version = "1.0",
            )
        } returns validationResponse
        every { snippetRepository.save(any()) } returns savedSnippet
        every {
            permissionServiceConnector.createPermission(
                snippetId = any(),
                userId = any(),
                role = any(),
            )
        } throws RuntimeException("Permission service error") // Simulate failure
        every { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) } returns Unit

        // When
        val result = snippetService.createSnippetFromFile(createSnippetFileDTO, userId)

        // Then
        assertEquals("fileSnippet", result.name)
        assertEquals(fileContent, result.content)
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { permissionServiceConnector.createPermission(2, userId, "OWNER") }
        // Verify that automatic triggers are still called even if permission creation fails
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) }
    }

    // --- Tests for getAllSnippets ---

    @Test
    fun `test getAllSnippets without name filter, with permissions`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = Snippet(3, "Own Snippet 1", "desc", SnippetLanguage.PRINTSCRIPT, "content1", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val ownSnippet2 = Snippet(4, "Own Snippet 2", "desc", SnippetLanguage.PRINTSCRIPT, "content2", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val sharedSnippet1 = Snippet(5, "Shared Snippet 1", "desc", SnippetLanguage.PRINTSCRIPT, "content3", "otherUser", "1.0", LocalDateTime.now(), LocalDateTime.now())

        val permittedIds = listOf(ownSnippet1.id, ownSnippet2.id, sharedSnippet1.id)
        val allSnippets = listOf(ownSnippet1, ownSnippet2, sharedSnippet1)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns permittedIds
        every { snippetRepository.findAllById(permittedIds) } returns allSnippets

        // When
        val result = snippetService.getAllSnippets(userId)

        // Then
        assertEquals(3, result.size)
        assertTrue(result.any { it.id == ownSnippet1.id })
        assertTrue(result.any { it.id == ownSnippet2.id })
        assertTrue(result.any { it.id == sharedSnippet1.id })
    }

    @Test
    fun `test getAllSnippets with name filter, with permissions`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = Snippet(3, "Own Snippet A", "desc", SnippetLanguage.PRINTSCRIPT, "content1", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val ownSnippet2 = Snippet(4, "Own Snippet B", "desc", SnippetLanguage.PRINTSCRIPT, "content2", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val sharedSnippet1 = Snippet(5, "Shared Snippet C", "desc", SnippetLanguage.PRINTSCRIPT, "content3", "otherUser", "1.0", LocalDateTime.now(), LocalDateTime.now())

        val permittedIds = listOf(ownSnippet1.id, ownSnippet2.id, sharedSnippet1.id)
        val allSnippets = listOf(ownSnippet1, ownSnippet2, sharedSnippet1)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns permittedIds
        every { snippetRepository.findAllById(permittedIds) } returns allSnippets

        // When
        val result = snippetService.getAllSnippets(userId, "snippet A")

        // Then
        assertEquals(1, result.size)
        assertEquals(ownSnippet1.id, result[0].id)
    }

    @Test
    fun `test getAllSnippets without name filter, no permissions (empty permittedSnippetIds)`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = Snippet(3, "Own Snippet 1", "desc", SnippetLanguage.PRINTSCRIPT, "content1", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val ownSnippet2 = Snippet(4, "Own Snippet 2", "desc", SnippetLanguage.PRINTSCRIPT, "content2", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val otherUserSnippet = Snippet(6, "Other User Snippet", "desc", SnippetLanguage.PRINTSCRIPT, "content4", "otherUser", "1.0", LocalDateTime.now(), LocalDateTime.now())

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns emptyList() // Simulate no permissions
        every { snippetRepository.findByUserId(userId) } returns listOf(ownSnippet1, ownSnippet2)

        // When
        val result = snippetService.getAllSnippets(userId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == userId })
        assertFalse(result.any { it.id == otherUserSnippet.id })
    }

    @Test
    fun `test getAllSnippets with name filter, no permissions (empty permittedSnippetIds)`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = Snippet(3, "Own Snippet A", "desc", SnippetLanguage.PRINTSCRIPT, "content1", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns emptyList()
        every { snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, "snippet A") } returns listOf(ownSnippet1)

        // When
        val result = snippetService.getAllSnippets(userId, "snippet A")

        // Then
        assertEquals(1, result.size)
        assertEquals(ownSnippet1.id, result[0].id)
    }

    @Test
    fun `test getAllSnippets when permissionServiceConnector getUserPermittedSnippets throws exception`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = Snippet(3, "Own Snippet 1", "desc", SnippetLanguage.PRINTSCRIPT, "content1", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())
        val ownSnippet2 = Snippet(4, "Own Snippet 2", "desc", SnippetLanguage.PRINTSCRIPT, "content2", userId, "1.0", LocalDateTime.now(), LocalDateTime.now())

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } throws RuntimeException("Permission service unavailable")
        every { snippetRepository.findByUserId(userId) } returns listOf(ownSnippet1, ownSnippet2) // Should fall back to own snippets

        // When
        val result = snippetService.getAllSnippets(userId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == userId })
    }

    @Test
    fun `test getAllSnippets returns empty list if no snippets found for user`() {
        // Given
        val userId = "nonExistentUser"

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns emptyList()
        every { snippetRepository.findByUserId(userId) } returns emptyList()

        // When
        val result = snippetService.getAllSnippets(userId)

        // Then
        assertEquals(0, result.size)
    }

    // --- Tests for updateSnippetFromFile ---

    @Test
    fun `test updateSnippetFromFile success`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updatedFileContent = "println(\"new content from file\")"
        val mockFile = MockMultipartFile("file", "updated.kts", "text/plain", updatedFileContent.toByteArray())
        val updateSnippetFileDTO = UpdateSnippetFileDTO(
            file = mockFile,
        )
        val updatedSnippet = originalSnippet.copy(content = updatedFileContent)
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every {
            printScriptServiceConnector.validateSnippet(
                content = updatedFileContent,
                language = originalSnippet.language.name,
                version = originalSnippet.version,
            )
        } returns validationResponse
        every { snippetRepository.save(any<Snippet>()) } returns updatedSnippet // Use any<Snippet>()
        every { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) } returns Unit

        // When
        val result = snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)

        // Then
        assertEquals(snippetId, result.id)
        assertEquals(updatedFileContent, result.content)
        verify(exactly = 1) { snippetRepository.save(any<Snippet>()) } // Use any<Snippet>()
        verify(exactly = 1) { permissionServiceConnector.hasWritePermission(snippetId, userId) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) }
    }

    @Test
    fun `test updateSnippetFromFile snippet not found`() {
        // Given
        val snippetId = 999L
        val userId = "user123"
        val mockFile = MockMultipartFile("file", "updated.kts", "text/plain", "new content".toByteArray())
        val updateSnippetFileDTO = UpdateSnippetFileDTO(file = mockFile)

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippetFromFile no write permission`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val mockFile = MockMultipartFile("file", "updated.kts", "text/plain", "new content".toByteArray())
        val updateSnippetFileDTO = UpdateSnippetFileDTO(file = mockFile)

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns false

        // When & Then
        assertThrows<IllegalAccessException> {
            snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippetFromFile empty file`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val mockFile = MockMultipartFile("file", "empty.kts", "text/plain", ByteArray(0))
        val updateSnippetFileDTO = UpdateSnippetFileDTO(file = mockFile)

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippetFromFile syntax validation failure`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updatedFileContent = "invalid code"
        val mockFile = MockMultipartFile("file", "invalid.kts", "text/plain", updatedFileContent.toByteArray())
        val updateSnippetFileDTO = UpdateSnippetFileDTO(file = mockFile)
        val validationResponse = ValidationResponse(
            isValid = false,
            errors = listOf(ValidationError("rule", 1, 1, "Invalid syntax")),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every {
            printScriptServiceConnector.validateSnippet(
                content = updatedFileContent,
                language = originalSnippet.language.name,
                version = originalSnippet.version,
            )
        } returns validationResponse

        // When & Then
        assertThrows<SyntaxValidationException> {
            snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    // --- Tests for updateSnippet (from editor) ---

    @Test
    fun `test updateSnippet success`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updatedContent = "println(\"new content from editor\")"
        val updateSnippetDTO = UpdateSnippetDTO(content = updatedContent)
        val updatedSnippet = originalSnippet.copy(content = updatedContent)
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every {
            printScriptServiceConnector.validateSnippet(
                content = updatedContent,
                language = originalSnippet.language.name,
                version = originalSnippet.version,
            )
        } returns validationResponse
        every { snippetRepository.save(any<Snippet>()) } returns updatedSnippet
        every { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) } returns Unit
        every { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) } returns Unit

        // When
        val result = snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)

        // Then
        assertEquals(snippetId, result.id)
        assertEquals(updatedContent, result.content)
        assertEquals("originalSnippet", result.name)
        verify(exactly = 1) { snippetRepository.save(any<Snippet>()) }
        verify(exactly = 1) { permissionServiceConnector.hasWritePermission(snippetId, userId) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) }
        verify(exactly = 1) { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) }
    }

    @Test
    fun `test updateSnippet snippet not found`() {
        // Given
        val snippetId = 999L
        val userId = "user123"
        val updateSnippetDTO = UpdateSnippetDTO(content = "new content")

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        // When & Then
        assertThrows<NoSuchElementException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippet no write permission`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updateSnippetDTO = UpdateSnippetDTO(content = "new content")

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns false

        // When & Then
        assertThrows<IllegalAccessException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippet empty content`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updateSnippetDTO = UpdateSnippetDTO(content = "")

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `test updateSnippet syntax validation failure`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val originalSnippet = Snippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "old content",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val updatedContent = "invalid code"
        val updateSnippetDTO = UpdateSnippetDTO(content = updatedContent)
        val validationResponse = ValidationResponse(
            isValid = false,
            errors = listOf(ValidationError("rule", 1, 1, "Invalid syntax")),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every {
            printScriptServiceConnector.validateSnippet(
                content = updatedContent,
                language = originalSnippet.language.name,
                version = originalSnippet.version,
            )
        } returns validationResponse

        // When & Then
        assertThrows<SyntaxValidationException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    // --- Tests for executeSnippet ---

    @Test
    fun `test executeSnippet success with println only`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Hello\");\nprintln('World');",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertEquals(listOf("Hello", "World"), result.outputs)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test executeSnippet success with readInput only`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "readInput();\nreadInput();",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = listOf("input1", "input2"))

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertTrue(result.outputs.isEmpty())
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test executeSnippet success with println and readInput`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Enter value:\");\nreadInput();\nprintln(\"Result:\");\nprintln(readInput());",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = listOf("user_input_1", "user_input_2"))

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertEquals(listOf("Enter value:", "Result:", "readInput()"), result.outputs) // The second readInput value is not executed, but printed as literal. "Result:" is also printed.
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test executeSnippet snippet not found`() {
        // Given
        val snippetId = 999L
        val userId = "user123"
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        // When & Then
        assertThrows<SnippetNotFoundException> {
            snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)
        }
    }

    @Test
    fun `test executeSnippet no read permission`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Hello\");",
            userId = "otherUser",
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns false

        // When & Then
        assertThrows<PermissionDeniedException> {
            snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)
        }
    }

    @Test
    fun `test executeSnippet missing input`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "readInput();\nreadInput();",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = listOf("input1")) // Missing one input

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertTrue(result.outputs.isEmpty())
        assertEquals(1, result.errors.size)
        assertTrue(result.errors[0].contains("Input required but not provided"))
    }

    @Test
    fun `test executeSnippet handles arbitrary text in println`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(someVariable);\nprintln(\"literal string\");\nprintln('another literal');",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertEquals(listOf("someVariable", "literal string", "another literal"), result.outputs)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `test executeSnippet handles complex content with errors`() {
        // Given
        val snippetId = 1L
        val userId = "user123"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Start\");\nreadInput();\nprintln(nonExistentFunction());\nreadInput();\nprintln(\"End\");",
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = listOf("input1")) // Only one input provided

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true

        // When
        val result = snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)

        // Then
        assertEquals(listOf("Start", "nonExistentFunction()", "End"), result.outputs) // "nonExistentFunction()" is printed as a literal, and the loop continues to "End"
        assertEquals(1, result.errors.size) // Only one error for missing input
        assertTrue(result.errors[0].contains("Input required but not provided at line: readInput();"))
    }
}
