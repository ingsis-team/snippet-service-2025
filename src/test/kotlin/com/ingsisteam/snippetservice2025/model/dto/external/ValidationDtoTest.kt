package com.ingsisteam.snippetservice2025.model.dto.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ValidationDtoTest {

    @Test
    fun `test ValidationRequest properties`() {
        val dto = ValidationRequest(
            content = "print(1)",
            language = "PRINTSCRIPT",
            version = "1.0",
        )
        assertEquals("print(1)", dto.content)
        assertEquals("PRINTSCRIPT", dto.language)
        assertEquals("1.0", dto.version)
    }

    @Test
    fun `test ValidationResponse with no errors`() {
        val dto = ValidationResponse(
            isValid = true,
            errors = null,
        )
        assertEquals(true, dto.isValid)
        assertEquals(null, dto.errors)
    }

    @Test
    fun `test ValidationResponse with errors`() {
        val error = ValidationError(
            rule = "NoPrint",
            line = 1,
            column = 1,
            message = "Print statements are not allowed",
        )
        val dto = ValidationResponse(
            isValid = false,
            errors = listOf(error),
        )
        assertEquals(false, dto.isValid)
        assertEquals(listOf(error), dto.errors)
    }

    @Test
    fun `test ValidationError properties`() {
        val dto = ValidationError(
            rule = "NoVariables",
            line = 5,
            column = 10,
            message = "Variables are not allowed",
        )
        assertEquals("NoVariables", dto.rule)
        assertEquals(5, dto.line)
        assertEquals(10, dto.column)
        assertEquals("Variables are not allowed", dto.message)
    }

    @Test
    fun `test ValidationResult properties`() {
        val dto = ValidationResult(
            isValid = true,
            rule = "NoSemicolons",
            line = 2,
            column = 3,
        )
        assertEquals(true, dto.isValid)
        assertEquals("NoSemicolons", dto.rule)
        assertEquals(2, dto.line)
        assertEquals(3, dto.column)
    }
}
