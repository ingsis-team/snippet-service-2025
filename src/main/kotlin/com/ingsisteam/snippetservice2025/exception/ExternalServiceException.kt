package com.ingsisteam.snippetservice2025.exception

class ExternalServiceException(
    message: String,
    val service: String,
    val operation: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
