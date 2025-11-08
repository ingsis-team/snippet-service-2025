package com.ingsisteam.snippetservice2025.controller

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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = jwt.subject
        println("📥 [POST /api/snippets (file)] Received request to create snippet: ${createSnippetFileDTO.name}")
        println("👤 [POST /api/snippets (file)] User ID: $userId")
        val snippet = snippetService.createSnippetFromFile(createSnippetFileDTO, userId)
        println("✅ [POST /api/snippets (file)] Snippet created successfully with ID: ${snippet.id}")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = jwt.subject
        println("📥 [POST /api/snippets (JSON)] Received request to create snippet: ${createSnippetDTO.name}")
        println("👤 [POST /api/snippets (JSON)] User ID: $userId")
        val snippet = snippetService.createSnippet(createSnippetDTO, userId)
        println("✅ [POST /api/snippets (JSON)] Snippet created successfully with ID: ${snippet.id}")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = jwt.subject
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<SnippetResponseDTO>> {
        val userId = jwt.subject
        println("📥 [GET /api/snippets] Received request to list snippets")
        println("👤 [GET /api/snippets] User ID: $userId")
        val snippets = snippetService.getAllSnippets(userId)
        println("✅ [GET /api/snippets] Returning ${snippets.size} snippets")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = jwt.subject
        println("📥 [PUT /api/snippets/{id} (file)] Received request to update snippet: $id")
        val snippet = snippetService.updateSnippetFromFile(id, updateSnippetFileDTO, userId)
        println("✅ [PUT /api/snippets/{id} (file)] Snippet updated successfully")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<SnippetResponseDTO> {
        val userId = jwt.subject
        println("📥 [PUT /api/snippets/{id} (JSON)] Received request to update snippet: $id")
        val snippet = snippetService.updateSnippet(id, updateSnippetDTO, userId)
        println("✅ [PUT /api/snippets/{id} (JSON)] Snippet updated successfully")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<List<Auth0UserDTO>> {
        val userId = jwt.subject
        println("📥 [GET /api/snippets/users] Recibiendo solicitud para obtener usuarios${if (search != null) " con filtro: $search" else ""}")
        println("👤 [GET /api/snippets/users] Usuario autenticado: $userId")

        val users = shareService.getAvailableUsers(search)
        println("✅ [GET /api/snippets/users] Retornando ${users.size} usuarios")
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
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<ShareSnippetResponseDTO> {
        val userId = jwt.subject
        println("📥 [POST /api/snippets/share] Recibiendo solicitud para compartir snippet ${shareSnippetDTO.snippetId} con usuario ${shareSnippetDTO.targetUserId}")
        println("👤 [POST /api/snippets/share] Usuario autenticado: $userId")

        val response = shareService.shareSnippet(shareSnippetDTO, userId)
        println("✅ [POST /api/snippets/share] Snippet compartido exitosamente")
        return ResponseEntity.ok(response)
    }
}
