package com.ingsisteam.snippetservice2025.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.support.MissingServletRequestPartException
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    data class ErrorResponse(
        val timestamp: String = LocalDateTime.now().toString(),
        val status: Int,
        val error: String,
        val message: String,
        val details: List<String>? = null,
        val path: String,
    )

    @ExceptionHandler(SnippetNotFoundException::class)
    fun handleSnippetNotFound(
        ex: SnippetNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Snippet No Encontrado",
            message = ex.message ?: "El snippet solicitado no existe",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Snippet not found: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(TestNotFoundException::class)
    fun handleTestNotFound(
        ex: TestNotFoundException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Test No Encontrado",
            message = ex.message ?: "El test solicitado no existe",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Test not found: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(PermissionDeniedException::class)
    fun handlePermissionDenied(
        ex: PermissionDeniedException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Permiso Denegado",
            message = ex.message ?: "No tienes permisos para realizar esta acción",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Permission denied: {}", ex.message)
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        ex: UnauthorizedException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "No Autorizado",
            message = ex.message ?: "No estás autorizado para acceder a este recurso",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Unauthorized: {}", ex.message)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(SyntaxValidationException::class)
    fun handleSyntaxValidation(
        ex: SyntaxValidationException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Error de Sintaxis",
            message = "El código contiene errores de sintaxis",
            details = listOf(
                "Línea ${ex.line}, Columna ${ex.column}: ${ex.message}",
                "Regla violada: ${ex.rule}",
            ),
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Syntax validation error at line {}, column {}: {}", ex.line, ex.column, ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(
        ex: MethodArgumentNotValidException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errors = mutableListOf<String>()

        // Capturar errores de validación de campos
        ex.bindingResult.fieldErrors.forEach { error ->
            val fieldName = error.field
            val errorMessage = error.defaultMessage ?: "Valor inválido"
            errors.add("Campo '$fieldName': $errorMessage")
        }

        // Capturar errores globales del objeto
        ex.bindingResult.globalErrors.forEach { error ->
            val errorMessage = error.defaultMessage ?: "Error de validación"
            errors.add(errorMessage)
        }

        // Detectar específicamente el error de parámetro null en Kotlin
        if (ex.message?.contains("Parameter specified as non-null is null") == true) {
            val paramMatch = Regex("parameter (\\w+)").find(ex.message ?: "")
            val paramName = paramMatch?.groupValues?.get(1) ?: "desconocido"
            errors.add("El parámetro '$paramName' es obligatorio y no puede ser null")
        }

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Error de Validación",
            message = "La solicitud contiene datos inválidos o incompletos",
            details = errors,
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Validation error: {}", errors)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingPart(
        ex: MissingServletRequestPartException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Parámetro Faltante",
            message = "Falta el parámetro '${ex.requestPartName}' en la solicitud",
            details = listOf(
                "Se esperaba un parámetro de tipo multipart/form-data con nombre '${ex.requestPartName}'",
                "Verifica que el nombre del campo en el formulario coincida exactamente",
            ),
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Missing request part: {}", ex.requestPartName)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Argumento Inválido",
            message = ex.message ?: "Argumento inválido en la solicitud",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Illegal argument: {}", ex.message)
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(
        ex: NoSuchElementException,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Recurso No Encontrado",
            message = ex.message ?: "El recurso solicitado no existe",
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.warn("Resource not found: {}", ex.message)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Error Interno del Servidor",
            message = "Ocurrió un error inesperado al procesar la solicitud",
            details = listOf(ex.message ?: "Error desconocido"),
            path = request.getDescription(false).replace("uri=", ""),
        )

        logger.error("Unhandled exception: ", ex)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
