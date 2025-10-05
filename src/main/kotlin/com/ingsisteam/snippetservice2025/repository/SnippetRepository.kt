package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetRepository : JpaRepository<Snippet, Long> {

    // Buscar snippets por usuario
    fun findByUserId(userId: String): List<Snippet>

    // Verificar si existe un snippet con ese nombre para el usuario
    fun existsByUserIdAndName(userId: String, name: String): Boolean

    // Buscar snippet específico por usuario e id
    fun findByIdAndUserId(id: Long, userId: String): Snippet?
}
