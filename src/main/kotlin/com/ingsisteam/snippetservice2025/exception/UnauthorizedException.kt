package com.ingsisteam.snippetservice2025.exception

/**
 * Excepción lanzada cuando un usuario no tiene autorización para realizar una acción
 */
class UnauthorizedException(
    override val message: String,
) : RuntimeException(message)
