package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
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
import java.io.IOException
import java.time.LocalDateTime
import java.util.Optional
import java.util.concurrent.TimeUnit

@ExtendWith(MockKExtension::class)
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class SnippetServiceTest {

    @MockK
    private lateinit var snippetRepository: SnippetRepository

    @MockK
    private lateinit var printScriptServiceConnector: PrintScriptServiceConnector

    @MockK
    private lateinit var permissionServiceConnector: PermissionServiceConnector

    @MockK
    private lateinit var assetServiceConnector: AssetServiceConnector

    @InjectMockKs
    private lateinit var snippetService: SnippetService

    private fun buildSnippet(
        id: String = "snippet-id",
        name: String = "testSnippet",
        description: String = "description",
        language: SnippetLanguage = SnippetLanguage.PRINTSCRIPT,
        userId: String = "user123",
        version: String = "1.0",
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now(),
    ): Snippet = Snippet(
        id = id,
        name = name,
        description = description,
        language = language,
        userId = userId,
        version = version,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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
        val savedSnippet = buildSnippet(
            id = "1",
            name = "testSnippet",
            description = "description",
            userId = userId,
            version = "1.0",
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())
        val permissionResponse = PermissionResponse(
            id = "1",
            snippet_id = "1",
            user_id = userId,
            role = "OWNER",
            created_at = LocalDateTime.now().toString(),
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
        every { assetServiceConnector.storeSnippet(any(), any()) } returns true
        every { assetServiceConnector.getSnippet("1") } returns "content"
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
        val result = snippetService.createSnippet(createSnippetDTO, userId)

        // Then
        assertEquals("testSnippet", result.name)
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { assetServiceConnector.storeSnippet("1", "content") }
        verify(exactly = 1) { permissionServiceConnector.createPermission("1", userId, "OWNER") }
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
        val snippetId = "1"
        val userId = "user123"
        val snippet = buildSnippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            userId = userId,
        )

        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(snippetId) } returns "content"

        // When
        val result = snippetService.getSnippet(snippetId, userId)

        // Then
        assertEquals(snippetId, result.id)
        assertEquals("testSnippet", result.name)
        assertEquals("content", result.content)
    }

    @Test
    fun `test getSnippet without permission`() {
        // Given
        val snippetId = "1"
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
        val snippetId = "1"
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck
        every { snippetRepository.existsById(snippetId) } returns true
        every { snippetRepository.deleteById(snippetId) } returns Unit
        every { assetServiceConnector.deleteSnippet(snippetId) } returns true
        every { permissionServiceConnector.deleteSnippetPermissions(snippetId) } returns Unit

        // When
        snippetService.deleteSnippet(snippetId, userId)

        // Then
        verify(exactly = 1) { snippetRepository.deleteById(snippetId) }
        verify(exactly = 1) { assetServiceConnector.deleteSnippet(snippetId) }
        verify(exactly = 1) { permissionServiceConnector.deleteSnippetPermissions(snippetId) }
    }

    @Test
    fun `test deleteSnippet not as owner`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(has_permission = true, role = "READER")

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
        val savedSnippet = buildSnippet(
            id = "2",
            name = "fileSnippet",
            description = "description from file",
            userId = userId,
            version = "1.0",
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())
        val permissionResponse = PermissionResponse(
            id = "2",
            snippet_id = "2",
            user_id = userId,
            role = "OWNER",
            created_at = LocalDateTime.now().toString(),
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
        every { assetServiceConnector.storeSnippet(any(), any()) } returns true
        every { assetServiceConnector.getSnippet("2") } returns fileContent
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
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { assetServiceConnector.storeSnippet("2", fileContent) }
        verify(exactly = 1) { permissionServiceConnector.createPermission("2", userId, "OWNER") }
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
            id = "2",
            name = "fileSnippet",
            description = "description from file",
            language = SnippetLanguage.PRINTSCRIPT,
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
        every { assetServiceConnector.storeSnippet(any(), any()) } returns true
        every {
            permissionServiceConnector.createPermission(
                snippetId = "2",
                userId = userId,
                role = "OWNER",
            )
        } throws RuntimeException("Permission service error") // Simulate failure
        every { assetServiceConnector.deleteSnippet("2") } returns true
        every { snippetRepository.deleteById("2") } returns Unit

        // When & Then
        assertThrows<RuntimeException> {
            snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        }

        // Then
        verify(exactly = 1) { snippetRepository.save(any()) }
        verify(exactly = 1) { permissionServiceConnector.createPermission("2", userId, "OWNER") }
        verify(exactly = 1) { snippetRepository.deleteById("2") }
        verify(exactly = 0) { printScriptServiceConnector.triggerAutomaticFormatting(any(), any(), any()) }
        verify(exactly = 0) { printScriptServiceConnector.triggerAutomaticLinting(any(), any(), any()) }
        verify(exactly = 0) { printScriptServiceConnector.triggerAutomaticTesting(any(), any(), any()) }
    }

    @Test
    fun `createSnippetFromFile should throw IllegalArgumentException when file content reading fails`() {
        // Given
        val mockFile = object : MockMultipartFile("file", "error.kts", "text/plain", "dummy".toByteArray()) {
            override fun getBytes(): ByteArray {
                throw IOException("Simulated file read error")
            }
        }
        val createSnippetFileDTO = CreateSnippetFileDTO(
            name = "fileReadErrorSnippet",
            description = "description",
            file = mockFile,
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
        )
        val userId = "user123"

        every { snippetRepository.existsByUserIdAndName(userId, "fileReadErrorSnippet") } returns false

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        }.also {
            assertTrue(it.message?.contains("Error al leer el archivo") == true)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    // --- Tests for getAllSnippets ---

    @Test
    fun `test getAllSnippets without name filter, with permissions`() {
        // Given
        val userId = "user123"
        val ownSnippet1 = buildSnippet(id = "3", name = "Own Snippet 1", description = "desc", userId = userId)
        val ownSnippet2 = buildSnippet(id = "4", name = "Own Snippet 2", description = "desc", userId = userId)
        val sharedSnippet1 = buildSnippet(id = "5", name = "Shared Snippet 1", description = "desc", userId = "otherUser")

        val permittedIds = listOf(ownSnippet1.id, ownSnippet2.id, sharedSnippet1.id)
        val allSnippets = listOf(ownSnippet1, ownSnippet2, sharedSnippet1)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns permittedIds
        every { snippetRepository.findAllById(permittedIds) } returns allSnippets
        every { assetServiceConnector.getSnippet(any()) } returns "content"

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
        val ownSnippet1 = buildSnippet(id = "3", name = "Own Snippet A", description = "desc", userId = userId)
        val ownSnippet2 = buildSnippet(id = "4", name = "Own Snippet B", description = "desc", userId = userId)
        val sharedSnippet1 = buildSnippet(id = "5", name = "Shared Snippet C", description = "desc", userId = "otherUser")

        val permittedIds = listOf(ownSnippet1.id, ownSnippet2.id, sharedSnippet1.id)
        val allSnippets = listOf(ownSnippet1, ownSnippet2, sharedSnippet1)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns permittedIds
        every { snippetRepository.findAllById(permittedIds) } returns allSnippets
        every { assetServiceConnector.getSnippet(any()) } returns "content"

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
        val ownSnippet1 = buildSnippet(id = "3", name = "Own Snippet 1", description = "desc", userId = userId)
        val ownSnippet2 = buildSnippet(id = "4", name = "Own Snippet 2", description = "desc", userId = userId)
        val otherUserSnippet = buildSnippet(id = "6", name = "Other User Snippet", description = "desc", userId = "otherUser")

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns emptyList() // Simulate no permissions
        every { snippetRepository.findByUserId(userId) } returns listOf(ownSnippet1, ownSnippet2)
        every { assetServiceConnector.getSnippet(any()) } returns "content"

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
        val ownSnippet1 = buildSnippet(id = "3", name = "Own Snippet A", description = "desc", userId = userId)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } returns emptyList()
        every { snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, "snippet A") } returns listOf(ownSnippet1)
        every { assetServiceConnector.getSnippet(any()) } returns "content"

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
        val ownSnippet1 = buildSnippet(id = "3", name = "Own Snippet 1", description = "desc", userId = userId)
        val ownSnippet2 = buildSnippet(id = "4", name = "Own Snippet 2", description = "desc", userId = userId)

        every { permissionServiceConnector.getUserPermittedSnippets(userId) } throws RuntimeException("Permission service unavailable")
        every { snippetRepository.findByUserId(userId) } returns listOf(ownSnippet1, ownSnippet2) // Should fall back to own snippets
        every { assetServiceConnector.getSnippet(any()) } returns "content"

        // When
        val result = snippetService.getAllSnippets(userId)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.userId == userId })
    }

    // --- Tests for updateSnippetFromFile ---

    @Test
    fun `test updateSnippetFromFile snippet not found`() {
        // Given
        val snippetId = "999"
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
        )
        val updatedContent = "println(\"new content from editor\")"
        val updateSnippetDTO = UpdateSnippetDTO(content = updatedContent)
        val updatedSnippet = originalSnippet.copy()
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
        every { assetServiceConnector.updateSnippet(snippetId, updatedContent) } returns true
        every { assetServiceConnector.getSnippet(snippetId) } returns updatedContent
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
        val snippetId = "999"
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
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
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
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

    @Test
    fun `test updateSnippet with blank name should not update name`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalName",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
        )
        val updateSnippetDTO = UpdateSnippetDTO(name = "   ") // Blank name
        val originalContent = "println(\"original content\")"

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every { snippetRepository.existsByUserIdAndName(userId, "   ") } returns false // No duplicate name
        every { snippetRepository.save(any<Snippet>()) } answers { it.invocation.args[0] as Snippet } // Mock save to return the same snippet
        every { assetServiceConnector.getSnippet(snippetId) } returns originalContent

        // When
        val result = snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)

        // Then
        assertEquals("originalName", result.name) // Name should remain unchanged
        verify(exactly = 1) { snippetRepository.save(any<Snippet>()) } // Save should be called
    }

    @Test
    fun `test updateSnippet with blank description should update description`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalName",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
        )
        val updateSnippetDTO = UpdateSnippetDTO(description = "   ") // Blank description
        val originalContent = "println(\"original content\")"
        val updatedSnippet = originalSnippet.copy(description = "   ")

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every { snippetRepository.save(any<Snippet>()) } answers { it.invocation.args[0] as Snippet }
        every { assetServiceConnector.getSnippet(snippetId) } returns originalContent

        // When
        val result = snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)

        // Then
        assertEquals("originalName", result.name) // Name should remain unchanged
        assertEquals("   ", result.description) // Description should be updated to blank
        verify(exactly = 1) { snippetRepository.save(any<Snippet>()) }
    }

    @Test
    fun `test updateSnippet duplicate name for another snippet`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalName",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
        )
        val newName = "existingName" // Name already exists for another snippet
        val updateSnippetDTO = UpdateSnippetDTO(name = newName)

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every { snippetRepository.existsByUserIdAndName(userId, newName) } returns true // Simulate duplicate name
        every { snippetRepository.save(any<Snippet>()) } answers { it.invocation.args[0] as Snippet }

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }

        verify(exactly = 0) { snippetRepository.save(any<Snippet>()) } // Should not save
    }

    @Test
    fun `test updateSnippet when no fields are provided`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalName",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
        )
        val updateSnippetDTO = UpdateSnippetDTO(content = null, name = null, description = null)

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }

        verify(exactly = 0) { snippetRepository.save(any<Snippet>()) } // Should not save
    }

    @Test
    fun `test updateSnippet asset service update fails`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalName",
            description = "originalDescription",
            userId = userId,
            version = "1.0",
        )
        val updatedContent = "println(\"new content from editor\")"
        val updateSnippetDTO = UpdateSnippetDTO(content = updatedContent)
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every { printScriptServiceConnector.validateSnippet(any(), any(), any()) } returns validationResponse
        every { assetServiceConnector.updateSnippet(snippetId, updatedContent) } returns false // Simulate failure

        // When & Then
        assertThrows<RuntimeException> {
            snippetService.updateSnippet(snippetId, updateSnippetDTO, userId)
        }

        verify(exactly = 0) { snippetRepository.save(any<Snippet>()) } // Should not save
    }

    @Test
    fun `test deleteSnippet non existent snippet`() {
        // Given
        val snippetId = "999"
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck
        every { snippetRepository.existsById(snippetId) } returns false // Simulate non-existent snippet

        // When & Then
        assertThrows<NoSuchElementException> {
            snippetService.deleteSnippet(snippetId, userId)
        }

        verify(exactly = 0) { snippetRepository.deleteById(any()) }
    }

    @Test
    fun `test deleteSnippet asset service deletion fails but still completes`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck
        every { snippetRepository.existsById(snippetId) } returns true
        every { snippetRepository.deleteById(snippetId) } returns Unit
        every { assetServiceConnector.deleteSnippet(snippetId) } throws RuntimeException("Asset service deletion failed") // Simulate failure
        every { permissionServiceConnector.deleteSnippetPermissions(snippetId) } returns Unit

        // When
        snippetService.deleteSnippet(snippetId, userId)

        // Then
        verify(exactly = 1) { snippetRepository.deleteById(snippetId) } // Should still delete from DB
        verify(exactly = 1) { assetServiceConnector.deleteSnippet(snippetId) } // Should attempt to delete from asset service
        verify(exactly = 1) { permissionServiceConnector.deleteSnippetPermissions(snippetId) } // Should still attempt to delete permissions
    }

    @Test
    fun `test deleteSnippet permission service deletion fails but still completes`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val permissionCheck = PermissionCheckResponse(has_permission = true, role = "OWNER")

        every { permissionServiceConnector.checkPermission(snippetId, userId) } returns permissionCheck
        every { snippetRepository.existsById(snippetId) } returns true
        every { snippetRepository.deleteById(snippetId) } returns Unit
        every { assetServiceConnector.deleteSnippet(snippetId) } returns true
        every { permissionServiceConnector.deleteSnippetPermissions(snippetId) } throws RuntimeException("Permission service deletion failed") // Simulate failure

        // When
        snippetService.deleteSnippet(snippetId, userId)

        // Then
        verify(exactly = 1) { snippetRepository.deleteById(snippetId) } // Should still delete from DB
        verify(exactly = 1) { assetServiceConnector.deleteSnippet(snippetId) } // Should still delete from asset service
        verify(exactly = 1) { permissionServiceConnector.deleteSnippetPermissions(snippetId) } // Should still attempt to delete permissions
    }

    @Test
    fun `test executeSnippet asset service returns null content`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val snippet = buildSnippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            userId = userId,
            version = "1.0",
        )
        val executeSnippetDTO = ExecuteSnippetDTO(inputs = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { assetServiceConnector.getSnippet(snippetId) } returns null // Simulate asset service returning null

        // When & Then
        assertThrows<RuntimeException> {
            snippetService.executeSnippet(snippetId, executeSnippetDTO, userId)
        }
    }

    @Test
    fun `createSnippet should throw SyntaxValidationException if external validation is invalid with empty errors`() {
        // Given
        val createSnippetDTO = CreateSnippetDTO("test", "desc", SnippetLanguage.PRINTSCRIPT, "invalid content", "1.0")
        val userId = "user123"
        val validationResponse = ValidationResponse(isValid = false, errors = emptyList()) // Invalid but empty errors

        every { snippetRepository.existsByUserIdAndName(userId, "test") } returns false
        every { printScriptServiceConnector.validateSnippet(any(), any(), any()) } returns validationResponse

        // When & Then
        val exception = assertThrows<SyntaxValidationException> {
            snippetService.createSnippet(createSnippetDTO, userId)
        }
        assertEquals("Error en la validación del snippet", exception.message)
        assertEquals("VALIDATION_ERROR", exception.rule)
        assertEquals(1, exception.line)
        assertEquals(1, exception.column)
        verify(exactly = 0) { snippetRepository.save(any()) }
    }

    @Test
    fun `getSnippet should return empty content if asset service getSnippet returns null`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val snippet = buildSnippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            userId = userId,
            version = "1.0",
        )

        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(snippetId) } returns null // Simulate asset service returning null

        // When
        val result = snippetService.getSnippet(snippetId, userId)

        // Then
        assertEquals(snippetId, result.id)
        assertEquals("testSnippet", result.name)
        assertTrue(result.content.isEmpty()) // Content should be an empty string
    }

    @Test
    fun `updateSnippetFromFile asset service update fails`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val originalSnippet = buildSnippet(
            id = snippetId,
            name = "originalSnippet",
            description = "desc",
            userId = userId,
            version = "1.0",
        )
        val updatedFileContent = "println(\"new content from file\")"
        val mockFile = MockMultipartFile("file", "updated.kts", "text/plain", updatedFileContent.toByteArray())
        val updateSnippetFileDTO = UpdateSnippetFileDTO(
            file = mockFile,
        )
        val validationResponse = ValidationResponse(isValid = true, errors = emptyList())

        every { snippetRepository.findById(snippetId) } returns Optional.of(originalSnippet)
        every { permissionServiceConnector.hasWritePermission(snippetId, userId) } returns true
        every { printScriptServiceConnector.validateSnippet(any(), any(), any()) } returns validationResponse
        every { assetServiceConnector.updateSnippet(snippetId, updatedFileContent) } returns false // Simulate failure

        // When & Then
        assertThrows<RuntimeException> {
            snippetService.updateSnippetFromFile(snippetId, updateSnippetFileDTO, userId)
        }.also {
            assertTrue(it.message?.contains("No se pudo actualizar el contenido del snippet en el servicio de assets") == true)
        }
        verify(exactly = 0) { snippetRepository.save(any()) }
    }
}
