package com.ingsisteam.snippetservice2025.model.dto.external

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PrintScriptDtoTest {

    @Test
    fun `test SnippetDTO properties`() {
        val dto = SnippetDTO(
            snippetId = "s1",
            correlationId = "c1",
            language = "PRINTSCRIPT",
            version = "1.0",
            input = "println(\"hello\")",
            userId = "u1"
        )
        assertEquals("s1", dto.snippetId)
        assertEquals("c1", dto.correlationId)
        assertEquals("PRINTSCRIPT", dto.language)
        assertEquals("1.0", dto.version)
        assertEquals("println(\"hello\")", dto.input)
        assertEquals("u1", dto.userId)
    }

    @Test
    fun `test SnippetOutputDTO properties`() {
        val dto = SnippetOutputDTO(
            snippet = "output",
            correlationId = "c2",
            snippetId = "s2"
        )
        assertEquals("output", dto.snippet)
        assertEquals("c2", dto.correlationId)
        assertEquals("s2", dto.snippetId)
    }

    @Test
    fun `test Rule with Boolean value`() {
        val dto = Rule(name = "camelCase", value = true)
        assertEquals("camelCase", dto.name)
        assertEquals(true, dto.value)
    }

    @Test
    fun `test Rule with Int value`() {
        val dto = Rule(name = "indentation", value = 4)
        assertEquals("indentation", dto.name)
        assertEquals(4, dto.value)
    }

    @Test
    fun `test Rule with String value`() {
        val dto = Rule(name = "quotes", value = "double")
        assertEquals("quotes", dto.name)
        assertEquals("double", dto.value)
    }

    @Test
    fun `test TestDTO properties`() {
        val dto = TestDTO(
            input = listOf("input1"),
            output = listOf("output1"),
            snippet = "testSnippet",
            envVars = mapOf("ENV_VAR_1" to "value1")
        )
        assertEquals(listOf("input1"), dto.input)
        assertEquals(listOf("output1"), dto.output)
        assertEquals("testSnippet", dto.snippet)
        assertEquals(mapOf("ENV_VAR_1" to "value1"), dto.envVars)
    }

    @Test
    fun `test SCAOutput properties`() {
        val dto = SCAOutput(
            rule = "NoPrint",
            line = 1,
            column = 5,
            message = "Print statement found"
        )
        assertEquals("NoPrint", dto.rule)
        assertEquals(1, dto.line)
        assertEquals(5, dto.column)
        assertEquals("Print statement found", dto.message)
    }
}
