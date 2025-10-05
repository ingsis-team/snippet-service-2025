package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

data class CreateSnippetFileDTO(
    @field:NotBlank(message = "Snippet name cannot be null or empty")
    val name: String,

    @field:NotBlank(message = "Snippet description cannot be null or empty")
    val description: String,

    @field:NotNull(message = "Snippet language cannot be null")
    val language: SnippetLanguage,

    @field:NotBlank(message = "Version cannot be null or empty")
    val version: String,

    @field:NotNull(message = "File cannot be null")
    val file: MultipartFile,
)
