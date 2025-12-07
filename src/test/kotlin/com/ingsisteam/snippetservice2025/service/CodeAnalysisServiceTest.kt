package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.LintIssue
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.model.dto.external.SCAOutput
import com.ingsisteam.snippetservice2025.model.dto.external.SnippetOutputDTO
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
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
import java.util.*

@ExtendWith(MockKExtension::class)
class CodeAnalysisServiceTest {

    @MockK
    private lateinit var printScriptConnector: PrintScriptServiceConnector

    @MockK
    private lateinit var snippetRepository: SnippetRepository

    @MockK
    private lateinit var assetServiceConnector: AssetServiceConnector

    @InjectMockKs
    private lateinit var codeAnalysisService: CodeAnalysisService

    private fun buildSnippet(
        id: String = UUID.randomUUID().toString(),
        name: String = "testSnippet",
        language: SnippetLanguage = SnippetLanguage.PRINTSCRIPT,
        userId: String = "user123",
        version: String = "1.0",
    ): Snippet = Snippet(
        id = id,
        name = name,
        description = "description",
        language = language,
        userId = userId,
        version = version,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
    )

    // --- Tests for formatSnippet ---

    @Test
    fun `test formatSnippet success`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"
        val snippet = buildSnippet(id = snippetId.toString())
        val content = "let a: string = 'hello';"
        val formattedContent = "let a: string = 'hello';"
        val snippetOutput = SnippetOutputDTO(snippet = formattedContent, correlationId = "corr-id", snippetId = snippetId.toString())

        every { snippetRepository.findById(any()) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(any()) } returns content
        every { printScriptConnector.formatSnippet(any(), any(), any(), any(), any(), any()) } returns snippetOutput
        every { assetServiceConnector.updateSnippet(any(), any()) } returns true

        // When
        val result = codeAnalysisService.formatSnippet(snippetId, userId)

        // Then
        assertEquals(formattedContent, result)
        verify(exactly = 1) { printScriptConnector.formatSnippet(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test formatSnippet snippet not found in repo`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"

        every { snippetRepository.findById(snippetId.toString()) } returns Optional.empty()

        // When & Then
        assertThrows<SnippetNotFoundException> {
            codeAnalysisService.formatSnippet(snippetId, userId)
        }
        verify(exactly = 0) { assetServiceConnector.getSnippet(any()) }
        verify(exactly = 0) { printScriptConnector.formatSnippet(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test formatSnippet snippet content not found`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"
        val snippet = buildSnippet(id = snippetId.toString())

        every { snippetRepository.findById(snippetId.toString()) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(snippetId.toString()) } returns null

        // When & Then
        assertThrows<SnippetNotFoundException> {
            codeAnalysisService.formatSnippet(snippetId, userId)
        }
        verify(exactly = 1) { assetServiceConnector.getSnippet(snippetId.toString()) }
        verify(exactly = 0) { printScriptConnector.formatSnippet(any(), any(), any(), any(), any(), any()) }
    }

    // --- Tests for lintSnippet ---

    @Test
    fun `test lintSnippet success`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"
        val snippet = buildSnippet(id = snippetId.toString())
        val content = "let a:string= 'hello';"
        val lintingIssues = listOf(SCAOutput("camelCase", 1, 5, "Variable 'a' should be in camelCase"))
        val expectedIssues = listOf(LintIssue("camelCase", 1, 5, "Variable 'a' should be in camelCase"))

        every { snippetRepository.findById(snippetId.toString()) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(snippetId.toString()) } returns content
        every {
            printScriptConnector.lintSnippet(
                snippetId = snippetId.toString(),
                correlationId = any(),
                language = snippet.language.name,
                version = snippet.version,
                input = content,
                userId = userId,
            )
        } returns lintingIssues

        // When
        val result = codeAnalysisService.lintSnippet(snippetId, userId)

        // Then
        assertEquals(expectedIssues.size, result.size)
        assertEquals(expectedIssues[0].message, result[0].message)
        verify(exactly = 1) { snippetRepository.findById(snippetId.toString()) }
        verify(exactly = 1) { assetServiceConnector.getSnippet(snippetId.toString()) }
        verify(exactly = 1) { printScriptConnector.lintSnippet(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test lintSnippet snippet not found in repo`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"

        every { snippetRepository.findById(snippetId.toString()) } returns Optional.empty()

        // When & Then
        assertThrows<SnippetNotFoundException> {
            codeAnalysisService.lintSnippet(snippetId, userId)
        }
    }

    @Test
    fun `test lintSnippet snippet content not found`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"
        val snippet = buildSnippet(id = snippetId.toString())

        every { snippetRepository.findById(snippetId.toString()) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(snippetId.toString()) } returns null

        // When & Then
        assertThrows<SnippetNotFoundException> {
            codeAnalysisService.lintSnippet(snippetId, userId)
        }
    }

    // --- Tests for rules ---

    @Test
    fun `test getFormattingRules success`() {
        // Given
        val userId = "user123"
        val rules = listOf(Rule("rule1", "value1"))
        every { printScriptConnector.getFormattingRules(userId, any()) } returns rules

        // When
        val result = codeAnalysisService.getFormattingRules(userId)

        // Then
        assertEquals(rules, result)
        verify(exactly = 1) { printScriptConnector.getFormattingRules(userId, any()) }
    }

    @Test
    fun `test getLintingRules success`() {
        // Given
        val userId = "user123"
        val rules = listOf(Rule("rule2", "value2")) // Corrected
        every { printScriptConnector.getLintingRules(userId, any()) } returns rules

        // When
        val result = codeAnalysisService.getLintingRules(userId)

        // Then
        assertEquals(rules, result)
        verify(exactly = 1) { printScriptConnector.getLintingRules(userId, any()) }
    }

    @Test
    fun `test saveFormattingRules success`() {
        // Given
        val userId = "user123"
        val rules = listOf(Rule("rule1", "value1")) // Corrected
        every { printScriptConnector.saveFormattingRules(userId, any(), rules) } returns rules

        // When
        val result = codeAnalysisService.saveFormattingRules(userId, rules)

        // Then
        assertEquals(rules, result)
        verify(exactly = 1) { printScriptConnector.saveFormattingRules(userId, any(), rules) }
    }

    @Test
    fun `test saveFormattingRules with DTO success`() {
        // Given
        val userId = "user123"
        val dto = FormatterRulesFileDTO(
            spaceBeforeColon = false,
            spaceAfterColon = true,
            spaceAroundEquals = true,
            lineBreak = 1,
            lineBreakPrintln = 2,
            conditionalIndentation = 4,
        )
        val rules = listOf(Rule("rule1", "value1")) // Corrected
        every { printScriptConnector.saveFormattingRulesFile(userId, any(), dto) } returns rules

        // When
        val result = codeAnalysisService.saveFormattingRules(userId, dto)

        // Then
        assertEquals(rules, result)
        verify(exactly = 1) { printScriptConnector.saveFormattingRulesFile(userId, any(), dto) }
    }

    @Test
    fun `test saveFormattingRules with DTO returns empty on exception`() {
        // Given
        val userId = "user123"
        val dto = FormatterRulesFileDTO(
            spaceBeforeColon = false,
            spaceAfterColon = true,
            spaceAroundEquals = true,
            lineBreak = 1,
            lineBreakPrintln = 2,
            conditionalIndentation = 4,
        )
        every {
            printScriptConnector.saveFormattingRulesFile(
                userId,
                any(),
                dto,
            )
        } throws RuntimeException("Connector error")

        // When
        val result = codeAnalysisService.saveFormattingRules(userId, dto)

        // Then
        assertEquals(emptyList<Rule>(), result)
    }

    @Test
    fun `test saveLintingRules success`() {
        // Given
        val userId = "user123"
        val rules = listOf(Rule("rule2", "value2")) // Corrected
        every { printScriptConnector.saveLintingRules(userId, any(), rules) } returns rules

        // When
        val result = codeAnalysisService.saveLintingRules(userId, rules)

        // Then
        assertEquals(rules, result)
        verify(exactly = 1) { printScriptConnector.saveLintingRules(userId, any(), rules) }
    }

    @Test
    fun `test formatSnippet handles printScriptConnector exception`() {
        // Given
        val snippetId = UUID.randomUUID()
        val userId = "user123"
        val snippet = buildSnippet(id = snippetId.toString())
        val content = "let a: string = 'hello';"

        every { snippetRepository.findById(any()) } returns Optional.of(snippet)
        every { assetServiceConnector.getSnippet(any()) } returns content
        every {
            printScriptConnector.formatSnippet(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } throws RuntimeException("PrintScript error")

        // When & Then
        assertThrows<RuntimeException> {
            codeAnalysisService.formatSnippet(snippetId, userId)
        }
        verify(exactly = 1) { printScriptConnector.formatSnippet(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `test formatAllUserSnippets success`() {
        // Given
        val userId = "user123"
        val snippet1 = buildSnippet(userId = userId)
        val snippet2 = buildSnippet(userId = userId)
        val snippets = listOf(snippet1, snippet2)
        val content = "let a: string = 'hello';"
        val formattedContent = "let a: string = 'hello';"
        val snippetOutput1 = SnippetOutputDTO(snippet = formattedContent, correlationId = "corr-id-1", snippetId = snippet1.id)
        val snippetOutput2 = SnippetOutputDTO(snippet = formattedContent, correlationId = "corr-id-2", snippetId = snippet2.id)

        every { snippetRepository.findByUserId(userId) } returns snippets
        every { assetServiceConnector.getSnippet(snippet1.id) } returns content
        every { assetServiceConnector.getSnippet(snippet2.id) } returns content
        every { printScriptConnector.formatSnippet(eq(snippet1.id), any(), any(), any(), any(), any()) } returns snippetOutput1
        every { printScriptConnector.formatSnippet(eq(snippet2.id), any(), any(), any(), any(), any()) } returns snippetOutput2
        every { assetServiceConnector.updateSnippet(any(), any()) } returns true

        // When
        val result = codeAnalysisService.formatAllUserSnippets(userId)

        // Then
        assertEquals(2, result.totalSnippets)
    }

    @Test
    fun `test lintAllUserSnippets success`() {
        // Given
        val userId = "user123"
        val snippet1 = buildSnippet(userId = userId)
        val snippet2 = buildSnippet(userId = userId)
        val snippets = listOf(snippet1, snippet2)
        val content = "let a: string = 'hello';"
        val lintingIssues = listOf(SCAOutput("camelCase", 1, 5, "Variable 'a' should be in camelCase"))
        val noLintingIssues = emptyList<SCAOutput>()

        every { snippetRepository.findByUserId(userId) } returns snippets
        every { assetServiceConnector.getSnippet(snippet1.id) } returns content
        every { assetServiceConnector.getSnippet(snippet2.id) } returns content
        every { printScriptConnector.lintSnippet(eq(snippet1.id), any(), any(), any(), any(), any()) } returns lintingIssues
        every { printScriptConnector.lintSnippet(eq(snippet2.id), any(), any(), any(), any(), any()) } returns noLintingIssues

        // When
        val result = codeAnalysisService.lintAllUserSnippets(userId)

        // Then
        assertEquals(2, result.totalSnippets)
    }
}
