package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.TestNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import com.ingsisteam.snippetservice2025.service.SnippetTestService
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

class SnippetTestControllerTest {

    private lateinit var snippetTestService: SnippetTestService
    private lateinit var snippetTestController: SnippetTestController

    private val SNIPPET_ID = "1"
    private val TEST_ID = "100"
    private val userId = "another-user@example.com"

    @BeforeEach
    fun setUp() {
        io.mockk.clearAllMocks()
        snippetTestService = mockk(relaxed = true)
        snippetTestController = SnippetTestController(snippetTestService)
    }

    // Helper function to create a dummy TestResponseDTO
    private fun createDummyTestResponseDTO(
        id: String = TEST_ID,
        snippetId: String = SNIPPET_ID,
        name: String = "Test Name",
        inputs: List<String> = listOf("input1"),
        expectedOutputs: List<String> = listOf("output1"),
        expectedStatus: TestStatus = com.ingsisteam.snippetservice2025.model.enum.TestStatus.VALID,
    ) = TestResponseDTO(
        id = id,
        snippetId = snippetId,
        name = name,
        inputs = inputs,
        expectedOutputs = expectedOutputs,
        expectedStatus = expectedStatus,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    @Test
    fun `createTest should return 201 CREATED on success`() {
        val createTestDTO = CreateTestDTO(
            name = "My New Test",
            inputs = listOf("val x = 1"),
            expectedOutputs = listOf("1"),
            expectedStatus = com.ingsisteam.snippetservice2025.model.enum.TestStatus.VALID,
        )
        val expectedResponse = createDummyTestResponseDTO(
            id = TEST_ID + 1,
            snippetId = SNIPPET_ID,
            name = createTestDTO.name,
            inputs = createTestDTO.inputs,
            expectedOutputs = createTestDTO.expectedOutputs,
            expectedStatus = createTestDTO.expectedStatus,
        )
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetTestService.createTest(SNIPPET_ID, createTestDTO, userId) } returns expectedResponse

        val response: ResponseEntity<TestResponseDTO> = snippetTestController.createTest(SNIPPET_ID, createTestDTO, mockJwt)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(expectedResponse.id, response.body?.id)
        assertEquals(expectedResponse.name, response.body?.name)
        verify(exactly = 1) { snippetTestService.createTest(SNIPPET_ID, createTestDTO, userId) }
    }

    @Test
    fun `getTest should return 200 OK on success`() {
        val expectedResponse = createDummyTestResponseDTO(id = TEST_ID)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) } returns expectedResponse

        val response: ResponseEntity<TestResponseDTO> = snippetTestController.getTest(SNIPPET_ID, TEST_ID, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(expectedResponse.id, response.body?.id)
        assertEquals(expectedResponse.name, response.body?.name)
        verify(exactly = 1) { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) }
    }

    @Test
    fun `getTest should throw TestNotFoundException when service throws TestNotFoundException`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) } throws TestNotFoundException("Test not found")

        val exception = assertThrows<TestNotFoundException> {
            snippetTestController.getTest(SNIPPET_ID, TEST_ID, mockJwt)
        }

        assertEquals("Test not found", exception.message)
        verify(exactly = 1) { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) }
    }

    @Test
    fun `getTest should throw PermissionDeniedException when service throws PermissionDeniedException`() {
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) } throws PermissionDeniedException("Permission denied")

        val exception = assertThrows<PermissionDeniedException> {
            snippetTestController.getTest(SNIPPET_ID, TEST_ID, mockJwt)
        }

        assertEquals("Permission denied", exception.message)
        verify(exactly = 1) { snippetTestService.getTest(SNIPPET_ID, TEST_ID, userId) }
    }

    @Test
    fun `getTestsBySnippet should return 200 OK with a list of tests`() {
        val test1 = createDummyTestResponseDTO(id = "200", name = "Test A")
        val test2 = createDummyTestResponseDTO(id = "201", name = "Test B")
        val expectedTests = listOf(test1, test2)
        val mockJwt = mockk<Jwt>(relaxed = true) {
            every { subject } returns userId
        }

        every { snippetTestService.getTestsBySnippet(SNIPPET_ID, userId) } returns expectedTests

        val response: ResponseEntity<List<TestResponseDTO>> = snippetTestController.getTestsBySnippet(SNIPPET_ID, mockJwt)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(2, response.body?.size)
        assertEquals(expectedTests[0].name, response.body?.get(0)?.name)
        verify(exactly = 1) { snippetTestService.getTestsBySnippet(SNIPPET_ID, userId) }
    }
}
