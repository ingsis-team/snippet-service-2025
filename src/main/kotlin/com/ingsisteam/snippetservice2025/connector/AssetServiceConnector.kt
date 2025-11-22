package com.ingsisteam.snippetservice2025.connector

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

@Component
class AssetServiceConnector(
    @Value("\${asset.url}") private val assetUrl: String,
) {
    private val logger = LoggerFactory.getLogger(AssetServiceConnector::class.java)
    private val assetServiceApi = WebClient.builder().baseUrl("http://$assetUrl/v1/asset").build()

    companion object {
        const val SNIPPETS_CONTAINER = "snippets"
    }

    /**
     * Stores snippet content in the asset service blob storage
     * @param snippetId the ID of the snippet
     * @param content the content to store
     * @return true if the operation was successful
     */
    fun storeSnippetContent(snippetId: Long, content: String): Boolean {
        logger.info("Storing snippet content to asset service: snippetId={}", snippetId)
        return try {
            val response = assetServiceApi
                .put()
                .uri("/{container}/{key}", SNIPPETS_CONTAINER, snippetId.toString())
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(content)
                .exchangeToMono { clientResponse ->
                    logger.debug("Asset service response status: {}", clientResponse.statusCode())
                    Mono.just(clientResponse.statusCode())
                }
                .block()

            val success = response == HttpStatus.CREATED || response == HttpStatus.OK
            if (success) {
                logger.info("Successfully stored snippet content: snippetId={}", snippetId)
            } else {
                logger.error("Failed to store snippet content. Status: {}", response)
            }
            success
        } catch (e: Exception) {
            logger.error("Error storing snippet content to asset service: snippetId={}, error={}", snippetId, e.message, e)
            throw RuntimeException("Failed to store snippet content: ${e.message}", e)
        }
    }

    /**
     * Retrieves snippet content from the asset service blob storage
     * @param snippetId the ID of the snippet
     * @return the content of the snippet
     */
    fun getSnippetContent(snippetId: Long): String {
        logger.info("Retrieving snippet content from asset service: snippetId={}", snippetId)
        return try {
            val content = assetServiceApi
                .get()
                .uri("/{container}/{key}", SNIPPETS_CONTAINER, snippetId.toString())
                .accept(MediaType.TEXT_PLAIN)
                .exchangeToMono { clientResponse ->
                    if (clientResponse.statusCode() == HttpStatus.OK) {
                        clientResponse.bodyToFlux(org.springframework.core.io.buffer.DataBuffer::class.java)
                            .let { flux ->
                                DataBufferUtils.join(flux)
                                    .map { dataBuffer ->
                                        val bytes = ByteArray(dataBuffer.readableByteCount())
                                        dataBuffer.read(bytes)
                                        DataBufferUtils.release(dataBuffer)
                                        String(bytes, StandardCharsets.UTF_8)
                                    }
                            }
                    } else {
                        logger.error("Failed to retrieve snippet content. Status: {}", clientResponse.statusCode())
                        Mono.error(RuntimeException("Snippet content not found in asset service"))
                    }
                }
                .block()

            logger.info("Successfully retrieved snippet content: snippetId={}, length={}", snippetId, content?.length ?: 0)
            content ?: throw RuntimeException("Snippet content is null")
        } catch (e: Exception) {
            logger.error("Error retrieving snippet content from asset service: snippetId={}, error={}", snippetId, e.message, e)
            throw RuntimeException("Failed to retrieve snippet content: ${e.message}", e)
        }
    }

    /**
     * Deletes snippet content from the asset service blob storage
     * @param snippetId the ID of the snippet
     * @return true if the operation was successful
     */
    fun deleteSnippetContent(snippetId: Long): Boolean {
        logger.info("Deleting snippet content from asset service: snippetId={}", snippetId)
        return try {
            val response = assetServiceApi
                .delete()
                .uri("/{container}/{key}", SNIPPETS_CONTAINER, snippetId.toString())
                .exchangeToMono { clientResponse ->
                    logger.debug("Asset service delete response status: {}", clientResponse.statusCode())
                    Mono.just(clientResponse.statusCode())
                }
                .block()

            // Consider both NO_CONTENT (successful delete) and NOT_FOUND (already deleted) as success
            val success = response == HttpStatus.NO_CONTENT || response == HttpStatus.NOT_FOUND
            if (success) {
                logger.info("Successfully deleted snippet content: snippetId={}", snippetId)
            } else {
                logger.warn("Unexpected status when deleting snippet content. Status: {}", response)
            }
            success
        } catch (e: Exception) {
            logger.error("Error deleting snippet content from asset service: snippetId={}, error={}", snippetId, e.message, e)
            // Don't throw - allow snippet deletion to proceed even if asset deletion fails
            false
        }
    }
}
