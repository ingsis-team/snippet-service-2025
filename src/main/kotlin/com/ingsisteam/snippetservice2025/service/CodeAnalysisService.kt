package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CodeAnalysisService(
    private val printScriptConnector: PrintScriptServiceConnector,
    private val snippetRepository: SnippetRepository,
    private val assetServiceConnector: AssetServiceConnector,
) {
    private val logger = LoggerFactory.getLogger(CodeAnalysisService::class.java)

    fun formatSnippet(snippetId: UUID, userId: String): String {
        logger.info("Formatting snippet: snippetId={}, userId={}", snippetId, userId)

        // Get snippet from database
        val snippet = snippetRepository.findById(snippetId.toString())
            .orElseThrow { SnippetNotFoundException("Snippet no encontrado con id: $snippetId") }

        // Get snippet content from asset service
        val content = assetServiceConnector.getSnippet(snippetId.toString())
            ?: throw SnippetNotFoundException("Contenido del snippet no encontrado: $snippetId")

        // Format snippet using PrintScript service
        val correlationId = UUID.randomUUID().toString()
        val result = printScriptConnector.formatSnippet(
            snippetId = snippetId.toString(),
            correlationId = correlationId,
            language = snippet.language.name,
            version = snippet.version,
            input = content,
            userId = userId,
        )

        logger.info("Snippet formatted successfully: snippetId={}", snippetId)
        return result.output
    }

    fun lintSnippet(snippetId: UUID, userId: String): List<com.ingsisteam.snippetservice2025.model.dto.LintIssue> {
        logger.info("Linting snippet: snippetId={}, userId={}", snippetId, userId)

        // Get snippet from database
        val snippet = snippetRepository.findById(snippetId.toString())
            .orElseThrow { SnippetNotFoundException("Snippet no encontrado con id: $snippetId") }

        // Get snippet content from asset service
        val content = assetServiceConnector.getSnippet(snippetId.toString())
            ?: throw SnippetNotFoundException("Contenido del snippet no encontrado: $snippetId")

        // Lint snippet using PrintScript service
        val correlationId = UUID.randomUUID().toString()
        val issues = printScriptConnector.lintSnippet(
            snippetId = snippetId.toString(),
            correlationId = correlationId,
            language = snippet.language.name,
            version = snippet.version,
            input = content,
            userId = userId,
        )

        logger.info("Snippet linted successfully: snippetId={}, issuesFound={}", snippetId, issues.size)
        return issues.map {
            com.ingsisteam.snippetservice2025.model.dto.LintIssue(
                rule = it.rule,
                line = it.line,
                column = it.column,
                message = it.message,
            )
        }
    }

    fun getFormattingRules(userId: String): List<Rule> {
        logger.info("Getting formatting rules for user: {}", userId)
        val correlationId = UUID.randomUUID().toString()
        return printScriptConnector.getFormattingRules(userId, correlationId)
    }

    fun getLintingRules(userId: String): List<Rule> {
        logger.info("Getting linting rules for user: {}", userId)
        val correlationId = UUID.randomUUID().toString()
        return printScriptConnector.getLintingRules(userId, correlationId)
    }

    fun saveFormattingRules(userId: String, rules: List<Rule>): List<Rule> {
        logger.info("Saving formatting rules for user: {}, rulesCount={}", userId, rules.size)
        val correlationId = UUID.randomUUID().toString()
        return printScriptConnector.saveFormattingRules(userId, correlationId, rules)
    }

    fun saveFormattingRules(userId: String, dto: FormatterRulesFileDTO): List<Rule> {
        logger.info("Saving formatting rules (file DTO) for user: {}", userId)
        val correlationId = UUID.randomUUID().toString()
        return try {
            printScriptConnector.saveFormattingRulesFile(userId, correlationId, dto)
        } catch (e: Exception) {
            logger.error("Failed to save formatting rules (file DTO) for user {}: {}", userId, e.message)
            emptyList()
        }
    }

    fun saveLintingRules(userId: String, rules: List<Rule>): List<Rule> {
        logger.info("Saving linting rules for user: {}, rulesCount={}", userId, rules.size)
        val correlationId = UUID.randomUUID().toString()
        return printScriptConnector.saveLintingRules(userId, correlationId, rules)
    }
}
