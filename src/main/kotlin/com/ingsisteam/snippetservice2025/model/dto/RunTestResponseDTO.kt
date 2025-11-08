package com.ingsisteam.snippetservice2025.model.dto

data class RunTestResponseDTO(
    val testId: Long,
    val testName: String,
    val snippetId: Long,
    val output: String,
    val passed: Boolean,
    val message: String,
)
