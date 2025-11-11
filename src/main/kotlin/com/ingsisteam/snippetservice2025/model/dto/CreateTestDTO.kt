package com.ingsisteam.snippetservice2025.model.dto

import com.ingsisteam.snippetservice2025.model.enum.TestStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateTestDTO(
    @field:NotBlank(message = "Test name cannot be null or empty")
    val name: String,

    @field:NotNull(message = "Inputs list cannot be null")
    val inputs: List<String>,

    @field:NotNull(message = "Expected outputs list cannot be null")
    val expectedOutputs: List<String>,

    @field:NotNull(message = "Test status cannot be null")
    val expectedStatus: TestStatus,
)
