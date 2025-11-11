
package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * Conector para comunicarse con Auth0 y obtener información de usuarios
 */
@Service
class Auth0Connector(
    private val webClient: WebClient.Builder,
    @Value("\${auth0.domain}") private val auth0Domain: String,
    @Value("\${auth0.management.token}") private val managementToken: String,
) {

    private val client: WebClient by lazy {
        webClient.baseUrl("https://$auth0Domain").build()
    }

    /**
     * Obtiene la lista de usuarios desde Auth0
     * @param search Texto opcional para filtrar usuarios por nombre o email
     * @return Lista de usuarios de Auth0
     */
    fun getUsers(search: String? = null): List<Auth0UserDTO> {
        return try {
            val uri = if (search.isNullOrBlank()) {
                "/api/v2/users?per_page=100"
            } else {
                "/api/v2/users?q=$search&search_engine=v3&per_page=100"
            }

            client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $managementToken")
                .retrieve()
                .bodyToFlux(Auth0UserDTO::class.java)
                .collectList()
                .block() ?: emptyList()
        } catch (e: Exception) {
            println("[Auth0Connector] Error al obtener usuarios: ${e.message}")
            throw RuntimeException("Error al obtener usuarios de Auth0: ${e.message}", e)
        }
    }
}
