package com.ingsisteam.snippetservice2025.model.dto.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Auth0UserDTOTest {

    @Test
    fun `test Auth0UserDTO all properties`() {
        val dto = Auth0UserDTO(
            userId = "auth0|12345",
            email = "test@example.com",
            name = "Test User",
            nickname = "tester",
            picture = "http://example.com/pic.jpg",
        )

        assertEquals("auth0|12345", dto.userId)
        assertEquals("test@example.com", dto.email)
        assertEquals("Test User", dto.name)
        assertEquals("tester", dto.nickname)
        assertEquals("http://example.com/pic.jpg", dto.picture)
    }

    @Test
    fun `test Auth0UserDTO with only required property`() {
        val dto = Auth0UserDTO(
            userId = "auth0|67890",
        )

        assertEquals("auth0|67890", dto.userId)
        assertEquals(null, dto.email)
        assertEquals(null, dto.name)
        assertEquals(null, dto.nickname)
        assertEquals(null, dto.picture)
    }

    @Test
    fun `test Auth0UserDTO with some nullable properties`() {
        val dto = Auth0UserDTO(
            userId = "auth0|abcde",
            email = "another@example.com",
            name = "Another User",
        )

        assertEquals("auth0|abcde", dto.userId)
        assertEquals("another@example.com", dto.email)
        assertEquals("Another User", dto.name)
        assertEquals(null, dto.nickname)
        assertEquals(null, dto.picture)
    }
}
