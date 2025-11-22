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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SnippetService(
    private val snippetRepository: SnippetRepository,
    private val printScriptServiceConnector: PrintScriptServiceConnector,
    private val permissionServiceConnector: PermissionServiceConnector,
    private val assetServiceConnector: com.ingsisteam.snippetservice2025.connector.AssetServiceConnector,
) {
    private val logger = LoggerFactory.getLogger(SnippetService::class.java)

    fun createSnippetFromFile(createSnippetFileDTO: CreateSnippetFileDTO, userId: String): SnippetResponseDTO {
        logger.debug("Creating snippet from file for user: {}", userId)

        // Verificar que no exista otro snippet con el mismo nombre para este usuario
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetFileDTO.name)) {
            logger.warn("Duplicate snippet name '{}' for user: {}", createSnippetFileDTO.name, userId)
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetFileDTO.name}'")
        }

        // Validate that the file is not empty
        if (createSnippetFileDTO.file.isEmpty) {
            logger.warn("Empty file uploaded for snippet: {}", createSnippetFileDTO.name)
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Read the file content
        val content = try {
            String(createSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Error reading file for snippet {}: {}", createSnippetFileDTO.name, e.message)
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        logger.debug("Validating syntax for snippet: {}", createSnippetFileDTO.name)
        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(content, createSnippetFileDTO.language.name, createSnippetFileDTO.version)

        // Crear el snippet (metadata only - content goes to asset service)
        val snippet = Snippet(
            name = createSnippetFileDTO.name,
            description = createSnippetFileDTO.description,
            language = createSnippetFileDTO.language,
            userId = userId,
            version = createSnippetFileDTO.version,
        )

        val savedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet created successfully: ID={}, name='{}', user={}", savedSnippet.id, savedSnippet.name, userId)

        // Store content in asset service
        try {
            assetServiceConnector.storeSnippetContent(savedSnippet.id, content)
            logger.debug("Snippet content stored in asset service: {}", savedSnippet.id)
        } catch (e: Exception) {
            logger.error("Failed to store snippet content in asset service: {}", savedSnippet.id, e)
            // Rollback: delete the snippet from database
            snippetRepository.deleteById(savedSnippet.id)
            throw RuntimeException("Failed to store snippet content: ${e.message}", e)
        }

        // Delegate permission creation to Permission Service
        try {
            permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )
            logger.debug("Permission created for snippet: {}", savedSnippet.id)
        } catch (e: Exception) {
            logger.warn("Could not create permission for snippet {}: {}", savedSnippet.id, e.message)
            // Log warning but don't fail snippet creation
        }

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
        }

        return toResponseDTO(savedSnippet, content)
    }

    @Transactional(readOnly = true)
    fun getSnippet(id: Long, userId: String): SnippetResponseDTO {
        logger.debug("Fetching snippet: {} for user: {}", id, userId)

        // Verificar permisos con Permission Service
        if (!permissionServiceConnector.hasPermission(id, userId)) {
            logger.warn("User {} attempted to access snippet {} without permission", userId, id)
            throw NoSuchElementException("Snippet con ID $id no encontrado o sin permisos")
        }

        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Retrieve content from asset service
        val content = try {
            assetServiceConnector.getSnippetContent(id)
        } catch (e: Exception) {
            logger.error("Failed to retrieve snippet content from asset service: {}", id, e)
            throw RuntimeException("Failed to retrieve snippet content: ${e.message}", e)
        }

        logger.debug("Snippet {} retrieved successfully", id)
        return toResponseDTO(snippet, content)
    }

    @Transactional(readOnly = true)
    fun getAllSnippets(userId: String, nameFilter: String? = null): List<SnippetResponseDTO> {
        logger.debug("Fetching all snippets for user: {}{}", userId, if (nameFilter != null) " with filter: $nameFilter" else "")

        // Obtener todos los snippets a los que el usuario tiene acceso desde Permission Service
        val permittedSnippetIds = try {
            permissionServiceConnector.getUserPermittedSnippets(userId)
        } catch (e: Exception) {
            logger.error("Error fetching permitted snippets for user {}: {}", userId, e.message, e)
            // Fallback: solo mostrar snippets propios
            emptyList<Long>()
        }

        logger.debug("User {} has access to {} snippets via permissions", userId, permittedSnippetIds.size)

        // Buscar snippets: propios + compartidos
        val snippets = if (permittedSnippetIds.isEmpty()) {
            // Si no hay permisos desde el servicio, solo mostrar snippets propios
            if (nameFilter.isNullOrBlank()) {
                snippetRepository.findByUserId(userId)
            } else {
                snippetRepository.findByUserIdAndNameContainingIgnoreCase(userId, nameFilter)
            }
        } else {
            // Obtener snippets por IDs permitidos
            val allPermittedSnippets = snippetRepository.findAllById(permittedSnippetIds)

            // Aplicar filtro de nombre si existe
            if (nameFilter.isNullOrBlank()) {
                allPermittedSnippets
            } else {
                allPermittedSnippets.filter { it.name.contains(nameFilter, ignoreCase = true) }
            }
        }

        logger.debug("Returning {} snippets for user: {}", snippets.size, userId)
        // For list view, we retrieve content for each snippet
        return snippets.map { snippet ->
            try {
                val content = assetServiceConnector.getSnippetContent(snippet.id)
                toResponseDTO(snippet, content)
            } catch (e: Exception) {
                logger.error("Failed to retrieve content for snippet {}: {}", snippet.id, e.message)
                // Return snippet with empty content if asset service fails
                toResponseDTO(snippet, "")
            }
        }
    }

    fun createSnippet(createSnippetDTO: CreateSnippetDTO, userId: String): SnippetResponseDTO {
        logger.debug("Creating snippet from editor for user: {}", userId)

        // Verify that no other snippet with the same name exists for this user
        if (snippetRepository.existsByUserIdAndName(userId, createSnippetDTO.name)) {
            logger.warn("Duplicate snippet name '{}' for user: {}", createSnippetDTO.name, userId)
            throw IllegalArgumentException("Ya existe un snippet con el nombre '${createSnippetDTO.name}'")
        }

        // Validate that the content is not empty
        if (createSnippetDTO.content.isBlank()) {
            logger.warn("Empty content for snippet: {}", createSnippetDTO.name)
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        logger.debug("Validating syntax for snippet: {}", createSnippetDTO.name)
        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(createSnippetDTO.content, createSnippetDTO.language.name, createSnippetDTO.version)

        // Crear el snippet (metadata only - content goes to asset service)
        val snippet = Snippet(
            name = createSnippetDTO.name,
            description = createSnippetDTO.description,
            language = createSnippetDTO.language,
            userId = userId,
            version = createSnippetDTO.version,
        )

        val savedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet created successfully: ID={}, name='{}', user={}", savedSnippet.id, savedSnippet.name, userId)

        // Store content in asset service
        try {
            assetServiceConnector.storeSnippetContent(savedSnippet.id, createSnippetDTO.content)
            logger.debug("Snippet content stored in asset service: {}", savedSnippet.id)
        } catch (e: Exception) {
            logger.error("Failed to store snippet content in asset service: {}", savedSnippet.id, e)
            // Rollback: delete the snippet from database
            snippetRepository.deleteById(savedSnippet.id)
            throw RuntimeException("Failed to store snippet content: ${e.message}", e)
        }

        // Delegate permission creation to Permission Service
        try {
            permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )
            logger.debug("Permission created for snippet: {}", savedSnippet.id)
        } catch (e: Exception) {
            logger.warn("Could not create permission for snippet {}: {}", savedSnippet.id, e.message)
            // Log warning but don't fail snippet creation
        }

        return toResponseDTO(savedSnippet, createSnippetDTO.content)
    }

    fun updateSnippetFromFile(id: Long, updateSnippetFileDTO: UpdateSnippetFileDTO, userId: String): SnippetResponseDTO {
        logger.debug("Updating snippet {} from file for user: {}", id, userId)

        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verify write permissions with Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            logger.warn("User {} attempted to update snippet {} without write permission", userId, id)
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validate that the file is not empty
        if (updateSnippetFileDTO.file.isEmpty) {
            logger.warn("Empty file uploaded for snippet update: {}", id)
            throw IllegalArgumentException("El archivo no puede estar vacío")
        }

        // Read the file content
        val content = try {
            String(updateSnippetFileDTO.file.bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            logger.error("Error reading file for snippet update {}: {}", id, e.message)
            throw IllegalArgumentException("Error al leer el archivo: ${e.message}")
        }

        logger.debug("Validating syntax for snippet update: {}", id)
        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(content, snippet.language.name, snippet.version)

        // Update the snippet metadata (updated_at will be automatically set)
        val updatedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet {} metadata updated successfully", id)

        // Update content in asset service
        try {
            assetServiceConnector.storeSnippetContent(id, content)
            logger.debug("Snippet content updated in asset service: {}", id)
        } catch (e: Exception) {
            logger.error("Failed to update snippet content in asset service: {}", id, e)
            throw RuntimeException("Failed to update snippet content: ${e.message}", e)
        }

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
        }

        return toResponseDTO(updatedSnippet, content)
    }

    fun updateSnippet(id: Long, updateSnippetDTO: UpdateSnippetDTO, userId: String): SnippetResponseDTO {
        logger.debug("Updating snippet {} from editor for user: {}", id, userId)

        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verify write permissions with Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            logger.warn("User {} attempted to update snippet {} without write permission", userId, id)
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Validate that the content is not empty
        if (updateSnippetDTO.content.isBlank()) {
            logger.warn("Empty content for snippet update: {}", id)
            throw IllegalArgumentException("El contenido no puede estar vacío")
        }

        logger.debug("Validating syntax for snippet update: {}", id)
        // Delegate syntax validation to PrintScript Service
        validateSyntaxWithExternalService(updateSnippetDTO.content, snippet.language.name, snippet.version)

        // Update the snippet metadata (updated_at will be automatically set)
        val updatedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet {} metadata updated successfully", id)

        // Update content in asset service
        try {
            assetServiceConnector.storeSnippetContent(id, updateSnippetDTO.content)
            logger.debug("Snippet content updated in asset service: {}", id)
        } catch (e: Exception) {
            logger.error("Failed to update snippet content in asset service: {}", id, e)
            throw RuntimeException("Failed to update snippet content: ${e.message}", e)
        }

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updateSnippetDTO.content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updateSnippetDTO.content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = updatedSnippet.id.toString(),
                userId = userId,
                content = updateSnippetDTO.content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
        }

        return toResponseDTO(updatedSnippet, updateSnippetDTO.content)
    }

    fun deleteSnippet(id: Long, userId: String) {
        logger.debug("Deleting snippet: {} by user: {}", id, userId)

        // Verify that the user is OWNER (only owners can delete)
        val permissionCheck = permissionServiceConnector.checkPermission(id, userId)
        if (!permissionCheck.hasPermission || permissionCheck.role != "OWNER") {
            logger.warn("User {} attempted to delete snippet {} without owner permission", userId, id)
            throw IllegalAccessException("Solo el propietario puede eliminar este snippet")
        }

        // Verify that the snippet exists before deleting
        if (!snippetRepository.existsById(id)) {
            logger.warn("Attempted to delete non-existent snippet: {}", id)
            throw NoSuchElementException("Snippet con ID $id no encontrado")
        }

        // Delete content from asset service first
        try {
            assetServiceConnector.deleteSnippetContent(id)
            logger.debug("Snippet content deleted from asset service: {}", id)
        } catch (e: Exception) {
            logger.warn("Could not delete snippet content from asset service {}: {}", id, e.message)
            // Log warning but don't fail snippet deletion
        }

        // Delete the snippet
        snippetRepository.deleteById(id)
        logger.info("Snippet {} deleted successfully by user: {}", id, userId)

        // Delete all permissions for this snippet in Permission Service
        try {
            permissionServiceConnector.deleteSnippetPermissions(id)
            logger.debug("Permissions deleted for snippet: {}", id)
        } catch (e: Exception) {
            logger.warn("Could not delete permissions for snippet {}: {}", id, e.message)
            // Log warning but don't fail snippet deletion
        }
    }

    private fun validateSyntaxWithExternalService(content: String, language: String, version: String) {
        logger.debug("Validating syntax with PrintScript service: language={}, version={}", language, version)
        val validationResponse = printScriptServiceConnector.validateSnippet(content, language, version)

        if (!validationResponse.isValid && !validationResponse.errors.isNullOrEmpty()) {
            val firstError = validationResponse.errors.first()
            logger.warn("Syntax validation failed: {} at line {}, column {}", firstError.rule, firstError.line, firstError.column)
            throw SyntaxValidationException(
                rule = firstError.rule,
                line = firstError.line,
                column = firstError.column,
                message = firstError.message,
            )
        }
        logger.debug("Syntax validation passed")
    }

    @Transactional(readOnly = true)
    fun executeSnippet(
        id: Long,
        executeSnippetDTO: com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetDTO,
        userId: String,
    ): com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetResponseDTO {
        logger.info("Executing snippet {} for user: {}", id, userId)

        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw SnippetNotFoundException("Snippet con ID $id no encontrado")

        // Verificar permisos de lectura
        if (!permissionServiceConnector.hasPermission(id, userId)) {
            logger.warn("User {} attempted to execute snippet {} without permission", userId, id)
            throw PermissionDeniedException("No tienes permisos para ejecutar este snippet")
        }

        // Retrieve content from asset service
        val content = try {
            assetServiceConnector.getSnippetContent(id)
        } catch (e: Exception) {
            logger.error("Failed to retrieve snippet content for execution: {}", id, e)
            throw RuntimeException("Failed to retrieve snippet content: ${e.message}", e)
        }

        val outputs = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var inputIndex = 0

        try {
            // Parse and execute the snippet line by line
            val lines = content.lines()
            logger.debug("Executing {} lines of code", lines.size)

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
                    logger.debug("Output: {}", output)
                }

                // Handle readInput()
                if (trimmed.contains("readInput(")) {
                    if (inputIndex < executeSnippetDTO.inputs.size) {
                        val input = executeSnippetDTO.inputs[inputIndex]
                        logger.debug("Input provided: {}", input)
                        inputIndex++
                    } else {
                        val errorMsg = "Input required but not provided at line: $line"
                        errors.add(errorMsg)
                        logger.warn(errorMsg)
                    }
                }
            }

            logger.info("Snippet {} execution completed: {} outputs, {} errors", id, outputs.size, errors.size)
        } catch (e: Exception) {
            val errorMsg = "Execution error: ${e.message}"
            errors.add(errorMsg)
            logger.error("Error executing snippet {}: {}", id, e.message, e)
        }

        return com.ingsisteam.snippetservice2025.model.dto.ExecuteSnippetResponseDTO(
            outputs = outputs,
            errors = errors,
        )
    }

    private fun toResponseDTO(snippet: Snippet, content: String): SnippetResponseDTO {
        return SnippetResponseDTO(
            id = snippet.id,
            name = snippet.name,
            description = snippet.description,
            language = snippet.language,
            content = content,
            userId = snippet.userId,
            version = snippet.version,
            createdAt = snippet.createdAt,
            updatedAt = snippet.updatedAt,
        )
    }
}
