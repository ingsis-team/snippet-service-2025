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

    fun createPermission(snippetId: Long, userId: String, role: String = "OWNER"): PermissionResponse? {
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
            e.printStackTrace()
            logger.warn("Could not create permission for snippetId: {}, error: {}", snippetId, e.message)
            null
        }
    }

    /**
     * Verifica si un usuario tiene permisos sobre un snippet y devuelve el rol
     * @param snippetId ID del snippet
     * @param userId ID del usuario
     * @return PermissionCheckResponse con hasPermission y role
     */
    fun checkPermission(snippetId: Long, userId: String): PermissionCheckResponse {
        logger.debug("Checking permission for snippetId: {}, userId: {}", snippetId, userId)

        return try {
            val response = client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponse::class.java)
                .block() ?: PermissionCheckResponse(hasPermission = false, role = null)

            logger.debug(
                "Permission check result for snippetId {}: hasPermission={}, role={}",
                snippetId,
                response.hasPermission,
                response.role,
            )
            response
        } catch (e: Exception) {
            logger.error("Error checking permission for snippetId: {}, error: {}", snippetId, e.message, e)
            throw RuntimeException("Error al verificar permisos: ${e.message}", e)
        }
    }

    fun hasPermission(snippetId: Long, userId: String): Boolean {
        logger.debug("Checking if user {} has permission on snippet {}", userId, snippetId)

        return try {
            val response = client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponseDTO::class.java)
                .block()

            val hasPermission = response?.hasPermission ?: false
            logger.debug("User {} has permission on snippet {}: {}", userId, snippetId, hasPermission)
            hasPermission
        } catch (e: Exception) {
            // In case of error, allow access (fail-safe approach for now)
            e.printStackTrace()
            logger.warn("Could not check permission for snippetId: {}, allowing access. Error: {}", snippetId, e.message)
            true
        }
    }

    fun hasWritePermission(snippetId: Long, userId: String): Boolean {
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
            e.printStackTrace()
            logger.warn("Could not check write permission for snippetId: {}, denying access. Error: {}", snippetId, e.message)
            false
        }
    }
}
