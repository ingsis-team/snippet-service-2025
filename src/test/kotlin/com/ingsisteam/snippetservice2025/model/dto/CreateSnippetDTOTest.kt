package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CreateSnippetDTOTest {

    @Test
    fun `test CreateSnippetDTO properties`() {
        val dto = CreateSnippetDTO(
            name = "Test Snippet",
            description = "This is a test snippet",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"Hello\")",
            version = "1.0",
        )

        assertEquals("Test Snippet", dto.name)
        assertEquals("This is a test snippet", dto.description)
        assertEquals(SnippetLanguage.PRINTSCRIPT, dto.language)
        assertEquals("println(\"Hello\")", dto.content)
        assertEquals("1.0", dto.version)
    }

    @Test
    fun `test CreateSnippetDTO with default description`() {
        val dto = CreateSnippetDTO(
            name = "Test Snippet 2",
            language = SnippetLanguage.PRINTSCRIPT,
            content = "println(\"World\")",
            version = "2.0",
        )

        assertEquals("Test Snippet 2", dto.name)
        assertEquals("", dto.description) // Default value
        assertEquals(SnippetLanguage.PRINTSCRIPT, dto.language)
        assertEquals("println(\"World\")", dto.content)
        assertEquals("2.0", dto.version)
    }
}
