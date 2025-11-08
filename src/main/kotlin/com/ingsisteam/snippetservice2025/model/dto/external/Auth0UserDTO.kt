package com.ingsisteam.snippetservice2025.model.dto.external

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO que representa un usuario de Auth0
 */
data class Auth0UserDTO(
    @JsonProperty("user_id")
    val userId: String,
    val email: String? = null,
    val name: String? = null,
    val nickname: String? = null,
    val picture: String? = null,
)
