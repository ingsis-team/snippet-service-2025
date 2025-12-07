package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import com.ingsisteam.snippetservice2025.service.SnippetTestService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import java.time.LocalDateTime
import java.util.UUID

class SnippetTestControllerTest {

    private lateinit var snippetTestService: SnippetTestService
    private lateinit var snippetTestController: SnippetTestController
    private val userId = "test-user"
    private val mockJwt = mockk<Jwt>()

    @BeforeEach
    fun setUp() {
        snippetTestService = mockk(relaxed = true)
        snippetTestController = SnippetTestController(snippetTestService)
        every { mockJwt.subject } returns userId
    }

    @Test
    fun `createTest success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val createTestDTO = CreateTestDTO("test", listOf("input"), listOf("output"), TestStatus.VALID)
        val testResponseDTO = TestResponseDTO("id", snippetId, "test", listOf("input"), listOf("output"), TestStatus.VALID, LocalDateTime.now(), LocalDateTime.now())
        every { snippetTestService.createTest(snippetId, createTestDTO, userId) } returns testResponseDTO

        // When
        val response = snippetTestController.createTest(snippetId, createTestDTO, mockJwt)

        // Then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(testResponseDTO, response.body)
    }

    @Test
    fun `getTest success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val testId = UUID.randomUUID().toString()
        val testResponseDTO = TestResponseDTO("id", snippetId, "test", listOf("input"), listOf("output"), TestStatus.VALID, LocalDateTime.now(), LocalDateTime.now())
        every { snippetTestService.getTest(snippetId, testId, userId) } returns testResponseDTO

        // When
        val response = snippetTestController.getTest(snippetId, testId, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(testResponseDTO, response.body)
    }

    @Test
    fun `getTestsBySnippet success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val tests = listOf(TestResponseDTO("id", snippetId, "test", listOf("input"), listOf("output"), TestStatus.VALID, LocalDateTime.now(), LocalDateTime.now()))
        every { snippetTestService.getTestsBySnippet(snippetId, userId) } returns tests

        // When
        val response = snippetTestController.getTestsBySnippet(snippetId, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(tests, response.body)
    }

    @Test
    fun `deleteTest success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val testId = UUID.randomUUID().toString()

        // When
        val response = snippetTestController.deleteTest(snippetId, testId, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(true, response.body?.success)
    }

    @Test
    fun `executeTest success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val testId = UUID.randomUUID().toString()
        val result = mapOf("passed" to true)
        every { snippetTestService.executeTest(snippetId, testId, userId) } returns result

        // When
        val response = snippetTestController.executeTest(snippetId, testId, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(result, response.body)
    }

    @Test
    fun `runAllTests success`() {
        // Given
        val snippetId = UUID.randomUUID().toString()
        val responseDto = com.ingsisteam.snippetservice2025.model.dto.RunAllTestsResponseDTO(
            snippetId = snippetId,
            totalTests = 1,
            passedTests = 1,
            failedTests = 0,
            results = emptyList(),
        )
        every { snippetTestService.runAllTests(snippetId, userId) } returns responseDto

        // When
        val response = snippetTestController.runAllTests(snippetId, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
    }
}
