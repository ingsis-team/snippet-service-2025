package com.ingsisteam.snippetservice2025.model.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecuteSnippetDTOTest {

    @Test
    fun `test ExecuteSnippetDTO properties`() {
        val inputs = listOf("input1", "input2")
        val dto = ExecuteSnippetDTO(inputs = inputs)

        assertEquals(inputs, dto.inputs)
    }

    @Test
    fun `test ExecuteSnippetDTO with empty inputs`() {
        val dto = ExecuteSnippetDTO(inputs = emptyList())

        assertEquals(emptyList<String>(), dto.inputs)
    }

    @Test
    fun `test ExecuteSnippetDTO with default constructor`() {
        val dto = ExecuteSnippetDTO()

        assertEquals(emptyList<String>(), dto.inputs)
    }
}
