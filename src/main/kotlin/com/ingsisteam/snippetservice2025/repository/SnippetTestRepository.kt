package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetTestRepository : JpaRepository<SnippetTest, Long> {

    // Buscar todos los tests de un snippet
    fun findBySnippetId(snippetId: Long): List<SnippetTest>

    // Buscar un test específico por snippet e id
    fun findByIdAndSnippetId(id: Long, snippetId: Long): SnippetTest?

    // Verificar si existe un test con ese nombre para el snippet
    fun existsBySnippetIdAndName(snippetId: Long, name: String): Boolean
}
