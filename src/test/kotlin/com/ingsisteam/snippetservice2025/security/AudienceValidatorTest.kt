package com.ingsisteam.snippetservice2025.security

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt

class AudienceValidatorTest {

    private lateinit var audienceValidator: AudienceValidator
    private val audience = "test-audience"

    @BeforeEach
    fun setUp() {
        audienceValidator = AudienceValidator(audience)
    }

    @Test
    fun `test validate with valid audience`() {
        // Given
        val jwt = mockk<Jwt>()
        every { jwt.audience } returns listOf(audience)

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        assertTrue(result.hasErrors().not())
    }

    @Test
    fun `test validate with invalid audience`() {
        // Given
        val jwt = mockk<Jwt>()
        every { jwt.audience } returns listOf("invalid-audience")

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        assertTrue(result.hasErrors())
    }

    @Test
    fun `test validate with empty audience`() {
        // Given
        val jwt = mockk<Jwt>()
        every { jwt.audience } returns emptyList()

        // When
        val result = audienceValidator.validate(jwt)

        // Then
        assertTrue(result.hasErrors())
    }
}
