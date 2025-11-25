package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponse
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionRequest
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PermissionServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${permission.url}") private val permissionUrl: String,
) {
    private val logger = LoggerFactory.getLogger(PermissionServiceConnector::class.java)

    private val client: WebClient by lazy {
        webClient.baseUrl(permissionUrl).build()
    }

    fun createPermission(snippetId: String, userId: String, role: String = "OWNER"): PermissionResponse? {
        logger.debug("Creating permission for snippetId: {}, userId: {}, role: {}", snippetId, userId, role)

        val request = PermissionRequest(
            snippet_id = snippetId,
            user_id = userId,
            role = role,
        )

        return try {
            val response = client.post()
                .uri("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PermissionResponse::class.java)
                .block()

            logger.debug("Permission created successfully for snippetId: {}", snippetId)
            response
        } catch (e: Exception) {
            // Log error but don't fail snippet creation
            logger.error("Could not create permission for snippetId: {}, error: {}", snippetId, e.message, e)
            null
        }
    }

    /**
     * Verifica si un usuario tiene permisos sobre un snippet y devuelve el rol
     * @param snippetId ID del snippet
     * @param userId ID del usuario
     * @return PermissionCheckResponse con hasPermission y role
     */
    fun checkPermission(snippetId: String, userId: String): PermissionCheckResponse {
        logger.debug("Checking permission for snippetId: {}, userId: {}", snippetId, userId)

        return try {
            val response = client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponse::class.java)
                .block() ?: PermissionCheckResponse(has_permission = false, role = null)

            logger.debug(
                "Permission check result for snippetId {}: hasPermission={}, role={}",
                snippetId,
                response.has_permission,
                response.role,
            )
            response
        } catch (e: Exception) {
            logger.error("Error checking permission for snippetId: {}, error: {}", snippetId, e.message, e)
            throw RuntimeException("Error al verificar permisos: ${e.message}", e)
        }
    }

    fun hasPermission(snippetId: String, userId: String): Boolean {
        logger.debug("Checking if user {} has permission on snippet {}", userId, snippetId)

        return try {
            val response = client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponseDTO::class.java)
                .block()

            val hasPermission = response?.has_permission ?: false
            logger.debug("User {} has permission on snippet {}: {}", userId, snippetId, hasPermission)
            hasPermission
        } catch (e: Exception) {
            // In case of error, allow access (fail-safe approach for now)
            e.printStackTrace()
            logger.warn("Could not check permission for snippetId: {}, allowing access. Error: {}", snippetId, e.message)
            true
        }
    }

    fun hasWritePermission(snippetId: String, userId: String): Boolean {
        logger.debug("Checking if user {} has write permission on snippet {}", userId, snippetId)

        return try {
            val response = client.get()
                .uri("/api/permissions/write-check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(Boolean::class.java)
                .block()

            val hasWrite = response ?: false
            logger.debug("User {} has write permission on snippet {}: {}", userId, snippetId, hasWrite)
            hasWrite
        } catch (e: Exception) {
            // In case of error, deny write access (fail-secure approach)
            logger.warn("Could not check write permission for snippetId: {}, denying access. Error: {}", snippetId, e.message)
            false
        }
    }

    /**
     * Obtiene todos los IDs de snippets a los que el usuario tiene acceso
     * @param userId ID del usuario
     * @return Lista de IDs de snippets permitidos
     */
    fun getUserPermittedSnippets(userId: String): List<String> {
        logger.debug("Fetching all permitted snippets for user: {}", userId)

        return try {
            val permissions = client.get()
                .uri("/api/permissions/user/$userId")
                .retrieve()
                .bodyToFlux(PermissionResponse::class.java)
                .collectList()
                .block() ?: emptyList()

            val snippetIds = permissions.map { it.snippet_id }
            logger.debug("User {} has access to {} snippets", userId, snippetIds.size)
            snippetIds
        } catch (e: Exception) {
            logger.error("Error fetching permitted snippets for user {}: {}", userId, e.message, e)
            emptyList()
        }
    }

    /**
     * Elimina todos los permisos de un snippet
     * @param snippetId ID del snippet
     */
    fun deleteSnippetPermissions(snippetId: String) {
        logger.debug("Deleting all permissions for snippet: {}", snippetId)

        try {
            // Primero obtener todos los permisos del snippet
            val permissions = client.get()
                .uri("/api/permissions/snippet/$snippetId")
                .retrieve()
                .bodyToFlux(PermissionResponse::class.java)
                .collectList()
                .block() ?: emptyList()

            logger.debug("Found {} permissions to delete for snippet {}", permissions.size, snippetId)

            // Eliminar cada permiso
            permissions.forEach { permission ->
                try {
                    client.delete()
                        .uri("/api/permissions/snippet/$snippetId/user/${permission.user_id}")
                        .retrieve()
                        .bodyToMono(Void::class.java)
                        .block()

                    logger.debug("Deleted permission for user {} on snippet {}", permission.user_id, snippetId)
                } catch (e: Exception) {
                    logger.warn(
                        "Could not delete permission for user {} on snippet {}: {}",
                        permission.user_id,
                        snippetId,
                        e.message,
                    )
                }
            }

            logger.info("All permissions deleted for snippet: {}", snippetId)
        } catch (e: Exception) {
            logger.error("Error deleting permissions for snippet {}: {}", snippetId, e.message, e)
        }
    }
}
