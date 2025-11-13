package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponse
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionCheckResponseDTO
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionRequest
import com.ingsisteam.snippetservice2025.model.dto.external.PermissionResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PermissionServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${permission.url}") private val permissionUrl: String,
) {

    private val client: WebClient by lazy {
        webClient.baseUrl(permissionUrl).build()
    }

    fun createPermission(snippetId: Long, userId: String, role: String = "OWNER"): PermissionResponse? {
        val request = PermissionRequest(
            snippet_id = snippetId,
            user_id = userId,
            role = role,
        )

        return try {
            client.post()
                .uri("/api/permissions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PermissionResponse::class.java)
                .block()
        } catch (e: Exception) {
            // Log error but don't fail snippet creation
            e.printStackTrace()
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
        return try {
            client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponse::class.java)
                .block() ?: PermissionCheckResponse(hasPermission = false, role = null)
        } catch (e: Exception) {
            throw RuntimeException("Error al verificar permisos: ${e.message}", e)
        }
    }

    fun hasPermission(snippetId: Long, userId: String): Boolean {
        return try {
            val response = client.get()
                .uri("/api/permissions/check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(PermissionCheckResponseDTO::class.java)
                .block()
            response?.hasPermission ?: false
        } catch (e: Exception) {
            // In case of error, allow access (fail-safe approach for now)
            e.printStackTrace()
            true
        }
    }

    fun hasWritePermission(snippetId: Long, userId: String): Boolean {
        return try {
            val response = client.get()
                .uri("/api/permissions/write-check?snippetId=$snippetId&userId=$userId")
                .retrieve()
                .bodyToMono(Boolean::class.java)
                .block()
            response ?: false
        } catch (e: Exception) {
            // In case of error, deny write access (fail-secure approach)
            e.printStackTrace()
            false
        }
    }
}
