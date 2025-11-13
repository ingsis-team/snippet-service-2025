package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.connector.PrintScriptServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.exception.SnippetNotFoundException
import com.ingsisteam.snippetservice2025.exception.SyntaxValidationException
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.CreateSnippetFileDTO
import com.ingsisteam.snippetservice2025.model.dto.SnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.UpdateSnippetFileDTO
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

        // Validate that the file is not empty
        if (createSnippetFileDTO.file.isEmpty) {
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Read the file content
        val content = try {
            String(createSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(content, createSnippetFileDTO.language.name, createSnippetFileDTO.version)

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

        // Delegate permission creation to Permission Service
        try {
            permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )
        } catch (e: Exception) {
            // Log warning but don't fail snippet creation
        }

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = savedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = savedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = savedSnippet.content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
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
    fun getAllSnippets(userId: String, nameFilter: String? = null): List<SnippetResponseDTO> {
        // If name filter is provided, search by name (case-insensitive, partial search)
        val snippets = if (nameFilter.isNullOrBlank()) {
            snippetRepository.findByUserId(userId)
        } else {
            snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, nameFilter)
        }

        return snippets.map { toResponseDTO(it) }
    }

    fun createSnippet(createSnippetDTO: CreateSnippetDTO, userId: String): SnippetResponseDTO {
        // Verify that no other snippet with the same name exists for this user
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetDTO.name)) {
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetDTO.name}'")
        }

        // Validate that the content is not empty
        if (createSnippetDTO.content.isBlank()) {
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(createSnippetDTO.content, createSnippetDTO.language.name, createSnippetDTO.version)

        // Crear el snippet
        val snippet = Snippet(
            name = createSnippetDTO.name,
            description = createSnippetDTO.description,
            language = createSnippetDTO.language,
            content = createSnippetDTO.content,
            userId = userId,
            version = createSnippetDTO.version,
        )

        val savedSnippet = snippetRepository.save(snippet)

        // Delegate permission creation to Permission Service
        try {
            permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )
        } catch (e: Exception) {
            // Log warning but don't fail snippet creation
        }

        return toResponseDTO(savedSnippet)
    }

    fun updateSnippetFromFile(id: Long, updateSnippetFileDTO: UpdateSnippetFileDTO, userId: String): SnippetResponseDTO {
        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verify write permissions with Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validate that the file is not empty
        if (updateSnippetFileDTO.file.isEmpty) {
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Read the file content
        val content = try {
            String(updateSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(content, snippet.language.name, snippet.version)

        // Update the snippet content
        snippet.content = content
        val updatedSnippet = snippetRepository.save(snippet)

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
        }

        return toResponseDTO(updatedSnippet)
    }

    fun updateSnippet(id: Long, updateSnippetDTO: UpdateSnippetDTO, userId: String): SnippetResponseDTO {
        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verify write permissions with Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validate that the content is not empty
        if (updateSnippetDTO.content.isBlank()) {
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(updateSnippetDTO.content, snippet.language.name, snippet.version)

        // Update the snippet content
        snippet.content = updateSnippetDTO.content
        val updatedSnippet = snippetRepository.save(snippet)

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updatedSnippet.content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
        }

        return toResponseDTO(updatedSnippet)
    }

    fun deleteSnippet(id: Long, userId: String) {
        // Verify that the user is OWNER (only owners can delete)
        val permissionCheck = permissionServiceConnector.checkPermission(id, userId)
        if (!permissionCheck.hasPermission || permissionCheck.role != "OWNER") {
            throw IllegalAccessException("Solo el propietario puede eliminar este snippet")
        }

        // Verify that the snippet exists before deleting
        if (!snippetRepository.existsById(id)) {
            throw NoSuchElementException("Snippet con ID $id no encontrado")
        }

        // Delete the snippet
        snippetRepository.deleteById(id)

        // TODO: Also delete permissions in Permission Service
        // permissionServiceConnector.deletePermissions(id)
    }

    private fun validateSyntaxWithExternalService(content: String, language: String, version: String) {
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
    }

    @Transactional(readOnly = true)
    fun executeSnippet(
        id: Long,
        executeSnippetDTO: com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO,
        userId: String,
    ): com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetResponseDTO {
        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $id no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(id, userId)) {
            throw PermissionDeniedException("No tienes permisos para ejecutar este snippet")
        }

        val outputs = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var inputIndex = 0

        try {
            // Parse and execute the snippet line by line
            val lines = snippet.content.lines()

            for (line in lines) {
                val trimmed = line.trim()

                // Handle println()
                if (trimmed.startsWith("println(") && trimmed.endsWith(");")) {
                    val content = trimmed.substring(8, trimmed.length - 2).trim()
                    val output = if ((content.startsWith("\"") && content.endsWith("\"")) ||
                        (content.startsWith("'") && content.endsWith("'"))
                    ) {
                        content.substring(1, content.length - 1)
                    } else {
                        content
                    }
                    outputs.add(output)
                }

                // Handle readInput()
                if (trimmed.contains("readInput(")) {
                    if (inputIndex < executeSnippetDTO.inputs.size) {
                        inputIndex++
                    } else {
                        errors.add("Input required but not provided at line: $line")
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Execution error: ${e.message}")
        }

        return com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetResponseDTO(
            outputs = outputs,
            errors = errors,
        )
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
