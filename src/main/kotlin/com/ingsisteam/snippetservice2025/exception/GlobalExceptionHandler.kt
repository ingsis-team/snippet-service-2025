package com.ingsisteam.snippetservice2025.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    data class ErrorResponse(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val status: Int,
        val error: String,
        val message: String,
        val path: String? = null,
    )

    data class SyntaxErrorResponse(
        val timestamp: LocalDateTime = LocalDateTime.now(),
        val status: Int,
        val error: String,
        val message: String,
        val rule: String,
        val line: Int,
        val column: Int,
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.allErrors.map { error ->
            val fieldName = (error as FieldError).field
            val errorMessage = error.defaultMessage
            "$fieldName: $errorMessage"
        }.joinToString(", ")

        logger.warn("Validation failed: {}", errors)

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = errors,
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(SyntaxValidationException::class)
    fun handleSyntaxValidationException(ex: SyntaxValidationException): ResponseEntity<SyntaxErrorResponse> {
        logger.warn("Syntax validation failed: {} at line {}, column {}", ex.rule, ex.line, ex.column)

        val errorResponse = SyntaxErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Syntax Validation Failed",
            message = ex.message ?: "Invalid syntax in uploaded file",
            rule = ex.rule,
            line = ex.line,
            column = ex.column,
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        logger.warn("Bad request: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument",
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
        logger.warn("Resource not found: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "Resource not found",
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        // Log the full exception for debugging
        ex.printStackTrace()
        logger.error("Unexpected error: {}", ex.message, ex)

        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorizedException(ex: UnauthorizedException): ResponseEntity<ErrorResponse> {
        logger.warn("Unauthorized access: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            error = "Unauthorized",
            message = ex.message,
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse)
    }

    @ExceptionHandler(PermissionDeniedException::class)
    fun handlePermissionDeniedException(ex: PermissionDeniedException): ResponseEntity<ErrorResponse> {
        logger.warn("Permission denied: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            error = "Permission Denied",
            message = ex.message,
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse)
    }

    @ExceptionHandler(SnippetNotFoundException::class)
    fun handleSnippetNotFoundException(ex: SnippetNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Snippet not found: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Snippet Not Found",
            message = ex.message,
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    @ExceptionHandler(TestNotFoundException::class)
    fun handleTestNotFoundException(ex: TestNotFoundException): ResponseEntity<ErrorResponse> {
        logger.warn("Test not found: {}", ex.message)

        val errorResponse = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            error = "Test Not Found",
            message = ex.message,
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }
}
