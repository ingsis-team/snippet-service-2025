package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.multipart.MultipartFile

class CreateSnippetFileDTOTest {

    @Test
    fun `test CreateSnippetFileDTO properties`() {
        val mockFile = mockk<MultipartFile>()
        val dto = CreateSnippetFileDTO(
            name = "Test Snippet",
            description = "This is a test snippet",
            language = SnippetLanguage.PRINTSCRIPT,
            version = "1.0",
            file = mockFile,
        )

        assertEquals("Test Snippet", dto.name)
        assertEquals("This is a test snippet", dto.description)
        assertEquals(SnippetLanguage.PRINTSCRIPT, dto.language)
        assertEquals("1.0", dto.version)
        assertEquals(mockFile, dto.file)
    }
}
