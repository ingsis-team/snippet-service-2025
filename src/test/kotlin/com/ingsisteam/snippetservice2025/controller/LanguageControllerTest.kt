package com.ingsisteam.snippetservice2025.controller

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class LanguageControllerTest {

    private val controller = LanguageController()

    @Test
    fun `getLanguages should return a list of languages`() {
        val response = controller.getLanguages()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals("printscript", response.body?.get(0)?.id)
    }
}
