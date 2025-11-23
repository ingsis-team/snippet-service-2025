package com.ingsisteam.snippetservice2025.controller

import com.ingsisteam.snippetservice2025.exception.UnauthorizedException
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import com.ingsisteam.snippetservice2025.service.ShareService
import com.ingsisteam.snippetservice2025.service.SnippetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/snippets")
@Tag(name = "Snippet Controller", description = "API para gestionar snippets de código")
class SnippetController(
    private val snippetService: SnippetService,
    private val shareService: ShareService,
) {
    private val logger = LoggerFactory.getLogger(SnippetController::class.java)

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
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Creating snippet from file: '{}' for user: {}", createSnippetFileDTO.name, userId)
        val snippet = snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        logger.info("Snippet created successfully with ID: {}", snippet.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(snippet)
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Crear un nuevo snippet desde el editor",
        description = "Crea un snippet directamente desde el código escrito en el editor con validación de sintaxis",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Snippet creado exitosamente"),
            ApiResponse(responseCode = "400", description = "Datos inválidos, sintaxis incorrecta o contenido vacío"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        ],
    )
    fun createSnippet(
        @RequestBody createSnippetDTO: CreateSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Creating snippet from editor: '{}' for user: {}", createSnippetDTO.name, userId)
        val snippet = snippetService.createSnippet(createSnippetDTO, userId)
        logger.info("Snippet created successfully with ID: {}", snippet.id)
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
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Fetching snippet with ID: {} for user: {}", id, userId)
        val snippet = snippetService.getSnippet(id, userId)
        logger.debug("Snippet retrieved successfully: {}", id)
        return ResponseEntity.ok(snippet)
    }

    @GetMapping
    @Operation(summary = "Obtener todos los snippets", description = "Obtiene todos los snippets del usuario autenticado, opcionalmente filtrado por nombre")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de snippets obtenida exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
        ],
    )
    fun getAllSnippets(
        @Parameter(description = "Filtrar por nombre de snippet (búsqueda parcial, case-insensitive)")
        @RequestParam(required = false) name: String?,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<SnippetResponseDTO>> {
        val userId = getUserId(jwt)
        logger.info("Fetching all snippets for user: {}{}", userId, if (name != null) " with filter: $name" else "")
        val snippets = snippetService.getAllSnippets(userId, name)
        logger.info("Returning {} snippets", snippets.size)
        return ResponseEntity.ok(snippets)
    }

    @PutMapping("/{id}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(
        summary = "Actualizar snippet mediante archivo",
        description = "Actualiza el contenido de un snippet subiendo un nuevo archivo con validación de sintaxis",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet actualizado exitosamente"),
            ApiResponse(responseCode = "400", description = "Archivo inválido o sintaxis incorrecta"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos de escritura"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun updateSnippetFromFile(
        @Parameter(description = "ID del snippet") @PathVariable id: Long,
        @ModelAttribute updateSnippetFileDTO: UpdateSnippetFileDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Updating snippet {} from file for user: {}", id, userId)
        val snippet = snippetService.updateSnippetFromFile(id, updateSnippetFileDTO, userId)
        logger.info("Snippet {} updated successfully", id)
        return ResponseEntity.ok(snippet)
    }

    @PutMapping("/{id}", consumes = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(
        summary = "Actualizar snippet desde el editor",
        description = "Actualiza el contenido de un snippet directamente desde el código escrito en el editor con validación de sintaxis",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet actualizado exitosamente"),
            ApiResponse(responseCode = "400", description = "Contenido inválido o sintaxis incorrecta"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos de escritura"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun updateSnippet(
        @Parameter(description = "ID del snippet") @PathVariable id: Long,
        @RequestBody updateSnippetDTO: UpdateSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Updating snippet {} from editor for user: {}", id, userId)
        val snippet = snippetService.updateSnippet(id, updateSnippetDTO, userId)
        logger.info("Snippet {} updated successfully", id)
        return ResponseEntity.ok(snippet)
    }

    @GetMapping("/users")
    @Operation(
        summary = "Obtener usuarios disponibles",
        description = "Obtiene la lista de usuarios con los que se puede compartir snippets. Soporta filtrado por nombre o email.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "500", description = "Error al obtener usuarios de Auth0"),
        ],
    )
    fun getAvailableUsers(
        @Parameter(description = "Texto para filtrar usuarios por nombre o email")
        @RequestParam(required = false) search: String?,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<List<Auth0UserDTO>> {
        val userId = getUserId(jwt)
        logger.info("Fetching available users for user: {}{}", userId, if (search != null) " with filter: $search" else "")
        val users = shareService.getAvailableUsers(search)
        logger.info("Returning {} available users", users.size)
        return ResponseEntity.ok(users)
    }

    @PostMapping("/share")
    @Operation(
        summary = "Compartir snippet con un usuario",
        description = "Comparte un snippet con otro usuario otorgándole permisos de lectura. Solo el owner puede compartir.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet compartido exitosamente"),
            ApiResponse(responseCode = "400", description = "Datos inválidos"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "No tienes permisos de owner sobre este snippet"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
            ApiResponse(responseCode = "500", description = "Error al crear permiso"),
        ],
    )
    fun shareSnippet(
        @RequestBody shareSnippetDTO: ShareSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<ShareSnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info(
            "Sharing snippet {} with user {} by owner: {}",
            shareSnippetDTO.snippetId,
            shareSnippetDTO.targetUserId,
            userId,
        )
        val response = shareService.shareSnippet(shareSnippetDTO, userId)
        logger.info("Snippet {} shared successfully", shareSnippetDTO.snippetId)
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar snippet",
        description = "Elimina un snippet. Solo el propietario (OWNER) puede eliminar snippets.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet eliminado exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "No eres el propietario de este snippet"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun deleteSnippet(
        @Parameter(description = "ID del snippet") @PathVariable id: Long,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<SuccessResponse> {
        val userId = getUserId(jwt)
        logger.info("Deleting snippet {} by user: {}", id, userId)
        snippetService.deleteSnippet(id, userId)
        logger.info("Snippet {} deleted successfully", id)
        return ResponseEntity.ok(
            SuccessResponse(
                success = true,
                message = "Snippet con ID $id eliminado exitosamente",
            ),
        )
    }

    @PostMapping("/{id}/execute")
    @Operation(summary = "Ejecutar snippet", description = "Ejecuta un snippet de forma interactiva con inputs provistos")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Snippet ejecutado exitosamente"),
            ApiResponse(responseCode = "401", description = "Usuario no autenticado"),
            ApiResponse(responseCode = "403", description = "Sin permisos para ejecutar este snippet"),
            ApiResponse(responseCode = "404", description = "Snippet no encontrado"),
        ],
    )
    fun executeSnippet(
        @Parameter(description = "ID del snippet") @PathVariable id: Long,
        @RequestBody executeSnippetDTO: com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO,
        @AuthenticationPrincipal jwt: Jwt?,
    ): ResponseEntity<com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetResponseDTO> {
        val userId = getUserId(jwt)
        logger.info("Executing snippet {} for user: {} with inputs: {}", id, userId, executeSnippetDTO.inputs)
        val result = snippetService.executeSnippet(id, executeSnippetDTO, userId)
        logger.info("Snippet {} executed successfully with outputs: {}", id, result.outputs)
        return ResponseEntity.ok(result)
    }
}
