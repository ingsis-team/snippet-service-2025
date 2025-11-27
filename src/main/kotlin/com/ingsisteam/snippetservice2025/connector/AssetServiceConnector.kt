package com.ingsisteam.snippetservice2025.connector

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.net.URI

@Service
class AssetServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${asset.url}") private val assetUrl: String,
) {
    private val logger = LoggerFactory.getLogger(AssetServiceConnector::class.java)

    private val baseUrl: String by lazy {
        // Ensure the URL has http:// prefix if not already present
        val url = if (assetUrl.startsWith("http://") || assetUrl.startsWith("https://")) {
            assetUrl
        } else {
            "http://$assetUrl"
        }
        logger.info("Asset service base URL configured: {}", url)
        url
    }

    private val client: WebClient by lazy {
        // Don't set baseUrl - we'll use absolute URIs
        webClient.build()
    }

    private fun currentBearerToken(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication is JwtAuthenticationToken) {
            authentication.token.tokenValue
        } else {
            null
        }
    }

    /**
     * Store snippet content in the asset service
     * @param snippetId The unique identifier for the snippet
     * @param content The content to store
     * @return true if successful, false otherwise
     */
    fun storeSnippet(snippetId: String, content: String): Boolean {
        logger.debug("Storing snippet in asset service: snippetId={}", snippetId)

        return try {
            val fullUri = URI("$baseUrl/v1/asset/snippets/$snippetId")
            logger.debug("PUT request to: {}", fullUri)
            val responseStatus = client.put()
                .uri(fullUri)
                .headers { headers -> currentBearerToken()?.let { headers.setBearerAuth(it) } }
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(content)
                .exchangeToMono { clientResponse -> Mono.just(clientResponse.statusCode()) }
                .block()

            when (responseStatus) {
                HttpStatus.CREATED, HttpStatus.OK -> {
                    logger.info("Snippet stored successfully in asset service: snippetId={}", snippetId)
                    true
                }
                else -> {
                    logger.error("Failed to store snippet in asset service. Status: {}", responseStatus)
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("Error storing snippet in asset service: snippetId={}, error={}", snippetId, e.message, e)
            false
        }
    }

    /**
     * Retrieve snippet content from the asset service
     * @param snippetId The unique identifier for the snippet
     * @return The snippet content or null if not found
     */
    fun getSnippet(snippetId: String): String? {
        logger.debug("Retrieving snippet from asset service: snippetId={}", snippetId)

        return try {
            val fullUri = URI("$baseUrl/v1/asset/snippets/$snippetId")
            logger.debug("GET request to: {}", fullUri)
            val content = client.get()
                .uri(fullUri)
                .headers { headers -> currentBearerToken()?.let { headers.setBearerAuth(it) } }
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            logger.debug("Snippet retrieved successfully from asset service: snippetId={}", snippetId)
            content
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
            logger.warn("Snippet not found in asset service: snippetId={}", snippetId)
            null
        } catch (e: Exception) {
            logger.error("Error retrieving snippet from asset service: snippetId={}, error={}", snippetId, e.message, e)
            null
        }
    }

    /**
     * Delete snippet content from the asset service
     * @param snippetId The unique identifier for the snippet
     * @return true if successfully deleted or not found, false on error
     */
    fun deleteSnippet(snippetId: String): Boolean {
        logger.debug("Deleting snippet from asset service: snippetId={}", snippetId)

        return try {
            val fullUri = URI("$baseUrl/v1/asset/snippets/$snippetId")
            logger.debug("DELETE request to: {}", fullUri)
            val responseStatus = client.delete()
                .uri(fullUri)
                .headers { headers -> currentBearerToken()?.let { headers.setBearerAuth(it) } }
                .exchangeToMono { clientResponse -> Mono.just(clientResponse.statusCode()) }
                .block()

            when (responseStatus) {
                HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND -> {
                    logger.info("Snippet deleted from asset service: snippetId={}, status={}", snippetId, responseStatus)
                    true
                }
                else -> {
                    logger.error("Failed to delete snippet from asset service. Status: {}", responseStatus)
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("Error deleting snippet from asset service: snippetId={}, error={}", snippetId, e.message, e)
            false
        }
    }

    /**
     * Update snippet content in the asset service (delete + create)
     * @param snippetId The unique identifier for the snippet
     * @param content The new content to store
     * @return true if successful, false otherwise
     */
    fun updateSnippet(snippetId: String, content: String): Boolean {
        logger.debug("Updating snippet in asset service: snippetId={}", snippetId)

        // Delete existing content first
        deleteSnippet(snippetId)

        // Store new content
        return storeSnippet(snippetId, content)
    }
}
