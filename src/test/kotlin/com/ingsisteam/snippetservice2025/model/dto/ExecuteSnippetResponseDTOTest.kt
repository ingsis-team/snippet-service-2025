package com.ingsisteam.snippetservice2025.model.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExecuteSnippetResponseDTOTest {

    @Test
    fun `test ExecuteSnippetResponseDTO properties`() {
        val outputs = listOf("output1", "output2")
        val errors = listOf("error1")
        val dto = ExecuteSnippetResponseDTO(outputs = outputs, errors = errors)

        assertEquals(outputs, dto.outputs)
        assertEquals(errors, dto.errors)
    }

    @Test
    fun `test ExecuteSnippetResponseDTO with empty errors`() {
        val outputs = listOf("output1")
        val dto = ExecuteSnippetResponseDTO(outputs = outputs)

        assertEquals(outputs, dto.outputs)
        assertEquals(emptyList<String>(), dto.errors)
    }

    @Test
    fun `test ExecuteSnippetResponseDTO with empty outputs and errors`() {
        val dto = ExecuteSnippetResponseDTO(outputs = emptyList(), errors = emptyList())

        assertEquals(emptyList<String>(), dto.outputs)
        assertEquals(emptyList<String>(), dto.errors)
    }
}
