package com.ingsisteam.snippetservice2025.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

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

        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Validation Failed",
            message = errors,
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(SyntaxValidationException::class)
    fun handleSyntaxValidationException(ex: SyntaxValidationException): ResponseEntity<SyntaxErrorResponse> {
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
        val errorResponse = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "Invalid argument",
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNoSuchElementException(ex: NoSuchElementException): ResponseEntity<ErrorResponse> {
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
        println("❌ [EXCEPTION] ${ex.javaClass.simpleName}: ${ex.message}")
        ex.printStackTrace()
        
        val errorResponse = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = ex.message ?: "An unexpected error occurred",
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
