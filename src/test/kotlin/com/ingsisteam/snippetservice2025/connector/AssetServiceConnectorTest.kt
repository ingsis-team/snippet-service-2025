package com.ingsisteam.snippetservice2025.connector

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
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
    fun `test updateSnippet delete fails`() {
        // Given
        val snippetId = "123"
        val content = "new content"
        server.enqueue(MockResponse().setResponseCode(500)) // for delete
        server.enqueue(MockResponse().setResponseCode(500)) // for store

        // When
        val result = connector.updateSnippet(snippetId, content)

        // Then
        assertTrue(!result)
    }
}
