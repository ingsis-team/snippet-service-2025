package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.UnauthorizedException
import com.ingsisteam.snippetservice2025.model.dto.FormatSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.FormatSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.LintIssue
import com.ingsisteam.snippetservice2025.model.dto.LintSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.LintSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.service.CodeAnalysisService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.oauth2.jwt.Jwt
import java.util.UUID

class CodeAnalysisControllerTest {

    private lateinit var codeAnalysisService: CodeAnalysisService
    private lateinit var codeAnalysisController: CodeAnalysisController
    private val userId = "test-user"
    private val mockJwt = mockk<Jwt>()

    @BeforeEach
    fun setUp() {
        codeAnalysisService = mockk(relaxed = true)
        codeAnalysisController = CodeAnalysisController(codeAnalysisService)
        every { mockJwt.subject } returns userId
    }

    @Test
    fun `formatSnippet success`() {
        // Given
        val snippetId = UUID.randomUUID()
        val formatDTO = FormatSnippetDTO(snippetId = snippetId.toString())
        val formattedContent = "val a: Int = 1;"

        every { codeAnalysisService.formatSnippet(snippetId, userId) } returns formattedContent

        // When
        val response: ResponseEntity<FormatSnippetResponseDTO> = codeAnalysisController.formatSnippet(formatDTO, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(formattedContent, response.body?.formattedContent)
    }

    @Test
    fun `lintSnippet success`() {
        // Given
        val snippetId = UUID.randomUUID()
        val lintDTO = LintSnippetDTO(snippetId = snippetId.toString())
        val issues = listOf(LintIssue("ruleId", 1, 1, "message"))

        every { codeAnalysisService.lintSnippet(snippetId, userId) } returns issues

        // When
        val response: ResponseEntity<LintSnippetResponseDTO> = codeAnalysisController.lintSnippet(lintDTO, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(issues, response.body?.issues)
    }

    @Test
    fun `getFormattingRules success`() {
        // Given
        val rules = listOf(Rule("name", "value"))
        every { codeAnalysisService.getFormattingRules(userId) } returns rules

        // When
        val response = codeAnalysisController.getFormattingRules(mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(rules, response.body)
    }

    @Test
    fun `getLintingRules success`() {
        // Given
        val rules = listOf(Rule("name", "value"))
        every { codeAnalysisService.getLintingRules(userId) } returns rules

        // When
        val response = codeAnalysisController.getLintingRules(mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(rules, response.body)
    }

    @Test
    fun `saveFormattingRulesList success`() {
        // Given
        val rules = listOf(Rule("name", "value"))
        every { codeAnalysisService.saveFormattingRules(userId, rules) } returns rules

        // When
        val response = codeAnalysisController.saveFormattingRulesList(rules, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(rules, response.body)
    }

    @Test
    fun `saveLintingRulesList success`() {
        // Given
        val rules = listOf(Rule("name", "value"))
        every { codeAnalysisService.saveLintingRules(userId, rules) } returns rules

        // When
        val response = codeAnalysisController.saveLintingRulesList(rules, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(rules, response.body)
    }

    @Test
    fun `saveFormattingRules from DTO success`() {
        // Given
        val dto = FormatterRulesFileDTO(false, true, true, 1, 1, 4)
        val rules = listOf(Rule("name", "value"))
        every { codeAnalysisService.saveFormattingRules(userId, dto) } returns rules

        // When
        val response = codeAnalysisController.saveFormattingRules(dto, mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(rules, response.body)
    }

    @Test
    fun `getUserId throws UnauthorizedException for null jwt`() {
        assertThrows<UnauthorizedException> {
            codeAnalysisController.formatSnippet(FormatSnippetDTO("some-id"), null)
        }
    }

    @Test
    fun `getUserId throws UnauthorizedException for test user`() {
        val testJwt = mockk<Jwt>()
        every { testJwt.subject } returns "test-user@example.com"
        assertThrows<UnauthorizedException> {
            codeAnalysisController.formatSnippet(FormatSnippetDTO("some-id"), testJwt)
        }
    }

    @Test
    fun `formatAllSnippets success`() {
        // Given
        val responseDto = com.ingsisteam.snippetservice2025.model.dto.FormatAllSnippetsResponseDTO(
            totalSnippets = 1,
            successfullyFormatted = 1,
            failed = 0,
            results = emptyList(),
        )
        every { codeAnalysisService.formatAllUserSnippets(userId) } returns responseDto

        // When
        val response = codeAnalysisController.formatAllSnippets(mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
    }

    @Test
    fun `lintAllSnippets success`() {
        // Given
        val responseDto = com.ingsisteam.snippetservice2025.model.dto.LintAllSnippetsResponseDTO(
            totalSnippets = 1,
            snippetsWithIssues = 0,
            snippetsWithoutIssues = 1,
            results = emptyList(),
        )
        every { codeAnalysisService.lintAllUserSnippets(userId) } returns responseDto

        // When
        val response = codeAnalysisController.lintAllSnippets(mockJwt)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(responseDto, response.body)
    }
}
