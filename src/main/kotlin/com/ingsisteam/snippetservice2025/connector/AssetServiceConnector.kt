package com.ingsisteam.snippetservice2025.connector

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.net.InetAddress

@Service
class AssetServiceConnector(
    private val webClient: WebClient.Builder,
    @Value("\${asset.url}") private val assetUrl: String,
) {
    private val logger = LoggerFactory.getLogger(AssetServiceConnector::class.java)

    private val baseUrl: String by lazy {
        logger.info("Initializing AssetServiceConnector with assetUrl from config: '{}'", assetUrl)

        // Ensure the URL has http:// prefix if not already present
        var url = if (assetUrl.startsWith("http://") || assetUrl.startsWith("https://")) {
            logger.debug("Asset URL already has protocol prefix: {}", assetUrl)
            assetUrl
        } else {
            val urlWithProtocol = "http://$assetUrl"
            logger.debug("Added http:// prefix to asset URL: {} -> {}", assetUrl, urlWithProtocol)
            urlWithProtocol
        }

        // If hostname contains underscore (invalid in URIs), resolve to IP address
        try {
            logger.debug("Parsing URL to extract hostname: {}", url)
            // Parse URL manually to extract hostname (since URI constructor fails on underscores)
            val urlPattern = Regex("""(https?://)([^:/]+)(:(\d+))?(/.*)?$""")
            val match = urlPattern.find(url)
            if (match != null) {
                val host = match.groupValues[2]
                val port = match.groupValues[4].takeIf { it.isNotEmpty() } ?: "80"
                logger.debug("Extracted hostname: '{}', port: '{}'", host, port)

                if (host.contains("_")) {
                    logger.info("Hostname '{}' contains underscore (invalid in URIs), resolving to IP address", host)
                    try {
                        val ipAddress = InetAddress.getByName(host).hostAddress
                        logger.info("Successfully resolved hostname '{}' to IP address: {}", host, ipAddress)
                        url = url.replace(host, ipAddress)
                        logger.info("Updated URL after hostname resolution: {}", url)
                    } catch (dnsException: Exception) {
                        logger.error("DNS resolution failed for hostname '{}': {}", host, dnsException.message, dnsException)
                        throw dnsException
                    }
                } else {
                    logger.debug("Hostname '{}' does not contain underscore, no resolution needed", host)
                }
            } else {
                logger.warn("Could not parse URL pattern from: {}", url)
            }
        } catch (e: Exception) {
            logger.error("Error during hostname resolution for URL '{}': {}", url, e.message, e)
            logger.warn("Will attempt to use original URL: {}", url)
        }

        logger.info("Asset service base URL configured (final): {}", url)
        url
    }

    private val client: WebClient by lazy {
        // Don't set baseUrl - we'll build full URIs using UriComponentsBuilder
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
        logger.info("=== Starting storeSnippet operation ===")
        logger.info("Parameters: snippetId={}, contentLength={}", snippetId, content.length)
        logger.debug("Content preview (first 100 chars): {}", content.take(100))

        return try {
            logger.debug("Building URI from baseUrl: '{}'", baseUrl)
            logger.debug("Path component: /v1/asset/snippets/{}", snippetId)

            val uri = try {
                UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path("/v1/asset/snippets/$snippetId")
                    .build()
                    .toUri()
            } catch (uriException: Exception) {
                logger.error(
                    "Failed to build URI from baseUrl '{}' and path '/v1/asset/snippets/{}': {}",
                    baseUrl,
                    snippetId,
                    uriException.message,
                    uriException,
                )
                throw uriException
            }

            logger.info("Successfully built URI: {}", uri)
            logger.debug(
                "URI scheme: {}, host: {}, port: {}, path: {}",
                uri.scheme,
                uri.host,
                uri.port,
                uri.path,
            )

            val bearerToken = currentBearerToken()
            logger.debug("Bearer token present: {}", bearerToken != null)

            logger.info("Sending PUT request to asset service...")
            val responseStatus = try {
                client.put()
                    .uri(uri)
                    .headers { headers -> bearerToken?.let { headers.setBearerAuth(it) } }
                    .contentType(MediaType.TEXT_PLAIN)
                    .bodyValue(content)
                    .exchangeToMono { clientResponse ->
                        logger.debug("Received response with status: {}", clientResponse.statusCode())
                        Mono.just(clientResponse.statusCode())
                    }
                    .block()
            } catch (requestException: Exception) {
                logger.error("Request to asset service failed: {}", requestException.message, requestException)
                logger.error("Request details - URI: {}, Method: PUT, ContentType: TEXT_PLAIN", uri)
                throw requestException
            }

            logger.info("Received response status: {}", responseStatus)
            logger.debug("Response from asset service: status={}", responseStatus)

            when (responseStatus) {
                HttpStatus.CREATED, HttpStatus.OK -> {
                    logger.info(
                        "✓ Snippet stored successfully in asset service: snippetId={}, status={}",
                        snippetId,
                        responseStatus,
                    )
                    true
                }
                else -> {
                    logger.error(
                        "✗ Failed to store snippet in asset service. Status: {}, snippetId={}",
                        responseStatus,
                        snippetId,
                    )
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("✗ Exception during storeSnippet operation", e)
            logger.error(
                "Error details - snippetId: {}, error type: {}, message: {}",
                snippetId,
                e.javaClass.simpleName,
                e.message,
            )
            if (e.cause != null) {
                logger.error("Caused by: {} - {}", e.cause!!.javaClass.simpleName, e.cause!!.message)
            }
            false
        } finally {
            logger.info("=== Completed storeSnippet operation ===")
        }
    }

    /**
     * Retrieve snippet content from the asset service
     * @param snippetId The unique identifier for the snippet
     * @return The snippet content or null if not found
     */
    fun getSnippet(snippetId: String): String? {
        logger.info("=== Starting getSnippet operation ===")
        logger.info("Parameters: snippetId={}", snippetId)

        return try {
            logger.debug("Building URI from baseUrl: '{}'", baseUrl)
            val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/asset/snippets/$snippetId")
                .build()
                .toUri()
            logger.info("Successfully built URI: {}", uri)

            logger.info("Sending GET request to asset service...")
            val content = client.get()
                .uri(uri)
                .headers { headers -> currentBearerToken()?.let { headers.setBearerAuth(it) } }
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            logger.info(
                "✓ Snippet retrieved successfully from asset service: snippetId={}, contentLength={}",
                snippetId,
                content?.length ?: 0,
            )
            content
        } catch (e: org.springframework.web.reactive.function.client.WebClientResponseException.NotFound) {
            logger.warn("Snippet not found in asset service: snippetId={}", snippetId)
            null
        } catch (e: Exception) {
            logger.error(
                "✗ Error retrieving snippet from asset service: snippetId={}, error={}",
                snippetId,
                e.message,
                e,
            )
            null
        } finally {
            logger.info("=== Completed getSnippet operation ===")
        }
    }

    /**
     * Delete snippet content from the asset service
     * @param snippetId The unique identifier for the snippet
     * @return true if successfully deleted or not found, false on error
     */
    fun deleteSnippet(snippetId: String): Boolean {
        logger.info("=== Starting deleteSnippet operation ===")
        logger.info("Parameters: snippetId={}", snippetId)

        return try {
            logger.debug("Building URI from baseUrl: '{}'", baseUrl)
            val uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/v1/asset/snippets/$snippetId")
                .build()
                .toUri()
            logger.info("Successfully built URI: {}", uri)

            logger.info("Sending DELETE request to asset service...")
            val responseStatus = client.delete()
                .uri(uri)
                .headers { headers -> currentBearerToken()?.let { headers.setBearerAuth(it) } }
                .exchangeToMono { clientResponse ->
                    logger.debug("Received response with status: {}", clientResponse.statusCode())
                    Mono.just(clientResponse.statusCode())
                }
                .block()

            logger.info("Received response status: {}", responseStatus)
            logger.debug("Response from asset service: status={}", responseStatus)

            when (responseStatus) {
                HttpStatus.NO_CONTENT, HttpStatus.NOT_FOUND -> {
                    logger.info(
                        "✓ Snippet deleted from asset service: snippetId={}, status={}",
                        snippetId,
                        responseStatus,
                    )
                    true
                }
                else -> {
                    logger.error(
                        "✗ Failed to delete snippet from asset service. Status: {}, snippetId={}",
                        responseStatus,
                        snippetId,
                    )
                    false
                }
            }
        } catch (e: Exception) {
            logger.error("✗ Exception during deleteSnippet operation", e)
            logger.error(
                "Error details - snippetId: {}, error type: {}, message: {}",
                snippetId,
                e.javaClass.simpleName,
                e.message,
            )
            false
        } finally {
            logger.info("=== Completed deleteSnippet operation ===")
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
