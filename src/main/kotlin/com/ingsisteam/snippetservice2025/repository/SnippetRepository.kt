package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.Snippet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetRepository : JpaRepository<Snippet, Long> {

    // Find snippets by user
    fun findByUserId(userId: String): List<Snippet>

    // Find snippets by user and name filter (case-insensitive, partial search)
    fun findByUserIdAndNameContainingIgnoreCase(userId: String, name: String): List<Snippet>

    // Check if a snippet with that name exists for the user
    fun existsByUserIdAndName(userId: String, name: String): Boolean

    // Find specific snippet by user and id
    fun findByIdAndUserId(id: Long, userId: String): Snippet?
}
