package com.ingsisteam.snippetservice2025.model.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LintAllSnippetsResponseDTOTest {

    @Test
    fun `test LintAllSnippetsResponseDTO`() {
        val issue = LintIssue(
            line = 1,
            column = 1,
            message = "error",
            rule = "rule",
        )
        val result = LintResult(
            snippetId = "snippet-id",
            snippetName = "snippet-name",
            issuesCount = 1,
            issues = listOf(issue),
        )
        val dto = LintAllSnippetsResponseDTO(
            totalSnippets = 1,
            snippetsWithIssues = 1,
            snippetsWithoutIssues = 0,
            results = listOf(result),
        )

        assertEquals(1, dto.totalSnippets)
        assertEquals(1, dto.snippetsWithIssues)
        assertEquals(0, dto.snippetsWithoutIssues)
        assertEquals(1, dto.results.size)
        assertEquals(result, dto.results[0])
    }
}
