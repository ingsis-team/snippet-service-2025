package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.UnauthorizedException
import com.ingsisteam.snippetservice2025.model.dto.CreateTestDTO
import com.ingsisteam.snippetservice2025.model.dto.TestResponseDTO
import com.ingsisteam.snippetservice2025.service.SnippetTestService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/snippets/{snippetId}/tests")
@Tag(name = "Snippet Test Controller", description = "API para gestionar tests de snippets")
class SnippetTestController(
    private val snippetTestService: SnippetTestService,
) {
    private val logger = LoggerFactory.getLogger(SnippetTestController::class.java)

    // Helper function to extract user ID from JWT or use test user
    private fun getUserId(jwt: Jwt?): String {
        val userId = jwt?.subject ?: throw UnauthorizedException("Usuario no autenticado. Se requiere un token JWT válido")
        if (userId == "test-user@example.com") {
            throw UnauthorizedException("Usuario mockeado no permitido. Debe usar credenciales reales")
        }
        return userId
    }

    // Data class for success responses
    data class SuccessResponse(
        val success: Boolean,
        val message: String,
        val timestamp: String = java.time.LocalDateTime.now().toString(),
    )

    @PostMapping
    @Operation(
        summary = "Crear un test para un snippet",
        description = "Crea un test con inputs y outputs esperados para validar el comportamiento de un snippet",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Test creado exitosamente"),
            ApiResponse(responseCode = "400", description = "Datos inválidos o test duplicado"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para crear tests en este snippet"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun createTest(
        @Parameter(description = "ID del snippet") @PathVariable snippetId: String,
        @Valid @RequestBody createTestDTO: CreateTestDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<TestResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Creating test '{}' for snippet {} by user: {}", createTestDTO.name, snippetId, userId)
        val test = snippetTestService.createTest(snippetId, createTestDTO, userId)
        logger.info("Test created successfully with ID: {}", test.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(test)
    }

    @GetMapping("/{testId}")
    @Operation(
        summary = "Obtener un test específico",
        description = "Obtiene los detalles de un test específico de un snippet",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Test encontrado"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para ver tests de este snippet"),
            ApiResponse(responseCode = "404", description = "Test o snippet no encontrado"),
        ],
    )
    fun getTest(
        @Parameter(description = "ID del snippet") @PathVariable snippetId: String,
        @Parameter(description = "ID del test") @PathVariable testId: String,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<TestResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Fetching test {} for snippet {} by user: {}", testId, snippetId, userId)
        val test = snippetTestService.getTest(snippetId, testId, userId)
        logger.debug("Test {} retrieved successfully", testId)
        return ResponseEntity.ok(test)
    }

    @GetMapping
    @Operation(
        summary = "Obtener todos los tests de un snippet",
        description = "Obtiene la lista de todos los tests creados para un snippet específico",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de tests obtenida exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para ver tests de este snippet"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun getTestsBySnippet(
        @Parameter(description = "ID del snippet") @PathVariable snippetId: String,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<TestResponseDTO>> {
        val userId = getUserId(jwt)
        logger.info("Fetching all tests for snippet {} by user: {}", snippetId, userId)
        val tests = snippetTestService.getTestsBySnippet(snippetId, userId)
        logger.info("Returning {} tests for snippet {}", tests.size, snippetId)
        return ResponseEntity.ok(tests)
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{testId}")
    @Operation(
        summary = "Eliminar un test",
        description = "Elimina un test de un snippet",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Test eliminado exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para eliminar tests de este snippet"),
            ApiResponse(responseCode = "404", description = "Test o snippet no encontrado"),
        ],
    )
    fun deleteTest(
        @Parameter(description = "ID del snippet") @PathVariable snippetId: String,
        @Parameter(description = "ID del test") @PathVariable testId: String,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SuccessResponse> {
        val userId = getUserId(jwt)
        logger.info("Deleting test {} from snippet {} by user: {}", testId, snippetId, userId)
        snippetTestService.deleteTest(snippetId, testId, userId)
        logger.info("Test {} deleted successfully", testId)
        return ResponseEntity.ok(
            SuccessResponse(
                success = true,
                message = "Test con ID $testId eliminado exitosamente del snippet $snippetId",
            ),
        )
    }

    @PostMapping("/{testId}/execute")
    @Operation(
        summary = "Ejecutar un test",
        description = "Ejecuta un test con los inputs definidos y compara con los outputs esperados",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Test ejecutado exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para ejecutar tests de este snippet"),
            ApiResponse(responseCode = "404", description = "Test o snippet no encontrado"),
        ],
    )
    fun executeTest(
        @Parameter(description = "ID del snippet") @PathVariable snippetId: String,
        @Parameter(description = "ID del test") @PathVariable testId: String,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<Map<String, Any>> {
        val userId = getUserId(jwt)
        logger.info("Executing test {} for snippet {} by user: {}", testId, snippetId, userId)
        val result = snippetTestService.executeTest(snippetId, testId, userId)
        logger.info("Test {} executed: {}", testId, if (result["passed"] as Boolean) "PASSED" else "FAILED")
        return ResponseEntity.ok(result)
    }
}
