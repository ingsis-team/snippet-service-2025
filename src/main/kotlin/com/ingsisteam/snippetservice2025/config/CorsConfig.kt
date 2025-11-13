package com.ingsisteam.snippetservice2025.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Allow requests from the frontend
        configuration.allowedOrigins = listOf(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:3000",
        )

        // Allow all HTTP methods
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")

        // Allow all headers
        configuration.allowedHeaders = listOf("*")

        // Allow credentials (cookies, authorization headers, etc.)
        configuration.allowCredentials = true

        // How long the browser should cache the preflight response
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
