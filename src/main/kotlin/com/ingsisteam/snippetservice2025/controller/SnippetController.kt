package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.service.SnippetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/snippets")
@Tag(name = "Snippet Controller", description = "API para gestionar snippets de código")
class SnippetController(
    private val snippetService: SnippetService,
) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Crear un nuevo snippet mediante archivo",
        description = "Crea un snippet subiendo un archivo de código con validación de sintaxis",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Snippet creado exitosamente"),
            ApiResponse(responseCode = "400", description = "Archivo inválido, sintaxis incorrecta o datos faltantes"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        ],
    )
    fun createSnippetFromFile(
        @ModelAttribute createSnippetFileDTO: CreateSnippetFileDTO,
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = extractUserIdFromAuth(authHeader)
        val snippet = snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(snippet)
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener snippet por ID", description = "Obtiene un snippet específico del usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet encontrado"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        ],
    )
    fun getSnippet(
        @Parameter(description = "ID del snippet") @PathVariable id: Long,
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = extractUserIdFromAuth(authHeader)
        val snippet = snippetService.getSnippet(id, userId)
        return ResponseEntity.ok(snippet)
    }

    @GetMapping
    @Operation(summary = "Obtener todos los snippets", description = "Obtiene todos los snippets del usuario autenticado")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de snippets obtenida exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        ],
    )
    fun getAllSnippets(
        @RequestHeader("Authorization") authHeader: String,
    ): ResponseEntity<List<SnippetResponseDTO>> {
        val userId = extractUserIdFromAuth(authHeader)
        val snippets = snippetService.getAllSnippets(userId)
        return ResponseEntity.ok(snippets)
    }

    // Método temporal para extraer userId del header Authorization
    // Más adelante se reemplazará por lógica de JWT
    private fun extractUserIdFromAuth(authHeader: String): String {
        // Por ahora retornamos un userId fijo para testing
        // TODO: Implementar extracción real del JWT token
        return "user-test-123"
    }
}
