package com.ingsisteam.snippetservice2025.model.dto.external

data class RunRequest(
    val userId: String,
    val snippetId: String,
    val language: String,
    val version: String,
    val input: String,
    val correlationId: String,
)

data class RunResponse(
    val output: String,
    val correlationId: String,
    val snippetId: String,
)
