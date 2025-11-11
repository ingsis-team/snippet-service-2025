package com.ingsisteam.snippetservice2025.exception

/**
 * Excepción lanzada cuando no se encuentra un snippet
 */
class SnippetNotFoundException(
    override val message: String,
) : RuntimeException(message)
