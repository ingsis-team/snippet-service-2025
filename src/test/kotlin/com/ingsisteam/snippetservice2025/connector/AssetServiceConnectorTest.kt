package com.ingsisteam.snippetservice2025.connector

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.reactive.function.client.WebClient

class AssetServiceConnectorTest {

    private lateinit var server: MockWebServer
    private lateinit var connector: AssetServiceConnector

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val webClientBuilder = WebClient.builder()
        val assetUrl = server.url("/").toString().removeSuffix("/")
        connector = AssetServiceConnector(webClientBuilder, assetUrl)
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        SecurityContextHolder.clearContext() // Clear context after each test
        clearAllMocks() // Clear all mocks after each test
    }

    private fun setupSecurityContextWithToken(tokenValue: String) {
        val jwt = mockk<Jwt>(relaxed = true) // Make it relaxed
        every { jwt.tokenValue } returns tokenValue
        val authentication = JwtAuthenticationToken(jwt, emptyList())
        val securityContext = mockk<SecurityContext>(relaxed = true)
        every { securityContext.authentication } returns authentication
        SecurityContextHolder.setContext(securityContext)
    }

    private fun setupSecurityContextWithoutToken() {
        val securityContext = mockk<SecurityContext>(relaxed = true)
        every { securityContext.authentication } returns null
        SecurityContextHolder.setContext(securityContext)
    }

    @Test
    fun `test storeSnippet success`() {
        // Given
        val snippetId = "123"
        val content = "println(\"hello world\")"
        server.enqueue(MockResponse().setResponseCode(201))

        // When
        val result = connector.storeSnippet(snippetId, content)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("PUT", request.method)
        assertEquals(content, request.body.readUtf8())
    }

    @Test
    fun `test storeSnippet with bearer token success`() {
        // Given
        val snippetId = "123"
        val content = "println(\"hello world\")"
        val token = "test-bearer-token"
        setupSecurityContextWithToken(token)
        server.enqueue(MockResponse().setResponseCode(201))

        // When
        val result = connector.storeSnippet(snippetId, content)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("PUT", request.method)
        assertEquals(content, request.body.readUtf8())
        assertEquals("Bearer $token", request.headers["Authorization"])
    }

    @Test
    fun `test storeSnippet without bearer token success`() {
        // Given
        val snippetId = "123"
        val content = "println(\"hello world\")"
        setupSecurityContextWithoutToken() // Ensure no token is present
        server.enqueue(MockResponse().setResponseCode(201))

        // When
        val result = connector.storeSnippet(snippetId, content)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("PUT", request.method)
        assertEquals(content, request.body.readUtf8())
        assertNull(request.headers["Authorization"])
    }

    @Test
    fun `test storeSnippet server error`() {
        // Given
        val snippetId = "123"
        val content = "println(\"hello world\")"
        server.enqueue(MockResponse().setResponseCode(500))

        // When
        val result = connector.storeSnippet(snippetId, content)

        // Then
        assertTrue(!result)
    }

    @Test
    fun `test getSnippet success`() {
        // Given
        val snippetId = "123"
        val content = "println(\"hello world\")"
        server.enqueue(MockResponse().setResponseCode(200).setBody(content))

        // When
        val result = connector.getSnippet(snippetId)

        // Then
        assertEquals(content, result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("GET", request.method)
    }

    @Test
    fun `test getSnippet not found`() {
        // Given
        val snippetId = "123"
        server.enqueue(MockResponse().setResponseCode(404))

        // When
        val result = connector.getSnippet(snippetId)

        // Then
        assertNull(result)
    }

    @Test
    fun `test getSnippet server error`() {
        // Given
        val snippetId = "123"
        server.enqueue(MockResponse().setResponseCode(500))

        // When
        val result = connector.getSnippet(snippetId)

        // Then
        assertNull(result)
    }

    @Test
    fun `test getSnippet with bearer token success`() {
        // Given
        val snippetId = "123"
        val content = "retrieved content"
        val token = "test-bearer-token"
        setupSecurityContextWithToken(token)
        server.enqueue(MockResponse().setResponseCode(200).setBody(content))

        // When
        val result = connector.getSnippet(snippetId)

        // Then
        assertEquals(content, result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("GET", request.method)
        assertEquals("Bearer $token", request.headers["Authorization"])
    }

    @Test
    fun `test getSnippet without bearer token success`() {
        // Given
        val snippetId = "123"
        val content = "retrieved content"
        setupSecurityContextWithoutToken()
        server.enqueue(MockResponse().setResponseCode(200).setBody(content))

        // When
        val result = connector.getSnippet(snippetId)

        // Then
        assertEquals(content, result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("GET", request.method)
        assertNull(request.headers["Authorization"])
    }

    @Test
    fun `test deleteSnippet success`() {
        // Given
        val snippetId = "123"
        server.enqueue(MockResponse().setResponseCode(204))

        // When
        val result = connector.deleteSnippet(snippetId)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("DELETE", request.method)
    }

    @Test
    fun `test deleteSnippet not found`() {
        // Given
        val snippetId = "123"
        server.enqueue(MockResponse().setResponseCode(404))

        // When
        val result = connector.deleteSnippet(snippetId)

        // Then
        assertTrue(result)
    }

    @Test
    fun `test deleteSnippet server error`() {
        // Given
        val snippetId = "123"
        server.enqueue(MockResponse().setResponseCode(500))

        // When
        val result = connector.deleteSnippet(snippetId)

        // Then
        assertTrue(!result)
    }

    @Test
    fun `test deleteSnippet with bearer token success`() {
        // Given
        val snippetId = "123"
        val token = "test-bearer-token"
        setupSecurityContextWithToken(token)
        server.enqueue(MockResponse().setResponseCode(204))

        // When
        val result = connector.deleteSnippet(snippetId)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("DELETE", request.method)
        assertEquals("Bearer $token", request.headers["Authorization"])
    }

    @Test
    fun `test deleteSnippet without bearer token success`() {
        // Given
        val snippetId = "123"
        setupSecurityContextWithoutToken()
        server.enqueue(MockResponse().setResponseCode(204))

        // When
        val result = connector.deleteSnippet(snippetId)

        // Then
        assertTrue(result)
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
        assertEquals("DELETE", request.method)
        assertNull(request.headers["Authorization"])
    }

    @Test
    fun `test updateSnippet success`() {
        // Given
        val snippetId = "123"
        val content = "new content"
        server.enqueue(MockResponse().setResponseCode(204)) // for delete
        server.enqueue(MockResponse().setResponseCode(201)) // for store

        // When
        val result = connector.updateSnippet(snippetId, content)

        // Then
        assertTrue(result)
        val deleteRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", deleteRequest.path)
        assertEquals("DELETE", deleteRequest.method)
        val storeRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", storeRequest.path)
        assertEquals("PUT", storeRequest.method)
        assertEquals(content, storeRequest.body.readUtf8())
    }

    @Test
    fun `test baseUrl initialization with no protocol`() {
        // Given
        val webClientBuilder = WebClient.builder()
        val assetUrl = "localhost:${server.port}"
        val connector = AssetServiceConnector(webClientBuilder, assetUrl)

        // When
        val snippetId = "123"
        val content = "println(\"hello world\")"
        server.enqueue(MockResponse().setResponseCode(201))
        connector.storeSnippet(snippetId, content)

        // Then
        val request = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", request.path)
    }

    @Test
    fun `test updateSnippet delete fails`() {
        // Given
        val snippetId = "123"
        val content = "new content"
        server.enqueue(MockResponse().setResponseCode(500)) // for delete (failure)
        server.enqueue(MockResponse().setResponseCode(500)) // for store (this will still be called as per implementation)

        // When
        val result = connector.updateSnippet(snippetId, content)

        // Then
        assertTrue(!result)
        // Both delete and store requests should have been made
        val deleteRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", deleteRequest.path)
        assertEquals("DELETE", deleteRequest.method)
        val storeRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", storeRequest.path)
        assertEquals("PUT", storeRequest.method)
        assertEquals(content, storeRequest.body.readUtf8())
    }

    @Test
    fun `test updateSnippet store fails after successful delete`() {
        // Given
        val snippetId = "123"
        val content = "new content"
        server.enqueue(MockResponse().setResponseCode(204)) // for delete (success)
        server.enqueue(MockResponse().setResponseCode(500)) // for store (failure)

        // When
        val result = connector.updateSnippet(snippetId, content)

        // Then
        assertTrue(!result)
        // Both delete and store requests should have been made
        val deleteRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", deleteRequest.path)
        assertEquals("DELETE", deleteRequest.method)
        val storeRequest = server.takeRequest()
        assertEquals("/v1/asset/snippets/$snippetId", storeRequest.path)
        assertEquals("PUT", storeRequest.method)
        assertEquals(content, storeRequest.body.readUtf8())
    }
}
