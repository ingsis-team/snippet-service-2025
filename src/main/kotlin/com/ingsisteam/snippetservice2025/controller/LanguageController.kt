package com.ingsisteam.snippetservice2025.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
@Tag(name = "Language Controller", description = "API para obtener lenguajes soportados")
class LanguageController {
    private val logger = LoggerFactory.getLogger(LanguageController::class.java)

    data class LanguageDTO(
        val id: String,
        val name: String,
        val extension: String?,
        val description: String? = null,
    )

    @GetMapping("/languages")
    @Operation(summary = "Obtener lenguajes soportados", description = "Devuelve la lista de lenguajes disponibles en el servicio")
    fun getLanguages(): ResponseEntity<List<LanguageDTO>> {
        logger.debug("Fetching supported languages")

        val languages = listOf(
            LanguageDTO(id = "printscript", name = "PrintScript", extension = "ps", description = "Lenguaje educativo PrintScript"),
        )

        return ResponseEntity.ok(languages)
    }
}
