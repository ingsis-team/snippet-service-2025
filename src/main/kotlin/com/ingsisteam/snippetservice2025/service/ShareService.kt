package com.ingsisteam.snippetservice2025.service

import com.ingsisteam.snippetservice2025.connector.Auth0Connector
import com.ingsisteam.snippetservice2025.connector.PermissionServiceConnector
import com.ingsisteam.snippetservice2025.exception.PermissionDeniedException
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetDTO
import com.ingsisteam.snippetservice2025.model.dto.ShareSnippetResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import org.springframework.stereotype.Service

/**
 * Servicio para gestionar la funcionalidad de compartir snippets
 */
@Service
class ShareService(
    private val auth0Connector: Auth0Connector,
    private val permissionServiceConnector: PermissionServiceConnector,
) {

    /**
     * Obtiene la lista de usuarios disponibles para compartir, con filtrado opcional
     * @param search Texto para filtrar usuarios por nombre o email
     * @return Lista de usuarios de Auth0
     */
    fun getAvailableUsers(search: String?): List<Auth0UserDTO> {
        println("[ShareService] Obteniendo usuarios disponibles${if (search != null) " con filtro: $search" else ""}")
        val users = auth0Connector.getUsers(search)
        println("[ShareService] Se encontraron ${users.size} usuarios")
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
        println("[ShareService] Usuario $currentUserId intenta compartir snippet ${shareSnippetDTO.snippetId} con ${shareSnippetDTO.targetUserId}")

        // Verificar que el usuario actual sea owner del snippet
        val permissionCheck = permissionServiceConnector.checkPermission(
            snippetId = shareSnippetDTO.snippetId,
            userId = currentUserId,
        )

        if (!permissionCheck.hasPermission || permissionCheck.role != "OWNER") {
            println("[ShareService] Usuario $currentUserId no es owner del snippet ${shareSnippetDTO.snippetId}")
            throw PermissionDeniedException(
                "No tienes permisos de owner sobre este snippet. Solo el propietario puede compartir snippets.",
            )
        }

        println("[ShareService] Usuario $currentUserId verificado como owner del snippet ${shareSnippetDTO.snippetId}")

        // Verificar si el usuario ya tiene permisos sobre este snippet
        val existingPermission = permissionServiceConnector.checkPermission(
            snippetId = shareSnippetDTO.snippetId,
            userId = shareSnippetDTO.targetUserId,
        )

        if (existingPermission.hasPermission) {
            println("[ShareService] Usuario ${shareSnippetDTO.targetUserId} ya tiene permisos sobre el snippet ${shareSnippetDTO.snippetId} (rol: ${existingPermission.role})")
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
            println("[ShareService] Error al crear permiso para usuario ${shareSnippetDTO.targetUserId}")
            throw RuntimeException("No se pudo crear el permiso de lectura para el usuario")
        }

        println("[ShareService] Snippet ${shareSnippetDTO.snippetId} compartido exitosamente con ${shareSnippetDTO.targetUserId}")

        return ShareSnippetResponseDTO(
            snippetId = permissionResponse.snippetId,
            sharedWithUserId = permissionResponse.userId,
            role = permissionResponse.role,
            message = "Snippet compartido exitosamente con permisos de lectura",
        )
    }
}
