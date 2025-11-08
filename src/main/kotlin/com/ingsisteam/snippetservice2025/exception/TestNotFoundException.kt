package com.ingsisteam.snippetservice2025.exception

/**
 * Excepción lanzada cuando no se encuentra un test
 */
class TestNotFoundException(
    override val message: String,
) : RuntimeException(message)
