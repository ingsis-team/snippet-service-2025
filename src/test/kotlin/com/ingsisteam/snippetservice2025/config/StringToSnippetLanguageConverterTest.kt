package com.ingsisteam.snippetservice2025.config

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StringToSnippetLanguageConverterTest {

    private lateinit var converter: StringToSnippetLanguageConverter

    @BeforeEach
    fun setUp() {
        converter = StringToSnippetLanguageConverter()
    }

    @Test
    fun `convert should return correct SnippetLanguage for exact match`() {
        val source = "PRINTSCRIPT"
        val expected = SnippetLanguage.PRINTSCRIPT
        val result = converter.convert(source)
        assertEquals(expected, result)
    }

    @Test
    fun `convert should return correct SnippetLanguage for case-insensitive match`() {
        val source = "PRINTSCRIPT"
        val expected = SnippetLanguage.PRINTSCRIPT
        val result = converter.convert(source)
        assertEquals(expected, result)
    }

    @Test
    fun `convert should throw IllegalArgumentException for invalid language string`() {
        val source = "INVALID_LANGUAGE"
        val exception = assertThrows(IllegalArgumentException::class.java) {
            converter.convert(source)
        }
        assertEquals("Invalid language: '$source'. Valid values are: PRINTSCRIPT", exception.message)
    }
}
