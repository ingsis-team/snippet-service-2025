package com.ingsisteam.snippetservice2025.connector

import com.fasterxml.jackson.annotation.JsonProperty
import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

/**
 * Conector para comunicarse con Auth0 y obtener información de usuarios
 */
@Service
class Auth0Connector(
    private val webClient: WebClient.Builder,
    @Value("\${auth0.domain:}") private val auth0Domain: String,
    @Value("\${auth0.client.id:}") private val clientId: String,
    @Value("\${auth0.client.secret:}") private val clientSecret: String,
) {
    private val logger = LoggerFactory.getLogger(Auth0Connector::class.java)

    private val client: WebClient by lazy {
        webClient.baseUrl("https://$auth0Domain").build()
    }

    // Cache del token de management
    private var managementToken: String? = null
    private var tokenExpiresAt: Instant? = null

    data class TokenResponse(
        @JsonProperty("access_token") val accessToken: String,
        @JsonProperty("expires_in") val expiresIn: Int,
        @JsonProperty("token_type") val tokenType: String,
    )

    /**
     * Obtiene un token de acceso para la Management API de Auth0
     */
    private fun getManagementToken(): String? {
        // Si tenemos un token válido en cache, lo usamos
        if (managementToken != null && tokenExpiresAt?.isAfter(Instant.now().plusSeconds(300)) == true) {
            return managementToken
        }

        // Verificar que tenemos las credenciales necesarias
        if (auth0Domain.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            logger.warn(
                "Auth0 credentials not configured properly. Domain: {}, ClientId: {}, ClientSecret: {}",
                auth0Domain.isNotBlank(),
                clientId.isNotBlank(),
                clientSecret.isNotBlank(),
            )
            return null
        }

        logger.debug("Requesting new Auth0 Management API token")

        return try {
            val tokenRequest = mapOf(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "audience" to "https://$auth0Domain/api/v2/",
                "grant_type" to "client_credentials",
            )

            logger.debug("Request body to Auth0 (token): client_id={}, audience={}", clientId, "https://$auth0Domain/api/v2/")

            val response = client.post()
                .uri("/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenRequest)
                .retrieve()
                .bodyToMono(TokenResponse::class.java)
                .block()

            if (response != null) {
                managementToken = response.accessToken
                tokenExpiresAt = Instant.now().plusSeconds(response.expiresIn.toLong())
                logger.debug("Auth0 Management token obtained successfully, expires in {} seconds", response.expiresIn)
                logger.debug("Response body from Auth0 (token): tokenType={}, expiresIn={}", response.tokenType, response.expiresIn)
                response.accessToken
            } else {
                logger.error("Failed to obtain Auth0 Management token: null response")
                null
            }
        } catch (e: Exception) {
            logger.error("Error obtaining Auth0 Management token: {}", e.message, e)
            null
        }
    }

    /**
     * Obtiene la lista de usuarios desde Auth0
     * @param search Texto opcional para filtrar usuarios por nombre o email
     * @return Lista de usuarios de Auth0
     */
    fun getUsers(search: String? = null): List<Auth0UserDTO> {
        // Intentar obtener el token de management
        val token = getManagementToken()

        if (token == null) {
            logger.warn("Could not obtain Auth0 management token, returning mock users")
            return getMockUsers(search)
        }

        logger.debug("Fetching users from Auth0{}", if (search != null) " with search: $search" else "")

        return try {
            val uri = if (search.isNullOrBlank()) {
                "/api/v2/users?per_page=50&include_totals=false"
            } else {
                "/api/v2/users?q=${java.net.URLEncoder.encode(search, "UTF-8")}&search_engine=v3&per_page=50&include_totals=false"
            }

            logger.debug("Making request to Auth0: https://{}{}", auth0Domain, uri)

            val users = client.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .retrieve()
                .onStatus(
                    { status -> status.is4xxClientError || status.is5xxServerError },
                    { response ->
                        response.bodyToMono(String::class.java)
                            .map { body ->
                                logger.error("Auth0 API error response: Status={}, Body={}", response.statusCode(), body)
                                when {
                                    body.contains("Bad HTTP authentication header format") -> {
                                        logger.error("Auth0 token is invalid or malformed.")
                                        // Limpiar cache del token
                                        managementToken = null
                                        tokenExpiresAt = null
                                    }
                                    body.contains("Insufficient scope") -> {
                                        logger.error("Auth0 application doesn't have required 'read:users' scope. Please check application permissions in Auth0 dashboard.")
                                    }
                                    body.contains("expired") || body.contains("invalid") -> {
                                        logger.error("Auth0 token has expired or is invalid.")
                                        // Limpiar cache del token
                                        managementToken = null
                                        tokenExpiresAt = null
                                    }
                                }
                                RuntimeException("Auth0 API error: ${response.statusCode()} - $body")
                            }
                    },
                )
                .bodyToFlux(Auth0UserDTO::class.java)
                .collectList()
                .block() ?: emptyList()

            logger.debug("Fetched {} users from Auth0", users.size)
            logger.debug("Response body from Auth0: {} users returned", users.size)
            users
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException) {
            logger.error("Auth0 API error: Status={}, Response={}", e.statusCode, e.responseBodyAsString)
            logger.warn("Failed to fetch users from Auth0, returning mock users. Check Auth0 configuration.")
            // Limpiar cache del token en caso de error
            managementToken = null
            tokenExpiresAt = null
            return getMockUsers(search)
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
