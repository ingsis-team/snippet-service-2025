
package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import org.slf4j.LoggerFactory
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
    @Value("\${auth0.domain:}") private val auth0Domain: String,
    @Value("\${auth0.management.token:}") private val managementToken: String,
) {
    private val logger = LoggerFactory.getLogger(Auth0Connector::class.java)

    private val client: WebClient by lazy {
        webClient.baseUrl("https://$auth0Domain").build()
    }

    /**
     * Obtiene la lista de usuarios desde Auth0
     * @param search Texto opcional para filtrar usuarios por nombre o email
     * @return Lista de usuarios de Auth0
     */
    fun getUsers(search: String? = null): List<Auth0UserDTO> {
        // Si no hay token de Auth0, retornar usuarios mock para testing
        if (managementToken.isBlank()) {
            logger.warn("No Auth0 management token configured, returning mock users")
            return getMockUsers(search)
        }

        logger.debug("Fetching users from Auth0{}", if (search != null) " with search: $search" else "")

        return try {
            val uri = if (search.isNullOrBlank()) {
                "/api/v2/users?per_page=100"
            } else {
                "/api/v2/users?q=$search&search_engine=v3&per_page=100"
            }

            val users = client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $managementToken")
                .retrieve()
                .bodyToFlux(Auth0UserDTO::class.java)
                .collectList()
                .block() ?: emptyList()

            logger.debug("Fetched {} users from Auth0", users.size)
            users
        } catch (e: Exception) {
            logger.error("Error fetching users from Auth0: {}, returning mock users", e.message, e)
            return getMockUsers(search)
        }
    }

    /**
     * Retorna una lista de usuarios mock para testing cuando Auth0 no está configurado
     */
    private fun getMockUsers(search: String? = null): List<Auth0UserDTO> {
        logger.debug("Returning mock users{}", if (search != null) " with filter: $search" else "")

        val allMockUsers = listOf(
            Auth0UserDTO(
                userId = "test-user-1@example.com",
                email = "test-user-1@example.com",
                name = "Test User 1",
                nickname = "testuser1",
                picture = "https://via.placeholder.com/150",
            ),
            Auth0UserDTO(
                userId = "test-user-2@example.com",
                email = "test-user-2@example.com",
                name = "Test User 2",
                nickname = "testuser2",
                picture = "https://via.placeholder.com/150",
            ),
            Auth0UserDTO(
                userId = "john.doe@example.com",
                email = "john.doe@example.com",
                name = "John Doe",
                nickname = "johndoe",
                picture = "https://via.placeholder.com/150",
            ),
            Auth0UserDTO(
                userId = "jane.smith@example.com",
                email = "jane.smith@example.com",
                name = "Jane Smith",
                nickname = "janesmith",
                picture = "https://via.placeholder.com/150",
            ),
            Auth0UserDTO(
                userId = "talcazar@mail.austral.edu.ar",
                email = "talcazar@mail.austral.edu.ar",
                name = "Tomas Alcazar",
                nickname = "talcazar",
                picture = "https://via.placeholder.com/150",
            ),
        )

        // Filter by search if provided
        return if (search.isNullOrBlank()) {
            allMockUsers
        } else {
            allMockUsers.filter {
                it.name?.contains(search, ignoreCase = true) == true ||
                    it.email?.contains(search, ignoreCase = true) == true
            }
        }
    }
}
