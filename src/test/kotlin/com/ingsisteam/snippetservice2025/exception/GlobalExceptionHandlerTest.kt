package com.ingsisteam.snippetservice2025.exception

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.multipart.support.MissingServletRequestPartException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()
    private val mockRequest = mockk<WebRequest>(relaxed = true)

    init {
        every { mockRequest.getDescription(false) } returns "uri=/test"
    }

    @Test
    fun `handleValidationExceptions should return 400 BAD_REQUEST`() {
        val fieldError = FieldError("objectName", "fieldName", "Default message")
        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.fieldErrors } returns listOf(fieldError)
        every { bindingResult.globalErrors } returns emptyList()
        val methodParameter = mockk<MethodParameter>(relaxed = true)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleValidationExceptions(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Error de Validación", response.body!!.error)
    }

    @Test
    fun `handleSyntaxValidation should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("ruleName", 1, 10, "Invalid syntax")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSyntaxValidation(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Error de Sintaxis", response.body!!.error)
        assertEquals("El código contiene errores de sintaxis", response.body!!.message)
    }

    @Test
    fun `handleIllegalArgument should return 400 BAD_REQUEST`() {
        val ex = IllegalArgumentException("Invalid argument provided")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleIllegalArgument(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Argumento Inválido", response.body!!.error)
        assertEquals("Invalid argument provided", response.body!!.message)
    }

    @Test
    fun `handleNotFound should return 404 NOT_FOUND`() {
        val ex = NoSuchElementException("Resource not found in database")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleNotFound(ex, mockRequest)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body!!.status)
        assertEquals("Recurso No Encontrado", response.body!!.error)
        assertEquals("Resource not found in database", response.body!!.message)
    }

    @Test
    fun `handleGenericException should return 500 INTERNAL_SERVER_ERROR`() {
        val ex = Exception("Something went wrong internally")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleGenericException(ex, mockRequest)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.body!!.status)
        assertEquals("Error Interno del Servidor", response.body!!.error)
    }

    @Test
    fun `handleUnauthorized should return 401 UNAUTHORIZED`() {
        val ex = UnauthorizedException("Authentication required")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleUnauthorized(ex, mockRequest)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.body!!.status)
        assertEquals("No Autorizado", response.body!!.error)
    }

    @Test
    fun `handlePermissionDenied should return 403 FORBIDDEN`() {
        val ex = PermissionDeniedException("Access denied for this resource")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handlePermissionDenied(ex, mockRequest)

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.FORBIDDEN.value(), response.body!!.status)
        assertEquals("Permiso Denegado", response.body!!.error)
    }

    @Test
    fun `handleSnippetNotFound should return 404 NOT_FOUND`() {
        val ex = SnippetNotFoundException("Snippet with ID 1 not found")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSnippetNotFound(ex, mockRequest)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body!!.status)
        assertEquals("Snippet No Encontrado", response.body!!.error)
    }

    @Test
    fun `handleTestNotFound should return 404 NOT_FOUND`() {
        val ex = TestNotFoundException("Test with ID 1 not found")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleTestNotFound(ex, mockRequest)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.NOT_FOUND.value(), response.body!!.status)
        assertEquals("Test No Encontrado", response.body!!.error)
    }

    @Test
    fun `handleMissingPart should return 400 BAD_REQUEST`() {
        val ex = MissingServletRequestPartException("missingPart")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleMissingPart(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Parámetro Faltante", response.body!!.error)
        assertEquals("Falta el parámetro 'missingPart' en la solicitud", response.body!!.message)
    }

    @Test
    fun `handlePrintScriptServiceError should return 400 BAD_REQUEST for 4xx status`() {
        val ex = PrintScriptServiceException("Bad request", "test operation", 400)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handlePrintScriptServiceError(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Error del Servicio PrintScript", response.body!!.error)
    }

    @Test
    fun `handlePrintScriptServiceError should return 502 BAD_GATEWAY for 5xx status`() {
        val ex = PrintScriptServiceException("Server error", "test operation", 500)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handlePrintScriptServiceError(ex, mockRequest)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.body!!.status)
        assertEquals("Error del Servicio PrintScript", response.body!!.error)
    }

    @Test
    fun `handlePrintScriptServiceError should return 500 INTERNAL_SERVER_ERROR for other status`() {
        val ex = PrintScriptServiceException("Other error", "test operation", null)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handlePrintScriptServiceError(ex, mockRequest)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.body!!.status)
        assertEquals("Error del Servicio PrintScript", response.body!!.error)
    }

    @Test
    fun `handleExternalServiceError should return 502 BAD_GATEWAY`() {
        val ex = ExternalServiceException("service", "operation", "message")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleExternalServiceError(ex, mockRequest)

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.body!!.status)
        assertEquals("Error de Servicio Externo", response.body!!.error)
    }

    @Test
    fun `handleSyntaxValidation with VALIDATION_SERVICE_ERROR should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("VALIDATION_SERVICE_ERROR", 1, 1, "")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSyntaxValidation(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Error del Servicio de Validación", response.body!!.error)
        assertEquals("Error en printscript service durante la validación", response.body!!.message)
    }

    @Test
    fun `handleSyntaxValidation with CONNECTION_ERROR should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("CONNECTION_ERROR", 1, 1, "")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSyntaxValidation(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Error del Servicio de Validación", response.body!!.error)
        assertEquals("No se pudo conectar al servicio de validación", response.body!!.message)
    }

    @Test
    fun `handleSyntaxValidation with SERVICE_ERROR should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("SERVICE_ERROR", 1, 1, "")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSyntaxValidation(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Error del Servicio de Validación", response.body!!.error)
        assertEquals("Error interno del servicio de validación", response.body!!.message)
    }

    @Test
    fun `handleSyntaxValidation with UNKNOWN_ERROR should return 400 BAD_REQUEST`() {
        val ex = SyntaxValidationException("UNKNOWN_ERROR", 1, 1, "")

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleSyntaxValidation(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Error del Servicio de Validación", response.body!!.error)
        assertEquals("Error inesperado durante la validación", response.body!!.message)
    }

    @Test
    fun `handleValidationExceptions with global errors should return 400 BAD_REQUEST`() {
        val globalError = mockk<org.springframework.validation.ObjectError>(relaxed = true)
        every { globalError.defaultMessage } returns "Global error message"
        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.fieldErrors } returns emptyList()
        every { bindingResult.globalErrors } returns listOf(globalError)
        val methodParameter = mockk<MethodParameter>(relaxed = true)
        val ex = MethodArgumentNotValidException(methodParameter, bindingResult)

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleValidationExceptions(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Error de Validación", response.body!!.error)
        assertTrue(response.body!!.details!!.contains("Global error message"))
    }

    @Test
    fun `handleValidationExceptions with kotlin null parameter error should return 400 BAD_REQUEST`() {
        val bindingResult = mockk<BindingResult>(relaxed = true)
        every { bindingResult.fieldErrors } returns emptyList()
        every { bindingResult.globalErrors } returns emptyList()
        val methodParameter = mockk<MethodParameter>(relaxed = true)
        val ex = mockk<MethodArgumentNotValidException>()
        every { ex.parameter } returns methodParameter
        every { ex.bindingResult } returns bindingResult
        every { ex.message } returns "Parameter specified as non-null is null: parameter myParam"

        val response: ResponseEntity<GlobalExceptionHandler.ErrorResponse> = handler.handleValidationExceptions(ex, mockRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.body!!.status)
        assertEquals("Error de Validación", response.body!!.error)
        assertTrue(response.body!!.details!!.contains("El parámetro 'myParam' es obligatorio y no puede ser null"))
    }
}
