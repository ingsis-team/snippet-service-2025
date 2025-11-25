package com.ingsisteam.snippetservice2025.repository

import com.ingsisteam.snippetservice2025.model.entity.SnippetTest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SnippetTestRepository : JpaRepository<SnippetTest, String> {

    // Find all tests for a snippet
    fun findBySnippetId(snippetId: String): List<SnippetTest>

    // Find a specific test by snippet and id
    fun findByIdAndSnippetId(id: String, snippetId: String): SnippetTest?

    // Check if a test with that name exists for the snippet
    fun existsBySnippetIdAndName(snippetId: String, name: String): Boolean
}
