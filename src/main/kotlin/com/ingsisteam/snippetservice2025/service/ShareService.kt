package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.Auth0Connector
import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Servicio para gestionar la funcionalidad de compartir snippets
 */
@Service
class ShareService(
    private val auth0Connector: Auth0Connector,
    private val permissionServiceConnector: PermissionServiceConnector,
) {
    private val logger = LoggerFactory.getLogger(ShareService::class.java)

    /**
     * Obtiene la lista de usuarios disponibles para compartir, con filtrado opcional
     * @param search Texto para filtrar usuarios por nombre o email
     * @return Lista de usuarios de Auth0
     */
    fun getAvailableUsers(search: String?): List<Auth0UserDTO> {
        logger.info("Fetching available users{}", if (search != null) " with filter: $search" else "")
        val users = auth0Connector.getUsers(search)
        logger.debug("Found {} users", users.size)
        return users
    }

    /**
     * Comparte un snippet con un usuario, otorgándole permisos de lectura
     * Verifica que el usuario actual sea owner del snippet antes de compartir
     * @param shareSnippetDTO Datos del snippet y usuario objetivo
     * @param currentUserId ID del usuario que intenta compartir
     * @return Respuesta con detalles del permiso creado
     * @throws PermissionDeniedException si el usuario no es owner del snippet
     */
    fun shareSnippet(shareSnippetDTO: ShareSnippetDTO, currentUserId: String): ShareSnippetResponseDTO {
        logger.info(
            "User {} attempting to share snippet {} with user {}",
            currentUserId,
            shareSnippetDTO.snippetId,
            shareSnippetDTO.targetUserId,
        )

        // Verificar que el usuario actual sea owner del snippet
        val permissionCheck = permissionServiceConnector.checkPermission(
            snippetId = shareSnippetDTO.snippetId,
            userId = currentUserId,
        )

        if (!permissionCheck.hasPermission || permissionCheck.role != "OWNER") {
            logger.warn("User {} is not owner of snippet {}, permission denied", currentUserId, shareSnippetDTO.snippetId)
            throw PermissionDeniedException(
                "No tienes permisos de owner sobre este snippet. Solo el propietario puede compartir snippets.",
            )
        }

        logger.debug("User {} verified as owner of snippet {}", currentUserId, shareSnippetDTO.snippetId)

        // Verificar si el usuario ya tiene permisos sobre este snippet
        val existingPermission = permissionServiceConnector.checkPermission(
            snippetId = shareSnippetDTO.snippetId,
            userId = shareSnippetDTO.targetUserId,
        )

        if (existingPermission.hasPermission) {
            logger.info(
                "User {} already has permission on snippet {} with role: {}",
                shareSnippetDTO.targetUserId,
                shareSnippetDTO.snippetId,
                existingPermission.role,
            )
            return ShareSnippetResponseDTO(
                snippetId = shareSnippetDTO.snippetId,
                sharedWithUserId = shareSnippetDTO.targetUserId,
                role = existingPermission.role ?: "READ",
                message = "El usuario ya tiene acceso a este snippet con rol: ${existingPermission.role}",
            )
        }

        // Crear permiso de lectura para el usuario objetivo
        val permissionResponse = permissionServiceConnector.createPermission(
            snippetId = shareSnippetDTO.snippetId,
            userId = shareSnippetDTO.targetUserId,
            role = "READ",
        )

        if (permissionResponse == null) {
            logger.error(
                "Failed to create permission for user {} on snippet {}",
                shareSnippetDTO.targetUserId,
                shareSnippetDTO.snippetId,
            )
            throw RuntimeException("No se pudo crear el permiso de lectura para el usuario")
        }

        logger.info("Snippet {} successfully shared with user {}", shareSnippetDTO.snippetId, shareSnippetDTO.targetUserId)

        return ShareSnippetResponseDTO(
            snippetId = permissionResponse.snippetId,
            sharedWithUserId = permissionResponse.userId,
            role = permissionResponse.role,
            message = "Snippet compartido exitosamente con permisos de lectura",
        )
    }
}
