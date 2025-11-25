package com.ingsisteam.snippetservice2025.connector

import com.ingsisteam.snippetservice2025.model.dto.external.Auth0UserDTO
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
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

    @InjectMockKs
    private lateinit var auth0Connector: Auth0Connector

    private val auth0Domain = "test-auth0.com"
    private val managementToken = "test-token"

    // Helper function to mock the WebClient chain
    private fun mockWebClientChain(expectedUsers: List<Auth0UserDTO> = emptyList(), shouldThrow: Boolean = false, auth0Domain: String = this.auth0Domain) {
        // Mock the WebClient.Builder to return our mocked WebClient instance
        every { webClientBuilder.baseUrl("https://$auth0Domain") } returns webClientBuilder
        every { webClientBuilder.build() } returns webClient

        // Mock the chain of calls using the class-level mocks
        every { webClient.get() } returns requestHeadersUriSpec
        every { requestHeadersUriSpec.uri(any<String>()) } returns requestHeadersSpec
        every { requestHeadersSpec.header(HttpHeaders.AUTHORIZATION, "Bearer $managementToken") } returns requestHeadersSpec
        every { requestHeadersSpec.retrieve() } returns responseSpec

        if (shouldThrow) {
            every { responseSpec.bodyToFlux(Auth0UserDTO::class.java) } returns Flux.error(RuntimeException("WebClient error"))
        } else {
            val mockFlux = mockk<Flux<Auth0UserDTO>>()
            val mockMonoList = mockk<Mono<List<Auth0UserDTO>>>()

            every { responseSpec.bodyToFlux(Auth0UserDTO::class.java) } returns mockFlux
            every { mockFlux.collectList() } returns mockMonoList
            every { mockMonoList.block() } returns expectedUsers
        }
    }

    @Test
    fun `test getUsers returns users from auth0`() {
        // Given
        val users = listOf(Auth0UserDTO("1", "test@test.com", "Test User", "test", "pic"))
        mockWebClientChain(expectedUsers = users)

        // When
        val result = auth0Connector.getUsers()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test User", result[0].name)
        verify(exactly = 1) { requestHeadersUriSpec.uri("/api/v2/users?per_page=100") }
    }

    @Test
    fun `test getUsers with search returns filtered users from auth0`() {
        // Given
        val users = listOf(Auth0UserDTO("1", "test@test.com", "Test User", "test", "pic"))
        mockWebClientChain(expectedUsers = users)

        // When
        val result = auth0Connector.getUsers("Test")

        // Then
        assertEquals(1, result.size)
        assertEquals("Test User", result[0].name)
        verify(exactly = 1) { requestHeadersUriSpec.uri("/api/v2/users?q=Test&search_engine=v3&per_page=100") }
    }

    @Test
    fun `test getUsers returns mock users on failure`() {
        // Given
        mockWebClientChain(shouldThrow = true)
        // Ensure auth0Connector is initialized with a non-blank token so it attempts the external call
        val failingAuth0Connector = Auth0Connector(webClientBuilder, auth0Domain, managementToken)

        // When
        val result = failingAuth0Connector.getUsers()

        // Then
        assertEquals(5, result.size) // Expect mock users
        verify(exactly = 1) { requestHeadersSpec.retrieve() } // Verify it tried to make a call
    }

    @Test
    fun `test getUsers returns mock users when token is blank`() {
        // Given
        val blankTokenAuth0Connector = Auth0Connector(webClientBuilder, auth0Domain, "") // Blank token

        // When
        val result = blankTokenAuth0Connector.getUsers()

        // Then
        assertEquals(5, result.size) // Expect mock users
        verify(exactly = 0) { webClient.get() } // No external call should be made
        verify(exactly = 0) { webClientBuilder.baseUrl(any<String>()) } // Base URL should not be built
    }
}
