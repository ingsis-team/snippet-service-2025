package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@ExtendWith(MockKExtension::class)
class Auth0ConnectorTest {

    @MockK(relaxed = true)
    private lateinit var webClientBuilder: WebClient.Builder

    @MockK(relaxed = true)
    private lateinit var webClient: WebClient

    @MockK(relaxed = true)
    private lateinit var requestHeadersUriSpec: WebClient.RequestHeadersUriSpec<*>

    @MockK(relaxed = true)
    private lateinit var requestHeadersSpec: WebClient.RequestHeadersSpec<*>

    @MockK(relaxed = true)
    private lateinit var responseSpec: WebClient.ResponseSpec

    @MockK(relaxed = true)
    private lateinit var requestBodySpec: WebClient.RequestBodySpec

    @MockK(relaxed = true)
    private lateinit var requestBodyUriSpec: WebClient.RequestBodyUriSpec

    private val auth0Domain = "test-auth0.com"
    private val managementToken = "test-token"
    private val clientId = "test-client-id"
    private val clientSecret = "test-client-secret"

    private fun createAuth0Connector(
        domain: String = auth0Domain,
        clientId: String = this.clientId,
        clientSecret: String = this.clientSecret,
    ): Auth0Connector {
        return Auth0Connector(webClientBuilder, domain, clientId, clientSecret)
    }

    // Helper function to mock the token retrieval
    private fun mockTokenRetrieval(token: String = managementToken) {
        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri("/oauth/token") } returns requestBodySpec
        every { requestBodySpec.contentType(any()) } returns requestBodySpec
        every { requestBodySpec.bodyValue(any<Map<String, String>>()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec

        // Create a real TokenResponse instance instead of mocking
        val tokenResponse = Auth0Connector.TokenResponse(
            accessToken = token,
            expiresIn = 3600,
            tokenType = "Bearer",
        )

        val tokenMono = mockk<Mono<Auth0Connector.TokenResponse>>()
        every { responseSpec.bodyToMono(Auth0Connector.TokenResponse::class.java) } returns tokenMono
        every { tokenMono.block() } returns tokenResponse
    }

    // Helper function to mock the WebClient chain for getting users
    private fun mockGetUsersChain(expectedUsers: List<Auth0UserDTO> = emptyList(), shouldThrow: Boolean = false) {
        // Mock the chain of calls for GET /api/v2/users
        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $managementToken") } returns requestHeadersSpec
        every { requestHeadersSpec.header(HttpHeaders.CONTENT_TYPE, "application/json") } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.onStatus(any(), any()) } returns responseSpec
            every { responseSpec.bodyToFlux(Auth0UserDTO::class.java) } returns Flux.error(RuntimeException("WebClient error"))
        } else {
            val mockFlux = mockk<Flux<Auth0UserDTO>>()
            val mockMonoList = mockk<Mono<List<Auth0UserDTO>>>()

            every { responseSpec.onStatus(any(), any()) } returns responseSpec
            every { responseSpec.bodyToFlux(Auth0UserDTO::class.java) } returns mockFlux
            every { mockFlux.collectList() } returns mockMonoList
            every { mockMonoList.block() } returns expectedUsers
        }
    }

    @Test
    fun `test getUsers returns users from auth0`() {
        // Given
        every { webClientBuilder.baseUrl("https://$auth0Domain") } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient
        val users = listOf(Auth0UserDTO("1", "test@test.com", "Test User", "test", "pic"))
        mockTokenRetrieval()
        mockGetUsersChain(expectedUsers = users)
        val auth0Connector = createAuth0Connector()

        // When
        val result = auth0Connector.getUsers()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test User", result[0].name)
        verify(exactly = 1) { requestHeadersUriSpec.uri("/api/v2/users?per_page=50&include_totals=false") }
    }

    @Test
    fun `test getUsers with search returns filtered users from auth0`() {
        // Given
        every { webClientBuilder.baseUrl("https://$auth0Domain") } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient
        val users = listOf(Auth0UserDTO("1", "test@test.com", "Test User", "test", "pic"))
        mockTokenRetrieval()
        mockGetUsersChain(expectedUsers = users)
        val auth0Connector = createAuth0Connector()

        // When
        val result = auth0Connector.getUsers("Test")

        // Then
        assertEquals(1, result.size)
        assertEquals("Test User", result[0].name)
        verify(exactly = 1) { requestHeadersUriSpec.uri(any<String>()) }
    }

    @Test
    fun `test getUsers returns mock users on failure`() {
        // Given
        every { webClientBuilder.baseUrl("https://$auth0Domain") } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient
        mockTokenRetrieval()
        mockGetUsersChain(shouldThrow = true)
        // Ensure auth0Connector is initialized with a non-blank token so it attempts the external call
        val failingAuth0Connector = createAuth0Connector()

        // When
        val result = failingAuth0Connector.getUsers()

        // Then
        assertEquals(5, result.size) // Expect mock users
        verify(exactly = 1) { requestHeadersSpec.retrieve() } // Verify it tried to make a call
    }

    @Test
    fun `test getUsers returns mock users when token is blank`() {
        // Given
        val blankTokenAuth0Connector = createAuth0Connector(clientId = "", clientSecret = "") // Blank clientId and clientSecret

        // When
        val result = blankTokenAuth0Connector.getUsers()

        // Then
        assertEquals(5, result.size) // Expect mock users
        verify(exactly = 0) { webClient.get() } // No external call should be made
        verify(exactly = 0) { webClientBuilder.baseUrl(any<String>()) } // Base URL should not be built
    }
}
