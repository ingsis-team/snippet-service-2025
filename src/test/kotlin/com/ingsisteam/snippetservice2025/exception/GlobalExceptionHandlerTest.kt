package com.ingsisteam.snippetservice2025.exception

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @Test
    fun `handleValidationExceptions should return 400 BAD_REQUEST`() {
        val fieldError = FieldError("objectName", "fieldName", "Default message")
        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.allErrors } returns listOf(fieldError)
        val methodParameter = MethodParameter(handler.javaClass.getMethod("handleValidationExceptions", MethodArgumentNotValidException::class.java), -1)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleValidationExceptions(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body?.status)
        assertEquals("Validation Failed", response.body?.error)
        assertEquals("fieldName: Default message", response.body?.message)
    }

    @Test
    fun `handleSyntaxValidationException should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("ruleName", 1, 10, "Invalid syntax")

        val response: ResponseEntity<GlobalExceptionHandler.SyntaxErrorResponse> = handler.handleSyntaxValidationException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body?.status)
        assertEquals("Syntax Validation Failed", response.body?.error)
        assertEquals("Invalid syntax", response.body?.message)
        assertEquals("ruleName", response.body?.rule)
        assertEquals(1, response.body?.line)
        assertEquals(10, response.body?.column)
    }

    @Test
    fun `handleIllegalArgumentException should return 400 BAD_REQUEST`() {
        val ex = IllegalArgumentException("Invalid argument provided")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleIllegalArgumentException(ex)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body?.status)
        assertEquals("Bad Request", response.body?.error)
        assertEquals("Invalid argument provided", response.body?.message)
    }

    @Test
    fun `handleNoSuchElementException should return 404 NOT_FOUND`() {
        val ex = NoSuchElementException("Resource not found in database")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleNoSuchElementException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body?.status)
        assertEquals("Not Found", response.body?.error)
        assertEquals("Resource not found in database", response.body?.message)
    }

    @Test
    fun `handleGenericException should return 500 INTERNAL_SERVER_ERROR`() {
        val ex = Exception("Something went wrong internally")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleGenericException(ex)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.body?.status)
        assertEquals("Internal Server Error", response.body?.error)
        assertEquals("Something went wrong internally", response.body?.message)
    }

    @Test
    fun `handleUnauthorizedException should return 401 UNAUTHORIZED`() {
        val ex = UnauthorizedException("Authentication required")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleUnauthorizedException(ex)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.body?.status)
        assertEquals("Unauthorized", response.body?.error)
        assertEquals("Authentication required", response.body?.message)
    }

    @Test
    fun `handlePermissionDeniedException should return 403 FORBIDDEN`() {
        val ex = PermissionDeniedException("Access denied for this resource")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handlePermissionDeniedException(ex)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals(HttpStatus.FORBIDDEN.value(), response.body?.status)
        assertEquals("Permission Denied", response.body?.error)
        assertEquals("Access denied for this resource", response.body?.message)
    }

    @Test
    fun `handleSnippetNotFoundException should return 404 NOT_FOUND`() {
        val ex = SnippetNotFoundException("Snippet with ID 1 not found")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSnippetNotFoundException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body?.status)
        assertEquals("Snippet Not Found", response.body?.error)
        assertEquals("Snippet with ID 1 not found", response.body?.message)
    }

    @Test
    fun `handleTestNotFoundException should return 404 NOT_FOUND`() {
        val ex = TestNotFoundException("Test with ID 1 not found")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleTestNotFoundException(ex)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body?.status)
        assertEquals("Test Not Found", response.body?.error)
        assertEquals("Test with ID 1 not found", response.body?.message)
    }
}
