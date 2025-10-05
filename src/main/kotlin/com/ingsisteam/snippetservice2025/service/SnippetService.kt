package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.repository.SnippetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val printScriptServiceConnector: PrintScriptServiceConnector,
    private val permissionServiceConnector: PermissionServiceConnector,
) {

    fun createSnippetFromFile(createSnippetFileDTO: CreateSnippetFileDTO, userId: String): SnippetResponseDTO {
        // Verificar que no exista otro snippet con el mismo nombre para este usuario
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetFileDTO.name)) {
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetFileDTO.name}'")
        }

        // Validar que el archivo no esté vacío
        if (createSnippetFileDTO.file.isEmpty) {
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Leer el contenido del archivo
        val content = try {
            String(createSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        // Delegar validación de sintaxis al PrintScript Service
        // TODO: Descomentar cuando el servicio esté disponible
        // validateSyntaxWithExternalService(content, createSnippetFileDTO.language.name, createSnippetFileDTO.version)

        // Crear el snippet
        val snippet = Snippet(
            name = createSnippetFileDTO.name,
            description = createSnippetFileDTO.description,
            language = createSnippetFileDTO.language,
            content = content,
            userId = userId,
            version = createSnippetFileDTO.version,
        )

        val savedSnippet = snippetRepository.save(snippet)

        // Delegar creación de permisos al Permission Service
        try {
            permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )
        } catch (e: Exception) {
            // Log warning but don't fail snippet creation
            println("Warning: Could not create permission for snippet ${savedSnippet.id}: ${e.message}")
        }

        return toResponseDTO(savedSnippet)
    }

    @Transactional(readOnly = true)
    fun getSnippet(id: Long, userId: String): SnippetResponseDTO {
        // Verificar permisos con Permission Service
        if (!permissionServiceConnector.hasPermission(id, userId)) {
            throw NoSuchElementException("Snippet con ID $id no encontrado o sin permisos")
        }

        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        return toResponseDTO(snippet)
    }

    @Transactional(readOnly = true)
    fun getAllSnippets(userId: String): List<SnippetResponseDTO> {
        // Por ahora retornamos todos los snippets del usuario
        // En el futuro se podría consultar Permission Service para filtrar por permisos
        return snippetRepository.findByUserId(userId).map { toResponseDTO(it) }
    }

    private fun validateSyntaxWithExternalService(content: String, language: String, version: String) {
        // TODO: Implementar llamada real al PrintScript Service cuando esté disponible
        /*
        val validationResponse = printScriptServiceConnector.validateSnippet(content, language, version)

        if (!validationResponse.isValid && !validationResponse.errors.isNullOrEmpty()) {
            val firstError = validationResponse.errors.first()
            throw SyntaxValidationException(
                rule = firstError.rule,
                line = firstError.line,
                column = firstError.column,
                message = firstError.message,
            )
        }
         */
    }

    private fun toResponseDTO(snippet: Snippet): SnippetResponseDTO {
        return SnippetResponseDTO(
            id = snippet.id,
            name = snippet.name,
            description = snippet.description,
            language = snippet.language,
            content = snippet.content,
            userId = snippet.userId,
            version = snippet.version,
            createdAt = snippet.createdAt,
            updatedAt = snippet.updatedAt,
        )
    }
}
