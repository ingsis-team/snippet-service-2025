package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import java.time.LocalDateTime

data class TestResponseDTO(
    val id: Long,
    val snippetId: Long,
    val name: String,
    val inputs: List<String>,
    val expectedOutputs: List<String>,
    val expectedStatus: TestStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
