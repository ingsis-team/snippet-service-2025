package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.UnauthorizedException
import com.ingsisteam.snippetservice2025.model.dto.FormatSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.FormatSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.FormatterRulesFileDTO
import com.ingsisteam.snippetservice2025.model.dto.LintSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.LintSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Rule
import com.ingsisteam.snippetservice2025.service.CodeAnalysisService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("")
@Tag(name = "Code Analysis Controller", description = "API para formateo y linting de snippets")
class CodeAnalysisController(
    private val codeAnalysisService: CodeAnalysisService,
) {
    private val logger = LoggerFactory.getLogger(CodeAnalysisController::class.java)

    private fun getUserId(jwt: Jwt?): String {
        val userId = jwt?.subject ?: throw UnauthorizedException("Usuario no autenticado. Se requiere un token JWT válido")
        if (userId == "test-user@example.com") {
            throw UnauthorizedException("Usuario mockeado no permitido. Debe usar credenciales reales")
        }
        return userId
    }

    @PostMapping("/format")
    @Operation(summary = "Formatear un snippet", description = "Formatea el código de un snippet según las reglas configuradas del usuario")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet formateado exitosamente"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun formatSnippet(
        @RequestBody request: FormatSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<FormatSnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Formatting snippet request: snippetId={}, userId={}", request.snippetId, userId)

        val snippetId = UUID.fromString(request.snippetId)
        val formattedContent = codeAnalysisService.formatSnippet(snippetId, userId)

        return ResponseEntity.ok(FormatSnippetResponseDTO(formattedContent))
    }

    @PostMapping("/lint")
    @Operation(summary = "Analizar un snippet", description = "Analiza el código de un snippet con el linter según las reglas configuradas del usuario")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet analizado exitosamente"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun lintSnippet(
        @RequestBody request: LintSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<LintSnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Linting snippet request: snippetId={}, userId={}", request.snippetId, userId)

        val snippetId = UUID.fromString(request.snippetId)
        val issues = codeAnalysisService.lintSnippet(snippetId, userId)

        return ResponseEntity.ok(LintSnippetResponseDTO(issues))
    }

    @GetMapping("/rules/format")
    @Operation(summary = "Obtener reglas de formateo", description = "Obtiene las reglas de formateo configuradas para el usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reglas obtenidas exitosamente"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun getFormattingRules(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Rule>> {
        val userId = getUserId(jwt)
        logger.info("Getting formatting rules for user: {}", userId)

        val rules = codeAnalysisService.getFormattingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @GetMapping("/rules/lint")
    @Operation(summary = "Obtener reglas de linting", description = "Obtiene las reglas de linting configuradas para el usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reglas obtenidas exitosamente"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun getLintingRules(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Rule>> {
        val userId = getUserId(jwt)
        logger.info("Getting linting rules for user: {}", userId)

        val rules = codeAnalysisService.getLintingRules(userId)
        return ResponseEntity.ok(rules)
    }

    @PostMapping("/rules/format")
    @Operation(summary = "Guardar reglas de formateo", description = "Guarda las reglas de formateo para el usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reglas guardadas exitosamente"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun saveFormattingRulesList(
        @RequestBody rules: List<Rule>,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Rule>> {
        val userId = getUserId(jwt)
        logger.info("Saving formatting rules for user: {}, rulesCount={}", userId, rules.size)

        val savedRules = codeAnalysisService.saveFormattingRules(userId, rules)
        return ResponseEntity.ok(savedRules)
    }

    @PostMapping("/rules/lint")
    @Operation(summary = "Guardar reglas de linting", description = "Guarda las reglas de linting para el usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reglas guardadas exitosamente"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun saveLintingRulesList(
        @RequestBody rules: List<Rule>,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Rule>> {
        val userId = getUserId(jwt)
        logger.info("Saving linting rules for user: {}, rulesCount={}", userId, rules.size)

        val savedRules = codeAnalysisService.saveLintingRules(userId, rules)
        return ResponseEntity.ok(savedRules)
    }

    @PostMapping("/format/all")
    @Operation(
        summary = "Formatear todos los snippets del usuario",
        description = "Formatea todos los snippets donde el usuario es OWNER según las reglas de formateo configuradas",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Formateo masivo completado"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun formatAllSnippets(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<com.ingsisteam.snippetservice2025.model.dto.FormatAllSnippetsResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Formatting all snippets for user: {}", userId)

        val result = codeAnalysisService.formatAllUserSnippets(userId)
        logger.info("Formatting completed: {}/{} successful", result.successfullyFormatted, result.totalSnippets)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/lint/all")
    @Operation(
        summary = "Analizar todos los snippets del usuario",
        description = "Ejecuta el linter en todos los snippets donde el usuario es OWNER según las reglas de linting configuradas",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Linting masivo completado"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun lintAllSnippets(
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<com.ingsisteam.snippetservice2025.model.dto.LintAllSnippetsResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Linting all snippets for user: {}", userId)

        val result = codeAnalysisService.lintAllUserSnippets(userId)
        logger.info("Linting completed: {} with issues, {} without issues", result.snippetsWithIssues, result.snippetsWithoutIssues)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/format/save-rules")
    @Operation(summary = "Guardar reglas de formateo (file DTO)", description = "Guarda las reglas de formateo para el usuario autenticado usando FormatterRulesFileDTO")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Reglas guardadas exitosamente"),
            ApiResponse(responseCode = "401", description = "No autenticado"),
        ],
    )
    fun saveFormattingRules(
        @RequestBody request: FormatterRulesFileDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Rule>> {
        val userId = getUserId(jwt)
        logger.info("Saving formatting rules (file DTO) for user: {}, rulesPayloadPresent=true", userId)

        val savedRules = codeAnalysisService.saveFormattingRules(userId, request)
        return ResponseEntity.ok(savedRules)
    }
}
