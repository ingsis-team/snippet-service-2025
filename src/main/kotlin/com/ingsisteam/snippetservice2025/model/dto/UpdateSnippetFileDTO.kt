package com.ingsisteam.snippetservice2025.model.dto

import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

data class UpdateSnippetFileDTO(
    @field:NotNull(message = "File cannot be null")
    val file: MultipartFile,
)
