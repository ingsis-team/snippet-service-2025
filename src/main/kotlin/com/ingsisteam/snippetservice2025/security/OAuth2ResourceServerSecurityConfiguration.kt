package com.ingsisteam.snippetservice2025.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/**
 * Configuración de seguridad para el recurso OAuth2.
 * Configura la validación de tokens JWT y las reglas de autorización para las solicitudes entrantes.
 */
@Configuration
@EnableWebSecurity
class OAuth2ResourceServerSecurityConfiguration(
    @Value("\${auth0.audience:}")
    val audience: String,
    @Value("\${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    val issuer: String,
    private val corsConfigurationSource: CorsConfigurationSource,
) {

    /**
     * Configura la cadena de filtros de seguridad.
     * Define las reglas de autorización, la configuración del servidor de recursos OAuth2 y CORS.
     */
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it
                .requestMatchers("/").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
        }
            .oauth2ResourceServer { it.jwt(withDefaults()) }
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
        return http.build()
    }

    /**
     * Configura el decodificador de JWT con validación personalizada.
     * Incluye validación de emisor y audiencia.
     */
    @Bean
    fun jwtDecoder(): JwtDecoder? {
        if (issuer.isNullOrBlank()) {
            return null
        }
        val jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuer).build()
        val audienceValidator: OAuth2TokenValidator<Jwt> = AudienceValidator(audience)
        val withIssuer: OAuth2TokenValidator<Jwt> = JwtValidators.createDefaultWithIssuer(issuer)
        val withAudience: OAuth2TokenValidator<Jwt> = DelegatingOAuth2TokenValidator(withIssuer, audienceValidator)
        jwtDecoder.setJwtValidator(withAudience)
        return jwtDecoder
    }
}
