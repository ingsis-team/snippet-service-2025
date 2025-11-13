package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetTestRepository : JpaRepository<SnippetTest, Long> {

    // Find all tests for a snippet
    fun findBySnippetId(snippetId: Long): List<SnippetTest>

    // Find a specific test by snippet and id
    fun findByIdAndSnippetId(id: Long, snippetId: Long): SnippetTest?

    // Check if a test with that name exists for the snippet
    fun existsBySnippetIdAndName(snippetId: Long, name: String): Boolean
}
