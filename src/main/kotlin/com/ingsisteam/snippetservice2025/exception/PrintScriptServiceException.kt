package com.ingsisteam.snippetservice2025.exception

class PrintScriptServiceException(
    message: String,
    val operation: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
