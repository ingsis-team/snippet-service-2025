package com.ingsisteam.snippetservice2025

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
@ActiveProfiles("test")
class SnippetService2025ApplicationTests {

    // Evita que Spring construya un JwtDecoder real (y llame a Auth0)
    @MockitoBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun contextLoads() {
    }
}
