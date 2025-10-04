package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.Snippet
import com.ingsisteam.snippetservice2025.model.enum.SnippetLanguage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SnippetRepository : JpaRepository<Snippet, Long> {

    // Buscar snippets por usuario
    fun findByUserId(userId: String): List<Snippet>

    // Buscar snippets por usuario con paginación
    fun findByUserId(userId: String, pageable: Pageable): Page<Snippet>

    // Buscar por usuario y lenguaje
    fun findByUserIdAndLanguage(userId: String, language: SnippetLanguage): List<Snippet>

    // Buscar por nombre (case insensitive)
    @Query("SELECT s FROM Snippet s WHERE s.userId = :userId AND LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    fun findByUserIdAndNameContainingIgnoreCase(
        @Param("userId") userId: String,
        @Param("name") name: String,
    ): List<Snippet>

    // Verificar si existe un snippet con ese nombre para el usuario
    fun existsByUserIdAndName(userId: String, name: String): Boolean

    // Buscar snippet específico por usuario e id
    fun findByIdAndUserId(id: Long, userId: String): Snippet?
}
