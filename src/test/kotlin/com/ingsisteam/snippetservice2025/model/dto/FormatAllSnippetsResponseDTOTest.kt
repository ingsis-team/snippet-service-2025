package com.ingsisteam.snippetservice2025.model.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FormatAllSnippetsResponseDTOTest {

    @Test
    fun `test FormatAllSnippetsResponseDTO`() {
        val result = FormatResult(
            snippetId = "snippet-id",
            snippetName = "snippet-name",
            success = true,
        )
        val dto = FormatAllSnippetsResponseDTO(
            totalSnippets = 1,
            successfullyFormatted = 1,
            failed = 0,
            results = listOf(result),
        )

        assertEquals(1, dto.totalSnippets)
        assertEquals(1, dto.successfullyFormatted)
        assertEquals(0, dto.failed)
        assertEquals(1, dto.results.size)
        assertEquals(result, dto.results[0])
    }
}
