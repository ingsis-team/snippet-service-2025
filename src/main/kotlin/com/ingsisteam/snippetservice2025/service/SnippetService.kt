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
    fun getAllSnippets(userId: String, nameFilter: String? = null): List<SnippetResponseDTO> {
        // Si hay filtro de nombre, buscar por nombre (case-insensitive, búsqueda parcial)
        val snippets = if (nameFilter.isNullOrBlank()) {
            snippetRepository.findByUserId(userId)
        } else {
            snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, nameFilter)
        }

        return snippets.map { toResponseDTO(it) }
    }

    fun createSnippet(createSnippetDTO: CreateSnippetDTO, userId: String): SnippetResponseDTO {
        // Verificar que no exista otro snippet con el mismo nombre para este usuario
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetDTO.name)) {
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetDTO.name}'")
        }

        // Validar que el contenido no esté vacío
        if (createSnippetDTO.content.isBlank()) {
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        // Delegar validación de sintaxis al PrintScript Service
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

    fun updateSnippetFromFile(id: Long, updateSnippetFileDTO: UpdateSnippetFileDTO, userId: String): SnippetResponseDTO {
        // Verificar que el snippet existe
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verificar permisos de escritura con Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validar que el archivo no esté vacío
        if (updateSnippetFileDTO.file.isEmpty) {
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Leer el contenido del archivo
        val content = try {
            String(updateSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        // Delegar validación de sintaxis al PrintScript Service
        validateSyntaxWithExternalService(content, snippet.language.name, snippet.version)

        // Actualizar el contenido del snippet
        snippet.content = content
        val updatedSnippet = snippetRepository.save(snippet)

        return toResponseDTO(updatedSnippet)
    }

    fun updateSnippet(id: Long, updateSnippetDTO: UpdateSnippetDTO, userId: String): SnippetResponseDTO {
        // Verificar que el snippet existe
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verificar permisos de escritura con Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validar que el contenido no esté vacío
        if (updateSnippetDTO.content.isBlank()) {
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        // Delegar validación de sintaxis al PrintScript Service
        validateSyntaxWithExternalService(updateSnippetDTO.content, snippet.language.name, snippet.version)

        // Actualizar el contenido del snippet
        snippet.content = updateSnippetDTO.content
        val updatedSnippet = snippetRepository.save(snippet)

        return toResponseDTO(updatedSnippet)
    }

    fun deleteSnippet(id: Long, userId: String) {
        // Verificar que el usuario sea OWNER (solo los owners pueden eliminar)
        val permissionCheck = permissionServiceConnector.checkPermission(id, userId)
        if (!permissionCheck.hasPermission || permissionCheck.role != "OWNER") {
            throw IllegalAccessException("Solo el propietario puede eliminar este snippet")
        }

        // Verificar que el snippet existe antes de eliminar
        if (!snippetRepository.existsById(id)) {
            throw NoSuchElementException("Snippet con ID $id no encontrado")
        }

        // Eliminar el snippet
        snippetRepository.deleteById(id)
        println("✅ [DELETE] Snippet $id eliminado por usuario $userId")

        // TODO: También eliminar permisos en Permission Service
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
        println("🚀 [EXECUTE] Executing snippet $id for user $userId")

        // Verificar que el snippet existe
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
                    val output = if (content.startsWith("\"") && content.endsWith("\"")) {
                        content.substring(1, content.length - 1)
                    } else {
                        content
                    }
                    outputs.add(output)
                    println("📤 [OUTPUT] $output")
                }

                // Handle readInput()
                if (trimmed.contains("readInput(")) {
                    if (inputIndex < executeSnippetDTO.inputs.size) {
                        val input = executeSnippetDTO.inputs[inputIndex]
                        println("📥 [INPUT] Provided: $input")
                        inputIndex++
                    } else {
                        errors.add("Input required but not provided at line: $line")
                    }
                }
            }

            println("✅ [EXECUTE] Execution completed with ${outputs.size} outputs")
        } catch (e: Exception) {
            errors.add("Execution error: ${e.message}")
            println("❌ [EXECUTE] Error: ${e.message}")
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
