package com.ingsisteam.snippetservice2025.exception

/**
 * Excepción lanzada cuando un usuario no tiene los permisos necesarios
 */
class PermissionDeniedException(
    override val message: String,
) : RuntimeException(message)
