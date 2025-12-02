package com.ingsisteam.snippetservice2025.model.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RunAllTestsResponseDTOTest {

    @Test
    fun `test RunAllTestsResponseDTO`() {
        val result = TestExecutionResult(
            testId = "test-id",
            testName = "test-name",
            passed = true,
            actualOutputs = listOf("output"),
            expectedOutputs = listOf("output"),
            errors = emptyList(),
        )
        val dto = RunAllTestsResponseDTO(
            snippetId = "snippet-id",
            totalTests = 1,
            passedTests = 1,
            failedTests = 0,
            results = listOf(result),
        )

        assertEquals("snippet-id", dto.snippetId)
        assertEquals(1, dto.totalTests)
        assertEquals(1, dto.passedTests)
        assertEquals(0, dto.failedTests)
        assertEquals(1, dto.results.size)
        assertEquals(result, dto.results[0])
    }
}
