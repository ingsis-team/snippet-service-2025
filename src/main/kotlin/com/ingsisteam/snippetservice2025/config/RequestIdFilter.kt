package com.ingsisteam.snippetservice2025.config

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Filtro que genera y propaga un Request ID único para cada petición HTTP.
 * El Request ID se agrega al MDC de SLF4J para que aparezca en todos los logs.
 */
@Component
@Order(1)
class RequestIdFilter : Filter {
    private val logger = LoggerFactory.getLogger(RequestIdFilter::class.java)

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-ID"
        const val REQUEST_ID_MDC_KEY = "requestId"
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        try {
            // Intentar obtener el Request ID del header, o generar uno nuevo
            val requestId = httpRequest.getHeader(REQUEST_ID_HEADER)
                ?: UUID.randomUUID().toString().substring(0, 8)

            // Agregar Request ID al MDC para que aparezca en todos los logs
            MDC.put(REQUEST_ID_MDC_KEY, requestId)

            // Agregar Request ID al response header para trazabilidad
            httpResponse.setHeader(REQUEST_ID_HEADER, requestId)

            logger.info(
                "\nIncoming request: {} {} from {}",
                httpRequest.method,
                httpRequest.requestURI,
                httpRequest.remoteAddr,
            )

            chain.doFilter(request, response)

            logger.info(
                "Request completed: {} {} - Status: {}\n",
                httpRequest.method,
                httpRequest.requestURI,
                httpResponse.status,
            )
        } finally {
            // Limpiar el MDC después de procesar la request
            MDC.remove(REQUEST_ID_MDC_KEY)
        }
    }
}
