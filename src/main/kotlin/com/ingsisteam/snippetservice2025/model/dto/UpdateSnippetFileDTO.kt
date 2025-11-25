package com.ingsisteam.snippetservice2025.model.dto

import org.springframework.web.multipart.MultipartFile

data class UpdateSnippetFileDTO(
    val file: MultipartFile,
    val name: String? = null,
    val description: String? = null,
)
