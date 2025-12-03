package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.TestNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import com.ingsisteam.snippetservice2025.repository.SnippetTestRepository
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockKExtension::class)
class SnippetTestServiceTest {

    @MockK
    private lateinit var snippetTestRepository: SnippetTestRepository

    @MockK
    private lateinit var snippetRepository: SnippetRepository

    @MockK
    private lateinit var permissionServiceConnector: PermissionServiceConnector

    @MockK
    private lateinit var printScriptServiceConnector: PrintScriptServiceConnector

    @MockK
    private lateinit var assetServiceConnector: AssetServiceConnector

    @InjectMockKs
    private lateinit var snippetTestService: SnippetTestService

    private fun buildSnippet(
        id: String,
        name: String = "testSnippet",
        description: String = "description",
        language: SnippetLanguage = SnippetLanguage.PRINTSCRIPT,
        userId: String,
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
    fun `test createTest success`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val createTestDTO = CreateTestDTO(
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
        )
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val savedTest = SnippetTest(
            id = "1",
            snippetId = snippetId,
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.existsBySnippetIdAndName(snippetId, "test1") } returns false
        every { snippetTestRepository.save(any()) } returns savedTest

        // When
        val result = snippetTestService.createTest(snippetId, createTestDTO, userId)

        // Then
        assertEquals("test1", result.name)
        assertEquals(snippetId, result.snippetId)
    }

    @Test
    fun `test createTest snippet not found`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val createTestDTO = CreateTestDTO(
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
        )

        every { snippetRepository.findById(snippetId) } returns Optional.empty()

        // When & Then
        assertThrows<SnippetNotFoundException> {
            snippetTestService.createTest(snippetId, createTestDTO, userId)
        }
    }

    @Test
    fun `test getTest success`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test

        // When
        val result = snippetTestService.getTest(snippetId, testId, userId)

        // Then
        assertEquals(testId, result.id)
        assertEquals("test1", result.name)
    }

    @Test
    fun `test getTest not found`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns null

        // When & Then
        assertThrows<TestNotFoundException> {
            snippetTestService.getTest(snippetId, testId, userId)
        }
    }

    @Test
    fun `test getTestsBySnippet success`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val snippet = buildSnippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
        val tests = listOf(
            SnippetTest(
                id = "1",
                snippetId = snippetId,
                name = "test1",
                inputs = listOf("input1"),
                expectedOutputs = listOf("output1"),
                expectedStatus = TestStatus.VALID,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
            SnippetTest(
                id = "2",
                snippetId = snippetId,
                name = "test2",
                inputs = listOf("input2"),
                expectedOutputs = listOf("output2"),
                expectedStatus = TestStatus.VALID,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findBySnippetId(snippetId) } returns tests

        // When
        val result = snippetTestService.getTestsBySnippet(snippetId, userId)

        // Then
        assertEquals(2, result.size)
        assertEquals("test1", result[0].name)
        assertEquals("test2", result[1].name)
    }

    @Test
    fun `test deleteTest success`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { snippetTestRepository.delete(test) } returns Unit

        // When
        snippetTestService.deleteTest(snippetId, testId, userId)

        // Then
        verify(exactly = 1) { snippetTestRepository.delete(test) }
    }

    @Test
    fun `test executeTest passed`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = emptyList(),
            expectedOutputs = listOf("hello"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { assetServiceConnector.getSnippet(snippetId) } returns "println(\"hello\");"

        // When
        val result = snippetTestService.executeTest(snippetId, testId, userId)

        // Then
        assertEquals(true, result["passed"])
        assertEquals(listOf("hello"), result["expectedOutputs"])
        assertEquals(listOf("hello"), result["actualOutputs"])
    }

    @Test
    fun `test executeTest failed`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = emptyList(),
            expectedOutputs = listOf("world"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { assetServiceConnector.getSnippet(snippetId) } returns "println(\"hello\");"

        // When
        val result = snippetTestService.executeTest(snippetId, testId, userId)

        // Then
        assertEquals(false, result["passed"])
        assertEquals(listOf("world"), result["expectedOutputs"])
        assertEquals(listOf("hello"), result["actualOutputs"])
    }

    @Test
    fun `test executeTest expected to fail and it does`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = emptyList(),
            expectedOutputs = listOf("world"),
            expectedStatus = TestStatus.INVALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { assetServiceConnector.getSnippet(snippetId) } returns "println(\"hello\");" // Mismatched output

        // When
        val result = snippetTestService.executeTest(snippetId, testId, userId)

        // Then
        assertEquals(true, result["passed"])
        assertEquals(listOf("world"), result["expectedOutputs"])
        assertEquals(listOf("hello"), result["actualOutputs"])
    }

    @Test
    fun `test executeTest expected to fail and execution fails`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = emptyList(),
            expectedOutputs = listOf("world"),
            expectedStatus = TestStatus.INVALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { assetServiceConnector.getSnippet(snippetId) } throws RuntimeException("Execution error")

        // When
        val result = snippetTestService.executeTest(snippetId, testId, userId)

        // Then
        assertEquals(true, result["passed"])
        assertEquals(true, result["executionFailed"])
    }

    @Test
    fun `test createTest with existing name`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val createTestDTO = CreateTestDTO(
            name = "test1",
            inputs = listOf("input1"),
            expectedOutputs = listOf("output1"),
            expectedStatus = TestStatus.VALID,
        )
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.existsBySnippetIdAndName(snippetId, "test1") } returns true

        // When & Then
        assertThrows<IllegalArgumentException> {
            snippetTestService.createTest(snippetId, createTestDTO, userId)
        }
    }

    @Test
    fun `test getTest no permission`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = Snippet(
            id = snippetId,
            name = "testSnippet",
            description = "description",
            language = SnippetLanguage.PRINTSCRIPT,
            userId = userId,
            version = "1.0",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns false

        // When & Then
        assertThrows<PermissionDeniedException> {
            snippetTestService.getTest(snippetId, testId, userId)
        }
    }

    @Test
    fun `test deleteTest not found`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns null

        // When & Then
        assertThrows<TestNotFoundException> {
            snippetTestService.deleteTest(snippetId, testId, userId)
        }
    }

    @Test
    fun `test executeTest snippet content not found`() {
        // Given
        val snippetId = "1"
        val userId = "user123"
        val testId = "1"
        val snippet = buildSnippet(
            id = snippetId,
            userId = userId,
        )
        val test = SnippetTest(
            id = testId,
            snippetId = snippetId,
            name = "test1",
            inputs = emptyList(),
            expectedOutputs = listOf("hello"),
            expectedStatus = TestStatus.VALID,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        every { snippetRepository.findById(snippetId) } returns Optional.of(snippet)
        every { permissionServiceConnector.hasPermission(snippetId, userId) } returns true
        every { snippetTestRepository.findByIdAndSnippetId(testId, snippetId) } returns test
        every { assetServiceConnector.getSnippet(snippetId) } returns null

        // When
        val result = snippetTestService.executeTest(snippetId, testId, userId)
        // Then
        assertEquals(false, result["passed"])
        assertEquals(true, result["executionFailed"])
    }
}
