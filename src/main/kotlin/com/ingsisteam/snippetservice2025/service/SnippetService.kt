package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.AssetServiceConnector
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
    private val assetServiceConnector: AssetServiceConnector,
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

        // Crear el snippet (sin content en la base de datos)
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
        if (!assetServiceConnector.storeSnippet(savedSnippet.id, content)) {
            logger.error("Failed to store snippet content in asset service, rolling back snippet creation")
            snippetRepository.deleteById(savedSnippet.id)
            throw RuntimeException("No se pudo almacenar el contenido del snippet en el servicio de assets")
        }
        logger.debug("Snippet content stored in asset service: {}", savedSnippet.id)

        // Delegate permission creation to Permission Service - CRITICAL: must succeed
        try {
            val permissionResult = permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )

            if (permissionResult == null) {
                // Rollback: delete the snippet if permission creation failed
                logger.error("Permission creation failed for snippet {}, rolling back snippet creation", savedSnippet.id)
                assetServiceConnector.deleteSnippet(savedSnippet.id)
                throw RuntimeException("No se pudo crear el permiso para el snippet. El snippet no fue creado.")
            }

            logger.debug("Permission created for snippet: {}", savedSnippet.id)
        } catch (e: Exception) {
            // Rollback: delete the snippet and asset if permission creation failed
            logger.error("Permission creation failed for snippet {}: {}, rolling back snippet creation", savedSnippet.id, e.message)
            assetServiceConnector.deleteSnippet(savedSnippet.id)
            throw RuntimeException("No se pudo crear el permiso para el snippet: ${e.message}", e)
        }

        return toResponseDTO(savedSnippet)
    }

    fun updateSnippetFromFile(id: String, updateSnippetFileDTO: UpdateSnippetFileDTO, userId: String): SnippetResponseDTO {
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

        // Update the snippet content in asset service
        if (!assetServiceConnector.updateSnippet(id, content)) {
            logger.error("Failed to update snippet content in asset service")
            throw RuntimeException("No se pudo actualizar el contenido del snippet en el servicio de assets")
        }
        logger.debug("Snippet content updated in asset service: {}", id)

        // Update name if provided
        updateSnippetFileDTO.name?.let {
            if (it.isNotBlank()) {
                // Verificar que no exista otro snippet con el mismo nombre para este usuario (excepto el actual)
                if (snippetRepository.existsByUserIdAndName(userId, it) && snippet.name != it) {
                    logger.warn("Duplicate snippet name '{}' for user: {}", it, userId)
                    throw IllegalArgumentException("Ya existe un snippet con el nombre '$it'")
                }
                snippet.name = it
            }
        }

        // Update description if provided
        updateSnippetFileDTO.description?.let {
            snippet.description = it
        }

        val updatedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet {} updated successfully", id)

        return toResponseDTO(updatedSnippet)
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

        // Crear el snippet (sin content en la base de datos)
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
        if (!assetServiceConnector.storeSnippet(savedSnippet.id, createSnippetDTO.content)) {
            logger.error("Failed to store snippet content in asset service, rolling back snippet creation")
            snippetRepository.deleteById(savedSnippet.id)
            throw RuntimeException("No se pudo almacenar el contenido del snippet en el servicio de assets")
        }
        logger.debug("Snippet content stored in asset service: {}", savedSnippet.id)

        // Delegate permission creation to Permission Service - CRITICAL: must succeed
        try {
            val permissionResult = permissionServiceConnector.createPermission(
                snippetId = savedSnippet.id,
                userId = userId,
                role = "OWNER",
            )

            if (permissionResult == null) {
                // Rollback: delete the snippet if permission creation failed
                logger.error("Permission creation failed for snippet {}, rolling back snippet creation", savedSnippet.id)
                snippetRepository.deleteById(savedSnippet.id)
                throw RuntimeException("No se pudo crear el permiso para el snippet. El snippet no fue creado.")
            }

            logger.debug("Permission created for snippet: {}", savedSnippet.id)
        } catch (e: Exception) {
            // Rollback: delete the snippet and asset if permission creation failed
            logger.error("Permission creation failed for snippet {}: {}, rolling back snippet creation", savedSnippet.id, e.message)
            assetServiceConnector.deleteSnippet(savedSnippet.id)
            snippetRepository.deleteById(savedSnippet.id)
            throw RuntimeException("No se pudo crear el permiso para el snippet: ${e.message}", e)
        }

        // Trigger automatic formatting, linting, and testing
        try {
            printScriptServiceConnector.triggerAutomaticFormatting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = createSnippetDTO.content,
            )
            printScriptServiceConnector.triggerAutomaticLinting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = createSnippetDTO.content,
            )
            printScriptServiceConnector.triggerAutomaticTesting(
                snippetId = savedSnippet.id.toString(),
                userId = userId,
                content = createSnippetDTO.content,
            )
        } catch (e: Exception) {
            // Log but don't fail - automatic formatting/linting/testing is optional
            logger.debug("Optional operations failed, but snippet creation succeeded: {}", e.message)
        }

        return toResponseDTO(savedSnippet)
    }

    fun updateSnippet(id: String, updateSnippetDTO: UpdateSnippetDTO, userId: String): SnippetResponseDTO {
        logger.debug("Updating snippet {} from editor for user: {}", id, userId)

        // Verify that the snippet exists
        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        // Verify write permissions with Permission Service
        if (!permissionServiceConnector.hasWritePermission(id, userId)) {
            logger.warn("User {} attempted to update snippet {} without write permission", userId, id)
            throw IllegalAccessException("No tienes permisos de escritura para este snippet")
        }

        // Verificar que se proporcione al menos un campo para actualizar
        if (updateSnippetDTO.content == null && updateSnippetDTO.name == null && updateSnippetDTO.description == null) {
            logger.warn("No fields provided for snippet update: {}", id)
            throw IllegalArgumentException("Debe proporcionar al menos un campo para actualizar (content, name o description)")
        }

        // Update content if provided
        var contentUpdated = false
        updateSnippetDTO.content?.let { newContent ->
            if (newContent.isBlank()) {
                logger.warn("Empty content for snippet update: {}", id)
                throw IllegalArgumentException("El contenido no puede estar vacío")
            }

            logger.debug("Validating syntax for snippet update: {}", id)
            // Delegate syntax validation to PrintScript Service
            validateSyntaxWithExternalService(newContent, snippet.language.name, snippet.version)

            // Update the snippet content in asset service
            if (!assetServiceConnector.updateSnippet(id, newContent)) {
                logger.error("Failed to update snippet content in asset service")
                throw RuntimeException("No se pudo actualizar el contenido del snippet en el servicio de assets")
            }
            logger.debug("Snippet content updated in asset service: {}", id)
            contentUpdated = true
        }

        // Update name if provided
        updateSnippetDTO.name?.let {
            if (it.isNotBlank()) {
                // Verificar que no exista otro snippet con el mismo nombre para este usuario (excepto el actual)
                if (snippetRepository.existsByUserIdAndName(userId, it) && snippet.name != it) {
                    logger.warn("Duplicate snippet name '{}' for user: {}", it, userId)
                    throw IllegalArgumentException("Ya existe un snippet con el nombre '$it'")
                }
                snippet.name = it
            }
        }

        // Update description if provided
        updateSnippetDTO.description?.let {
            snippet.description = it
        }

        val updatedSnippet = snippetRepository.save(snippet)
        logger.info("Snippet {} updated successfully", id)

        // Trigger automatic formatting, linting, and testing only if content was updated
        if (contentUpdated && updateSnippetDTO.content != null) {
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
        }

        return toResponseDTO(updatedSnippet)
    }

    fun deleteSnippet(id: String, userId: String) {
        logger.debug("Deleting snippet: {} by user: {}", id, userId)

        // Verify that the user is OWNER (only owners can delete)
        val permissionCheck = permissionServiceConnector.checkPermission(id, userId)
        if (!permissionCheck.has_permission || permissionCheck.role != "OWNER") {
            logger.warn("User {} attempted to delete snippet {} without owner permission", userId, id)
            throw IllegalAccessException("Solo el propietario puede eliminar este snippet")
        }

        // Verify that the snippet exists before deleting
        if (!snippetRepository.existsById(id)) {
            logger.warn("Attempted to delete non-existent snippet: {}", id)
            throw NoSuchElementException("Snippet con ID $id no encontrado")
        }

        // Delete the snippet from the database
        snippetRepository.deleteById(id)
        logger.info("Snippet {} deleted successfully by user: {}", id, userId)

        // Delete the snippet content from asset service
        try {
            assetServiceConnector.deleteSnippet(id)
            logger.debug("Snippet content deleted from asset service: {}", id)
        } catch (e: Exception) {
            logger.warn("Could not delete snippet content from asset service {}: {}", id, e.message)
            // Log warning but don't fail snippet deletion
        }

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

        if (!validationResponse.isValid) {
            if (!validationResponse.errors.isNullOrEmpty()) {
                val firstError = validationResponse.errors.first()
                logger.warn("Syntax validation failed: {} at line {}, column {}", firstError.rule, firstError.line, firstError.column)
                throw SyntaxValidationException(
                    rule = firstError.rule,
                    line = firstError.line,
                    column = firstError.column,
                    message = firstError.message,
                )
            } else {
                // Si isValid es false pero no hay errores específicos, lanzar excepción genérica
                logger.error("Validation service returned invalid response without error details")
                throw SyntaxValidationException(
                    rule = "VALIDATION_ERROR",
                    line = 1,
                    column = 1,
                    message = "Error en la validación del snippet",
                )
            }
        }
        logger.debug("Syntax validation passed")
    }

    @Transactional(readOnly = true)
    fun executeSnippet(
        id: String,
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

        val outputs = mutableListOf<String>()
        val errors = mutableListOf<String>()
        var inputIndex = 0

        // Retrieve content from asset service
        val content = assetServiceConnector.getSnippet(id)
            ?: throw RuntimeException("No se pudo recuperar el contenido del snippet desde el servicio de assets")

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

    @Transactional(readOnly = true)
    fun getSnippet(id: String, userId: String): SnippetResponseDTO {
        logger.debug("Fetching snippet: {} for user: {}", id, userId)

        // Verificar permisos con Permission Service
        if (!permissionServiceConnector.hasPermission(id, userId)) {
            logger.warn("User {} attempted to access snippet {} without permission", userId, id)
            throw NoSuchElementException("Snippet con ID $id no encontrado o sin permisos")
        }

        val snippet = snippetRepository.findById(id).orElse(null)
            ?: throw NoSuchElementException("Snippet con ID $id no encontrado")

        logger.debug("Snippet {} retrieved successfully", id)
        return toResponseDTO(snippet)
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
            emptyList<String>()
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
        return snippets.map { toResponseDTO(it) }
    }

    private fun toResponseDTO(snippet: Snippet): SnippetResponseDTO {
        // Retrieve content from asset service
        val content = assetServiceConnector.getSnippet(snippet.id)
            ?: run {
                logger.warn("Could not retrieve content from asset service for snippet {}", snippet.id)
                ""
            }

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
