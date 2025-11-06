package com.ingsisteam.snippetservice2025.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(1)
class RequestLoggingFilter : Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val method = httpRequest.method
        val uri = httpRequest.requestURI
        val origin = httpRequest.getHeader("Origin") ?: "No Origin"

        println("🌐 [REQUEST] $method $uri from origin: $origin")

        chain.doFilter(request, response)

        println("📤 [RESPONSE] Status: ${httpResponse.status} for $method $uri")
    }
}
