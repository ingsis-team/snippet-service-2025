package com.ingsisteam.snippetservice2025.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.web.cors.CorsConfigurationSource

class OAuth2ResourceServerSecurityConfigurationTest {

    private val corsConfigurationSource: CorsConfigurationSource = mockk()

    @Test
    fun `jwtDecoder should return null when issuer is blank`() {
        // Given
        val config = OAuth2ResourceServerSecurityConfiguration(
            audience = "test-audience",
            issuer = "",
            corsConfigurationSource = corsConfigurationSource,
        )

        // When
        val jwtDecoder = config.jwtDecoder()

        // Then
        assertNull(jwtDecoder)
    }

    @Test
    fun `jwtDecoder should return a JwtDecoder when issuer is not blank`() {
        // Given
        val issuer = "https://test-issuer.com"
        val config = OAuth2ResourceServerSecurityConfiguration(
            audience = "test-audience",
            issuer = issuer,
            corsConfigurationSource = corsConfigurationSource,
        )
        val mockJwtDecoder = mockk<NimbusJwtDecoder>(relaxed = true)
        val builder = mockk<NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder>()

        every { builder.build() } returns mockJwtDecoder
        mockkStatic(NimbusJwtDecoder::class)
        every { NimbusJwtDecoder.withIssuerLocation(issuer) } returns builder

        // When
        val jwtDecoder = config.jwtDecoder()

        // Then
        assertNotNull(jwtDecoder)
    }
}
