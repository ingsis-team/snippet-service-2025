package com.ingsisteam.snippetservice2025.config

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class StringToSnippetLanguageConverter : Converter<String, SnippetLanguage> {
    override fun convert(source: String): SnippetLanguage {
        return try {
            // Try exact match first
            SnippetLanguage.valueOf(source.uppercase())
        } catch (e: IllegalArgumentException) {
            // If exact match fails, try case-insensitive match
            SnippetLanguage.values().firstOrNull {
                it.name.equals(source, ignoreCase = true)
            } ?: throw IllegalArgumentException(
                "Invalid language: '$source'. Valid values are: ${SnippetLanguage.values().joinToString { it.name }}",
            )
        }
    }
}
